package com.scazzumvivendi.seento.data.ble

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import com.movesense.mds.Mds
import com.movesense.mds.MdsConnectionListener
import com.movesense.mds.MdsException
import com.movesense.mds.MdsNotificationListener
import com.movesense.mds.MdsResponseListener
import com.movesense.mds.MdsSubscription
import com.scazzumvivendi.seento.data.ble.model.CatalogReadPhase
import com.scazzumvivendi.seento.data.ble.model.CatalogReadProgress
import com.scazzumvivendi.seento.data.ble.model.RemoteMusicCatalog
import com.scazzumvivendi.seento.data.ble.model.RemoteMusicTrack
import com.scazzumvivendi.seento.data.ble.model.RemotePlaylist
import com.scazzumvivendi.seento.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds

class MdsMusicClient(context: Context) : Closeable {

    private val appContext = context.applicationContext
    private val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val mdsHolder = lazy {
        Mds.builder()
            .responseHandler(Handler(Looper.getMainLooper()))
            .build(appContext)
    }

    private val mds: Mds
        get() = mdsHolder.value

    private var connectedAddress: String? = null
    private var connectedSerial: String? = null
    private var directSession: NgbBleSession? = null
    private var connectedDevicesSubscription: MdsSubscription? = null
    private var connectingDevicesSubscription: MdsSubscription? = null
    private var connectingDevice: CompletableDeferred<ConnectingDevice>? = null
    private var isDisconnecting = false
    private var connectionGeneration = 0L
    var onConnectedDeviceRemoved: (() -> Unit)? = null

    suspend fun connect(address: String, serial: String): String {
        disconnect()
        val generation = ++connectionGeneration
        connectedAddress = address
        mds // Initialize the MDS/SDS runtime before asking the SDK to connect.
        try {
            subscribeConnectedDevices()
            subscribeConnectingDevices()
            connectingDevice = CompletableDeferred()
            val session = NgbBleSession(appContext)
            directSession = session
            val sdkSerial = withTimeout(CONNECTION_TIMEOUT_MS.milliseconds) {
                session.connect(address) { }

                // Some firmware versions do not emit AboutToConnect before the
                // FinishConnect request. The direct session already owns the
                // Whiteboard address, so the notification is only an optional
                // refinement, not a prerequisite for completing the handshake.
                val connecting = connectingDevice
                    ?.takeIf { it.isCompleted }
                    ?.await()
                    ?: ConnectingDevice(
                        serial = serial,
                        address = session.whiteboardAddress
                    )
                val announcedSerial = connecting.serial
                    .ifBlank { serial }
                    .ifBlank { error("Il dispositivo non ha comunicato il seriale") }

                put(
                    uri = "suunto://MDS/ConnectingDevices",
                    contract = connectingDeviceContract(
                        serial = announcedSerial,
                        state = "FinishConnect",
                        address = connecting.address
                    )
                )
                val deviceInfo = connecting.deviceInfo
                if (deviceInfo == null) {
                    Log.w(TAG, "ConnectingDevices did not include DeviceInfo; registering serial only")
                }
                post(
                    uri = "suunto://MDS/ConnectedDevices",
                    contract = buildConnectedDeviceInfo(
                        serial = announcedSerial,
                        whiteboardAddress = connecting.address,
                        deviceInfo = deviceInfo
                    ).toString()
                )
                // A successful ConnectedDevices response (normally 201) is
                // the registration acknowledgement. Some watches do not send
                // a second notification for the same announcement.
                announcedSerial
            }
            if (connectionGeneration != generation) error("Connessione annullata")
            connectedSerial = sdkSerial
            Log.i(TAG, "MDS SDK lifecycle complete: serial=$sdkSerial")
            return sdkSerial
        } catch (error: Throwable) {
            directSession?.close()
            directSession = null
            runCatching { mds.disconnect(address) }
            closeLocalSession()
            if (error !is CancellationException) {
                Log.e(TAG, "Manual MDS lifecycle failed", error)
            }
            throw error
        }
    }

    private fun connectingDeviceContract(
        serial: String,
        state: String,
        address: String
    ): String = JSONObject()
        .put("Address", address)
        .put("Serial", serial)
        .put("State", state)
        .toString()

