package com.scazzumvivendi.seento.ui.wearable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scazzumvivendi.seento.data.ble.BleDeviceScanner
import com.scazzumvivendi.seento.data.ble.MdsConnectionStatus
import com.scazzumvivendi.seento.data.ble.MdsMusicClient
import com.scazzumvivendi.seento.data.ble.PlaylistDeviceMatcher
import com.scazzumvivendi.seento.data.ble.model.RemotePlaylist
import com.scazzumvivendi.seento.domain.model.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import android.content.SharedPreferences
import android.content.Context
import androidx.core.content.ContextCompat
import com.scazzumvivendi.seento.R

class WearableViewModel(
    private val scanner: BleDeviceScanner,
    private val mdsClient: MdsMusicClient,
    private val preferences: SharedPreferences,
    private val context: Context? = null
) : ViewModel() {

    private fun text(id: Int, vararg args: Any): String =
        context?.let { ContextCompat.getContextForLanguage(it).getString(id, *args) } ?: when (id) {
        R.string.mds_session_closed -> "La sessione MDS è stata chiusa da un'altra connessione. Chiudi l'app Suunto e riconnetti il dispositivo."
        R.string.ble_scanner_unavailable -> "Scanner BLE non disponibile sul telefono"
        R.string.bluetooth_permission_unavailable -> "Permesso Bluetooth non disponibile"
        R.string.bluetooth_scan_failed -> "Scansione Bluetooth fallita (codice ${args.firstOrNull()})"
        R.string.no_ble_device_found -> "Nessun dispositivo BLE trovato. Verifica che il dispositivo sia acceso e vicino al telefono."
        R.string.mds_handshake_timeout -> "Il dispositivo non ha completato l'handshake MDS entro 20 secondi"
        R.string.cannot_connect_device -> "Impossibile connettersi al dispositivo"
        R.string.disconnect_error -> "Errore durante la disconnessione"
        R.string.device_disconnected -> "Dispositivo disconnesso"
        R.string.connection_reset -> "Connessione azzerata"
        R.string.playlist_sent_confirmed -> "Playlist inviata e presente sul dispositivo."
        R.string.playlist_sent_refreshing -> "Playlist inviata. Il dispositivo sta aggiornando il catalogo."
        R.string.cannot_read_music_catalog -> "Impossibile leggere il catalogo musicale"
        R.string.tracks_missing_from_device -> "Una o più tracce non sono presenti nel dispositivo"
        R.string.duplicate_device_playlists -> "Esistono più playlist sul dispositivo con questo nome. Sincronizzale prima di inviare."
        R.string.playlist_sent_updating -> "Playlist inviata. Aggiorno i contenuti del dispositivo…"
        R.string.cannot_send_playlist -> "Impossibile inviare la playlist"
        R.string.playlist_removed_updating -> "Playlist rimossa. Aggiorno il catalogo…"
        R.string.cannot_remove_device_playlist -> "Impossibile rimuovere la playlist dal dispositivo"
        else -> ""
    }

    private val _uiState = MutableStateFlow(
        WearableUiState(
            lastDeviceAddress = preferences.getString(KEY_LAST_ADDRESS, null),
            lastDeviceName = preferences.getString(KEY_LAST_NAME, null)
        )
    )
    val uiState: StateFlow<WearableUiState> = _uiState.asStateFlow()
    private var scanTimeoutJob: Job? = null
    private var catalogJob: Job? = null
    private var writeJob: Job? = null
    private var disconnectJob: Job? = null
    private var disconnectFeedbackJob: Job? = null
    private var pendingPlaylistName: String? = null
    private var pendingPlaylistId: Long? = null

    init {
        mdsClient.onConnectedDeviceRemoved = {
            if (_uiState.value.connectionStatus == MdsConnectionStatus.CONNECTED ||
                _uiState.value.connectionStatus == MdsConnectionStatus.CONNECTING
            ) {
                catalogJob?.cancel()
                writeJob?.cancel()
                mdsClient.close()
                _uiState.update {
                    it.copy(
                        connectionStatus = MdsConnectionStatus.FAILED,
                        connectedSerial = null,
                        tracks = emptyList(),
                        remotePlaylists = emptyList(),
                        isLoadingMusic = false,
                        catalogProgress = null,
                        playlistReadWarning = null,
                        isDeviceDataLoaded = false,
                        isWritingPlaylist = false,
                        successMessage = null,
                        errorMessage = text(R.string.mds_session_closed)
                    )
                }
            }
        }
        viewModelScope.launch {
            scanner.devices.collect { devices ->
                _uiState.update { it.copy(devices = devices) }
            }
        }
        viewModelScope.launch {
            scanner.scanError.collect { errorCode ->
                if (errorCode != null) {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            errorMessage = when (errorCode) {
                                -1 -> text(R.string.ble_scanner_unavailable)
                                -2 -> text(R.string.bluetooth_permission_unavailable)
                                else -> text(R.string.bluetooth_scan_failed, errorCode)
                            }
                        )
                    }
                }
            }
        }
    }

    fun startScan() {
        scanTimeoutJob?.cancel()
        _uiState.update { it.copy(isScanning = true, errorMessage = null) }
        scanner.start()
        scanTimeoutJob = viewModelScope.launch {
            delay(15_000)
            if (_uiState.value.isScanning) {
                scanner.stop()
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = if (it.devices.isEmpty()) {
                            text(R.string.no_ble_device_found)
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    fun stopScan() {
        scanTimeoutJob?.cancel()
        scanner.stop()
        _uiState.update { it.copy(isScanning = false) }
    }

    fun reportError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun connect(address: String) {
        stopScan()
        disconnectFeedbackJob?.cancel()
        val serialHint = _uiState.value.devices
            .firstOrNull { it.address.equals(address, ignoreCase = true) }
            ?.name
            ?.let { serialHintFromName(it) }
        _uiState.update {
            it.copy(
                connectionStatus = MdsConnectionStatus.CONNECTING,
                errorMessage = null,
                tracks = emptyList(),
                remotePlaylists = emptyList(),
                playlistReadWarning = null,
                isDeviceDataLoaded = false
            )
        }

        viewModelScope.launch {
            try {
                val serial = withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
                    mdsClient.connect(address, serial = serialHint.orEmpty())
                } ?: error(text(R.string.mds_handshake_timeout))
                if (_uiState.value.connectionStatus != MdsConnectionStatus.CONNECTING) return@launch
                val deviceName = _uiState.value.devices
                    .firstOrNull { it.address.equals(address, ignoreCase = true) }
                    ?.name
                    ?.takeIf { it.isNotBlank() }
                    ?: _uiState.value.lastDeviceName
                _uiState.update {
                    it.copy(
                        connectionStatus = MdsConnectionStatus.CONNECTED,
                        connectedSerial = serial,
                        successMessage = null,
                        errorMessage = null,
                        lastDeviceAddress = address,
                        lastDeviceName = deviceName
                    )
                }
                preferences.edit()
                    .putString(KEY_LAST_ADDRESS, address)
                    .putString(KEY_LAST_NAME, deviceName)
                    .apply()
                loadMusic()
            } catch (exception: Exception) {
                if (_uiState.value.connectionStatus != MdsConnectionStatus.CONNECTING) return@launch
                _uiState.update {
                    it.copy(
                        connectionStatus = MdsConnectionStatus.FAILED,
                        errorMessage = exception.message
                            ?: text(R.string.cannot_connect_device)
                    )
                }
            }
        }
    }

    private companion object {
        const val CONNECTION_TIMEOUT_MS = 20_000L
        const val KEY_LAST_ADDRESS = "last_device_address"
        const val KEY_LAST_NAME = "last_device_name"

        private val SERIAL_IN_DEVICE_NAME = Regex("(?i)(?<![0-9a-z])[0-9a-z]{8,}(?![0-9a-z])")

        fun serialHintFromName(name: String): String? =
            SERIAL_IN_DEVICE_NAME.find(name)?.value
    }

    fun disconnect() {
        if (_uiState.value.connectionStatus != MdsConnectionStatus.CONNECTED &&
            _uiState.value.connectionStatus != MdsConnectionStatus.CONNECTING
        ) return

        stopScan()
        catalogJob?.cancel()
        writeJob?.cancel()
        disconnectJob?.cancel()
        disconnectFeedbackJob?.cancel()
        preferences.edit().remove(KEY_LAST_NAME).apply()
        _uiState.update {
            it.copy(
                connectionStatus = MdsConnectionStatus.DISCONNECTING,
                tracks = emptyList(),
                remotePlaylists = emptyList(),
                isLoadingMusic = false,
                catalogProgress = null,
                playlistReadWarning = null,
                isDeviceDataLoaded = false,
                successMessage = null,
                errorMessage = null
            )
        }
        disconnectJob = viewModelScope.launch {
            try {
                mdsClient.disconnect()
            } catch (exception: Exception) {
                pendingPlaylistName = null
                pendingPlaylistId = null
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: text(R.string.disconnect_error))
                }
            } finally {
                _uiState.update {
                    it.copy(
                        connectionStatus = MdsConnectionStatus.DISCONNECTED,
                        connectedSerial = null,
                        isWritingPlaylist = false,
                        lastDeviceName = null,
                        successMessage = if (it.errorMessage == null) {
                            text(R.string.device_disconnected)
                        } else {
                            null
                        }
                    )
                }
                if (_uiState.value.successMessage == text(R.string.device_disconnected)) {
                    clearFeedbackAfterDelay(text(R.string.device_disconnected))
                }
            }
        }
    }

    fun forceDisconnect() {
        stopScan()
        catalogJob?.cancel()
        writeJob?.cancel()
        disconnectJob?.cancel()
        disconnectFeedbackJob?.cancel()
        pendingPlaylistName = null
        pendingPlaylistId = null
        preferences.edit().remove(KEY_LAST_NAME).apply()
        mdsClient.close()
        _uiState.update {
            it.copy(
                connectionStatus = MdsConnectionStatus.DISCONNECTED,
                connectedSerial = null,
                lastDeviceName = null,
                tracks = emptyList(),
                remotePlaylists = emptyList(),
                isLoadingMusic = false,
                isWritingPlaylist = false,
                catalogProgress = null,
                playlistReadWarning = null,
                isDeviceDataLoaded = false,
                successMessage = text(R.string.connection_reset),
                errorMessage = null
            )
        }
        clearFeedbackAfterDelay(text(R.string.connection_reset))
    }

    private fun clearFeedbackAfterDelay(message: String) {
        disconnectFeedbackJob?.cancel()
        disconnectFeedbackJob = viewModelScope.launch {
            delay(3_000)
            _uiState.update {
                if (it.successMessage == message) it.copy(successMessage = null) else it
            }
        }
    }

    fun loadMusic() {
        if (_uiState.value.connectionStatus != MdsConnectionStatus.CONNECTED ||
            _uiState.value.isLoadingMusic
        ) return

        _uiState.update {
            it.copy(
                isLoadingMusic = true,
                catalogProgress = null,
                playlistReadWarning = null,
                errorMessage = null
            )
        }
        catalogJob = viewModelScope.launch {
            try {
                val catalog = mdsClient.readCatalog { progress ->
                    _uiState.update { it.copy(catalogProgress = progress) }
                }
                val expectedName = pendingPlaylistName
                val expectedId = pendingPlaylistId
                pendingPlaylistName = null
                pendingPlaylistId = null
                _uiState.update {
                    val playlistConfirmed = (expectedName != null || expectedId != null) &&
                        catalog.playlists.any { remote ->
                            remote.id == expectedId ||
                                (expectedName != null &&
                                    remote.name.trim().equals(expectedName.trim(), ignoreCase = true))
                        }
                    it.copy(
                        isLoadingMusic = false,
                        catalogProgress = null,
                        isDeviceDataLoaded = true,
                        tracks = catalog.tracks,
                        remotePlaylists = catalog.playlists,
                        playlistReadWarning = catalog.playlistReadWarning,
                        successMessage = when {
                            expectedName == null && expectedId == null -> it.successMessage
                            playlistConfirmed -> text(R.string.playlist_sent_confirmed)
                            else -> text(R.string.playlist_sent_refreshing)
                        }
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingMusic = false,
                        catalogProgress = null,
                        playlistReadWarning = null,
                            successMessage = null,
                            errorMessage = exception.message
                            ?: text(R.string.cannot_read_music_catalog)
                    )
                }
            }
        }
    }

    fun writePlaylist(
        playlist: Playlist,
        onRemoteIdAssigned: (Long) -> Unit = {}
    ) {
        val remoteTracks = _uiState.value.tracks
        if (_uiState.value.connectionStatus != MdsConnectionStatus.CONNECTED ||
            remoteTracks.isEmpty() || _uiState.value.isWritingPlaylist
        ) return

        val keys = PlaylistDeviceMatcher.resolveKeys(playlist, remoteTracks)

        if (keys == null) {
            _uiState.update {
                it.copy(
                    errorMessage = text(R.string.tracks_missing_from_device)
                )
            }
            return
        }

        val matchingRemotePlaylists = _uiState.value.remotePlaylists.filter {
            it.name.trim().equals(playlist.name.trim(), ignoreCase = true)
        }
        if (playlist.remotePlaylistId == null && matchingRemotePlaylists.size > 1) {
            _uiState.update {
                it.copy(errorMessage = text(R.string.duplicate_device_playlists))
            }
            return
        }
        val targetRemoteId = playlist.remotePlaylistId ?: matchingRemotePlaylists.singleOrNull()?.id

        _uiState.update {
            it.copy(
                isWritingPlaylist = true,
                errorMessage = null,
                successMessage = null
            )
        }
        writeJob = viewModelScope.launch {
            pendingPlaylistName = playlist.name
            try {
                val savedRemoteId = mdsClient.writePlaylist(
                    remotePlaylistId = targetRemoteId,
                    playlistName = playlist.name,
                    songKeys = keys
                )
                pendingPlaylistId = savedRemoteId
                onRemoteIdAssigned(savedRemoteId)
                _uiState.update { current ->
                    val written = RemotePlaylist(
                        id = savedRemoteId,
                        name = playlist.name,
                        songKeys = keys
                    )
                    val existing = current.remotePlaylists
                        .filterNot { it.id == savedRemoteId || it.id == targetRemoteId }
                    current.copy(
                        isWritingPlaylist = false,
                        remotePlaylists = existing + written,
                        successMessage = text(R.string.playlist_sent_updating)
                    )
                }
                // La risposta della PUT conferma la richiesta, non la lista
                // effettivamente esposta dal dispositivo: rileggiamola subito.
                delay(750)
                loadMusic()
            } catch (exception: Exception) {
                pendingPlaylistName = null
                _uiState.update {
                    it.copy(
                        isWritingPlaylist = false,
                        successMessage = null,
                        errorMessage = exception.message
                            ?: text(R.string.cannot_send_playlist)
                    )
                }
            }
        }
    }

    fun removePlaylist(remotePlaylistId: Long) {
        if (_uiState.value.connectionStatus != MdsConnectionStatus.CONNECTED ||
            _uiState.value.isWritingPlaylist
        ) return

        _uiState.update {
            it.copy(
                isWritingPlaylist = true,
                errorMessage = null,
                successMessage = null
            )
        }
        writeJob = viewModelScope.launch {
            try {
                mdsClient.deletePlaylist(remotePlaylistId)
                _uiState.update {
                    it.copy(
                        isWritingPlaylist = false,
                        successMessage = text(R.string.playlist_removed_updating)
                    )
                }
                delay(750)
                loadMusic()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isWritingPlaylist = false,
                        successMessage = null,
                        errorMessage = exception.message
                            ?: text(R.string.cannot_remove_device_playlist)
                    )
                }
            }
        }
    }

    override fun onCleared() {
        scanner.stop()
        mdsClient.close()
        super.onCleared()
    }
}