    private fun buildConnectedDeviceInfo(
        serial: String,
        whiteboardAddress: String,
        deviceInfo: JSONObject?
    ): JSONObject =
        JSONObject()
            .put("Serial", serial)
            .put(
                "DeviceInfo",
                deviceInfo ?: JSONObject()
                    .put("Serial", serial)
                    .put("serial", serial)
            )
            .put(
                "Connection",
                JSONObject()
                    .put("UUID", whiteboardAddress)
                    .put("Type", "BLE")
            )

    suspend fun readCatalog(
        onProgress: (CatalogReadProgress) -> Unit = {}
    ): RemoteMusicCatalog {
        val serial = connectedSerial
            ?: error("Dispositivo non connesso")

        onProgress(CatalogReadProgress(CatalogReadPhase.PREPARING))

        try {
            getWithConnectionRetry("suunto://$serial/offline/music/version")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Music version endpoint unavailable; trying allSongs", error)
        }

        val allSongsBody = getWithConnectionRetry(
            uri = "suunto://$serial/offline/music/allSongs",
            contract = null
        )

        val allSongs = decodeContent(allSongsBody)
        val playlist = allSongs.optJSONObject("playList") ?: allSongs
        val items = playlist.optJSONArray("musicItems") ?: JSONArray()
        onProgress(CatalogReadProgress(CatalogReadPhase.READING_SONGS, total = items.length()))
        val tracks = buildList {
            for (index in 0 until items.length()) {
                val key = items.optJSONObject(index)?.optLong("key") ?: continue
                val info = try {
                    val infoBody = getWithTransientRetry(
                        uri = "suunto://$serial/offline/music/info",
                        contract = JSONObject().put("key", key).toString()
                    )
                    decodeContent(infoBody).optJSONObject("musicInfo")
                        ?: decodeContent(infoBody)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.w(TAG, "Metadata unavailable for allSongs key=$key; keeping catalog entry", error)
                    JSONObject()
                }

                add(
                    RemoteMusicTrack(
                        key = key,
                        path = info.optString("musicPath").ifBlank { "device-key:$key" },
                        title = info.optNullableString("title"),
                        artist = info.optNullableString("artist"),
                        album = info.optNullableString("album"),
                        durationMillis = info.optLongOrNull("duration"),
                        deviceKey = key.toString()
                    )
                )
                onProgress(
                    CatalogReadProgress(
                        phase = CatalogReadPhase.READING_SONGS,
                        completed = size,
                        total = items.length()
                    )
                )
            }
        }

        onProgress(CatalogReadProgress(CatalogReadPhase.READING_PLAYLISTS))
        val playlistRead = try {
            readPlaylists(serial, onProgress) to null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Watch playlist list unavailable", error)
            emptyList<RemotePlaylist>() to error.message
        }

        return RemoteMusicCatalog(
            serial = serial,
            playlistName = playlist.optString("playListName"),
            tracks = tracks,
            playlists = playlistRead.first,
            playlistReadWarning = playlistRead.second
        )
    }

    private suspend fun getWithTransientRetry(
        uri: String,
        contract: String? = null
    ): String {
        var lastError: Exception? = null
        repeat(TRANSIENT_READ_ATTEMPTS) { attempt ->
            try {
                return get(uri, contract)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!TransientMdsReadFailure.matches(error)) throw error
                lastError = error
                if (attempt < TRANSIENT_READ_ATTEMPTS - 1) {
                    Log.w(TAG, "Transient read failure (${attempt + 1}/$TRANSIENT_READ_ATTEMPTS) for $uri", error)
                    delay(TRANSIENT_READ_RETRY_DELAY_MS)
                }
            }
        }
        throw lastError ?: IllegalStateException("Lettura fallita: $uri")
    }

    private suspend fun getWithConnectionRetry(
        uri: String,
        contract: String? = null
    ): String {
        var lastError: Exception? = null
        repeat(CONNECTION_READ_ATTEMPTS) { attempt ->
            try {
                return get(uri, contract)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                val status404 = error.message.orEmpty().contains("404")
                if (!status404 && !TransientMdsReadFailure.matches(error)) throw error
                lastError = error
                if (attempt < CONNECTION_READ_ATTEMPTS - 1) {
                    Log.w(TAG, "Device route not ready (${attempt + 1}/$CONNECTION_READ_ATTEMPTS): $uri", error)
                    delay(CONNECTION_READ_RETRY_DELAY_MS)
                }
            }
        }
        throw lastError ?: IllegalStateException("Lettura MDS fallita: $uri")
    }

    private suspend fun readPlaylists(
        serial: String,
        onProgress: (CatalogReadProgress) -> Unit
    ): List<RemotePlaylist> {
        val body = get(
            uri = "suunto://$serial/offline/music/playLists",
            contract = null
        )
        val root = decodeContentValue(body)
        val array = when (root) {
            is JSONArray -> root
            is JSONObject -> root.optJSONArrayAny(
                "playLists", "playlists", "musicLists"
            ) ?: when (val value = root.opt("playList")) {
                is JSONArray -> value
                is JSONObject -> JSONArray().put(value)
                else -> JSONArray()
            }
            else -> JSONArray()
        }

        return buildList {
            onProgress(
                CatalogReadProgress(
                    phase = CatalogReadPhase.READING_PLAYLISTS,
                    total = array.length()
                )
            )
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optLongAny("playListId", "playlistId", "id")
                val name = item.optStringAny(
                    "playListName", "playlistName", "name"
                ) ?: ContextCompat.getContextForLanguage(appContext)
                    .getString(R.string.unnamed_device_playlist, id)
                if (!WatchPlaylistPolicy.isUserPlaylist(
                        id = id,
                        name = name,
                        sortId = item.optInt("sortId", -1)
                    )
                ) continue
                val detailBody = getWithTransientRetry(
                    uri = "suunto://$serial/offline/music/customerList",
                    contract = JSONObject().put("playListId", id).toString()
                )
                val detail = decodeContent(detailBody)
                val detailPlaylist = detail.optJSONObject("playList") ?: detail
                val musicItems = detailPlaylist.optJSONArrayAny(
                    "musicItems", "tracks"
                ) ?: item.optJSONArrayAny("musicItems", "tracks") ?: JSONArray()
                val songs = buildList {
                    for (songIndex in 0 until musicItems.length()) {
                        val song = musicItems.opt(songIndex)
                        when (song) {
                            is JSONObject -> add(song.optLongAny("key", "musicKey"))
                            is Number -> add(song.toLong())
                        }
                    }
                }
                add(RemotePlaylist(id = id, name = name, songKeys = songs))
                onProgress(
                    CatalogReadProgress(
                        phase = CatalogReadPhase.READING_PLAYLISTS,
                        completed = size,
                        total = array.length()
                    )
                )
            }
        }
    }

    suspend fun writePlaylist(
        remotePlaylistId: Long?,
        playlistName: String,
        songKeys: List<Long>
    ): Long {
        val serial = connectedSerial
            ?: error("Dispositivo non connesso")
        val playlistId = remotePlaylistId ?: (System.currentTimeMillis() / 1_000L)

        val payload = JSONObject().put(
            "playList",
            JSONObject()
                .put("sortId", 4)
                .put("playListId", playlistId)
                .put("musicNum", songKeys.size)
                .put("playListName", playlistName)
                .put(
                    "musicItems",
                    JSONArray().apply {
                        songKeys.forEach { key ->
                            put(JSONObject().put("key", key))
                        }
                    }
                )
        )

        val contract = JSONObject().put(
            "value",
            Base64.encodeToString(
                payload.toString().toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )
        )

        val operation = if (remotePlaylistId == null) "List/add" else "List"
        put("suunto://$serial/offline/music/$operation", contract.toString())
        return playlistId
    }

    suspend fun deletePlaylist(remotePlaylistId: Long) {
        val serial = connectedSerial
            ?: error("Dispositivo non connesso")

        delete(
            uri = "suunto://$serial/offline/music/List",
            contract = JSONObject().put("playListId", remotePlaylistId).toString()
        )
    }

    private suspend fun get(uri: String, contract: String?): String =
        suspendCancellableCoroutine { continuation ->
            mds.get(uri, contract, object : MdsResponseListener {
                override fun onSuccess(data: String) {
                    Log.d(TAG, "GET succeeded: $uri")
                    if (continuation.isActive) continuation.resume(data)
                }

                override fun onError(error: MdsException) {
                    Log.e(TAG, "GET failed: $uri", error)
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
            })
        }

    private suspend fun put(uri: String, contract: String): String =
        suspendCancellableCoroutine { continuation ->
            mds.put(uri, contract, object : MdsResponseListener {
                override fun onSuccess(data: String) {
                    if (continuation.isActive) continuation.resume(data)
                }

                override fun onError(error: MdsException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
            })
        }

    private suspend fun post(uri: String, contract: String): String =
        suspendCancellableCoroutine { continuation ->
            mds.post(uri, contract, object : MdsResponseListener {
                override fun onSuccess(data: String) {
                    Log.i(TAG, "POST succeeded: $uri response=$data")
                    if (continuation.isActive) continuation.resume(data)
                }

                override fun onError(error: MdsException) {
                    Log.e(TAG, "POST failed: $uri", error)
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            })
        }

    private suspend fun delete(uri: String, contract: String?): String =
        suspendCancellableCoroutine { continuation ->
            mds.delete(uri, contract, object : MdsResponseListener {
                override fun onSuccess(data: String) {
                    Log.d(TAG, "DELETE succeeded: $uri")
                    if (continuation.isActive) continuation.resume(data)
                }

                override fun onError(error: MdsException) {
                    Log.e(TAG, "DELETE failed: $uri", error)
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
            })
        }

    private fun subscribeConnectedDevices() {
        if (connectedDevicesSubscription != null) return
        connectedDevicesSubscription = mds.subscribe(
            Mds.URI_EVENTLISTENER,
            JSONObject()
                .put("Uri", Mds.URI_CONNECTEDDEVICES)
                .toString(),
            object : MdsNotificationListener {
                override fun onNotification(data: String) {
                    Log.i(TAG, "ConnectedDevices event: $data")
                    if (runCatching { JSONObject(data).optString("Method") }
                            .getOrNull().equals("DEL", ignoreCase = true)
                    ) {
                        if (isDisconnecting) {
                            Log.d(TAG, "Ignoring ConnectedDevices removal during local disconnect")
                        } else {
                            Log.w(TAG, "ConnectedDevices route was removed by another connection")
                            onConnectedDeviceRemoved?.invoke()
                        }
                        return
                    }
                    val announcement = parseConnectedDeviceAnnouncement(data)
                    if (announcement == null) {
                        Log.d(TAG, "Ignoring ConnectedDevices notification without a successful device announcement")
                        return
                    }
                    val expectedAddress = connectedAddress
                    if (expectedAddress != null && announcement.address != null &&
                        !sameBluetoothAddress(expectedAddress, announcement.address)
                    ) {
                        Log.w(TAG, "Ignoring ConnectedDevices announcement for another BLE address")
                        return
                    }

                    connectedSerial = announcement.serial
                    Log.i(TAG, "ConnectedDevices registration confirmed: serial=${announcement.serial}")
                }

                override fun onError(error: MdsException) {
                    Log.w(TAG, "ConnectedDevices subscription failed", error)
                }
            }
        )
    }

    private fun subscribeConnectingDevices() {
        if (connectingDevicesSubscription != null) return
        connectingDevicesSubscription = mds.subscribe(
            Mds.URI_EVENTLISTENER,
            JSONObject()
                .put("Uri", "suunto://MDS/ConnectingDevices")
                .toString(),
            object : MdsNotificationListener {
                override fun onNotification(data: String) {
                    Log.i(TAG, "ConnectingDevices event: $data")
                    val root = runCatching { JSONObject(data) }.getOrNull() ?: return
                    val body = root.optJSONObject("Body") ?: return
                    val state = body.optString("State")
                    val address = body.optString("Address")
                    val deviceInfo = body.optJSONObject("DeviceInfo")
                        ?: body.optJSONObject("deviceInfo")
                    val serial = body.optString("Serial")
                        .ifBlank { body.optString("serial") }
                        .ifBlank { body.optString("DeviceSerialNumber") }
                        .ifBlank { body.optString("deviceSerialNumber") }
                        .ifBlank { deviceInfo?.optString("Serial").orEmpty() }
                        .ifBlank { deviceInfo?.optString("serial").orEmpty() }
                    if (state.equals("AboutToConnect", ignoreCase = true) && address.isNotBlank()) {
                        Log.i(TAG, "MDS assigned Whiteboard address $address")
                        connectingDevice?.complete(
                            ConnectingDevice(
                                serial = serial,
                                address = address,
                                deviceInfo = deviceInfo?.let { JSONObject(it.toString()) }
                            )
                        )
                    }
                }

                override fun onError(error: MdsException) {
                    Log.w(TAG, "ConnectingDevices subscription failed", error)
                    connectingDevice?.completeExceptionally(error)
                }
            }
        )
    }

    private fun parseConnectedDeviceAnnouncement(data: String): DeviceAnnouncement? =
        runCatching {
            val root = JSONObject(data)
            if (root.optString("Method").equals("DEL", ignoreCase = true)) return null
            val response = root.optJSONObject("Response")
            val status = response?.optInt("Status", -1)
                ?: root.optInt("Status", -1)
            if (status !in 200..299) return null

            val body = root.optJSONObject("Body") ?: return null
            val deviceInfo = body.optJSONObject("DeviceInfo") ?: return null
            val serial = deviceInfo.optString("Serial")
                .ifBlank { deviceInfo.optString("serial") }
                .takeIf(String::isNotBlank)
                ?: return null

            val addresses = deviceInfo.optJSONArray("addressInfo")
            val bleAddress = (0 until (addresses?.length() ?: 0))
                .asSequence()
                .mapNotNull { addresses?.optJSONObject(it) }
                .firstOrNull {
                    it.optString("name").contains("BLE", ignoreCase = true)
                }
                ?.optString("address")
                ?.takeIf(String::isNotBlank)

            DeviceAnnouncement(serial, bleAddress)
        }.getOrNull()

    private fun sameBluetoothAddress(first: String, second: String): Boolean =
        first.filter(Char::isLetterOrDigit)
            .equals(second.filter(Char::isLetterOrDigit), ignoreCase = true)

    private fun decodeContent(body: String): JSONObject {
        return when (val value = decodeContentValue(body)) {
            is JSONObject -> value
            else -> JSONObject()
        }
    }

    private fun decodeContentValue(body: String): Any {
        val root = JSONObject(body)
        val content = root.opt("Content") ?: root.opt("content") ?: root
        return when (content) {
            is JSONObject, is JSONArray -> content
            is String -> {
                val candidate = if (content.trimStart().startsWith("{") ||
                    content.trimStart().startsWith("[")
                ) {
                    content
                } else {
                    runCatching {
                        String(
                            Base64.decode(content, Base64.DEFAULT),
                            Charsets.UTF_8
                        )
                    }.getOrDefault(content)
                }
                runCatching { JSONObject(candidate) }.getOrElse {
                    runCatching { JSONArray(candidate) }.getOrElse { root }
                }
            }
            else -> root
        }
    }

    suspend fun disconnect() {
        isDisconnecting = true
        try {
            // Mds.disconnect() owns the SDK lifecycle and emits the matching event.
        } finally {
            connectedAddress?.let { address ->
                if (mdsHolder.isInitialized()) runCatching { mds.disconnect(address) }
            }
            closeLocalSession()
            isDisconnecting = false
        }
    }

    private fun closeLocalSession() {
        directSession?.close()
        directSession = null
        connectedDevicesSubscription?.unsubscribe()
        connectedDevicesSubscription = null
        connectingDevicesSubscription?.unsubscribe()
        connectingDevicesSubscription = null
        connectingDevice = null
        connectedAddress = null
        connectedSerial = null
        connectionGeneration++
    }

    override fun close() {
        isDisconnecting = true
        try {
            connectedAddress?.let { address ->
                if (mdsHolder.isInitialized()) runCatching { mds.disconnect(address) }
            }
            closeLocalSession()
        } finally {
            isDisconnecting = false
        }
    }

    private companion object {
        const val TAG = "SeentoMDS"
        const val TRANSIENT_READ_ATTEMPTS = 3
        const val TRANSIENT_READ_RETRY_DELAY_MS = 2_000L
        const val CONNECTION_READ_ATTEMPTS = 6
        const val CONNECTION_READ_RETRY_DELAY_MS = 1_000L
        const val CONNECTION_TIMEOUT_MS = 45_000L
        const val SYNC_FINISHED_ACTION = "com.stt.android.SYNC_FINISHED"
    }

}

private data class DeviceAnnouncement(
    val serial: String,
    val address: String?
)

private data class ConnectingDevice(
    val serial: String,
    val address: String,
    val deviceInfo: JSONObject? = null
)

private fun JSONObject.optNullableString(name: String): String? =
    optString(name).takeUnless { it.isBlank() }

private fun JSONObject.optLongOrNull(name: String): Long? =
    if (has(name) && !isNull(name)) optLong(name) else null

private fun JSONObject.optJSONArrayAny(vararg names: String): JSONArray? =
    names.firstNotNullOfOrNull { name -> optJSONArray(name) }

private fun JSONObject.optLongAny(vararg names: String): Long =
    names.firstNotNullOfOrNull { name ->
        if (has(name) && !isNull(name)) optLong(name) else null
    } ?: 0L

private fun JSONObject.optStringAny(vararg names: String): String? =
    names.firstNotNullOfOrNull { name -> optString(name).takeUnless { it.isBlank() } }
