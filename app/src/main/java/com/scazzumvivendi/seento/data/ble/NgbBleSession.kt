package com.scazzumvivendi.seento.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattConnectionSettings
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.os.Build
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.movesense.mds.BLEWrapper
import com.suunto.komposti.NGBLEDelegate
import com.suunto.komposti.NGBLEWrapper
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import java.util.ArrayDeque
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Direct GATT session bridged into the MDS library's native Whiteboard transport. */
internal class NgbBleSession(context: Context) : Closeable {

    val whiteboardAddress: String
        get() = WB_ADDRESS

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val decoder = WhiteboardSlipCodec.Decoder()
    private val writeQueue = ArrayDeque<WriteChunk>()
    private val queueLock = Any()

    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var ngBle: NGBLEWrapper? = null
    private var connectionContinuation: CancellableContinuation<Unit>? = null
    private var setupJob: Job? = null
    private var currentWrite: WriteChunk? = null
    private var writeInProgress = false
    private var closed = false
    private var onReady: (suspend () -> Unit)? = null

    @SuppressLint("MissingPermission")
    suspend fun connect(address: String, onReady: suspend () -> Unit) =
        suspendCancellableCoroutine { continuation ->
            connectionContinuation = continuation
            this.onReady = onReady
            try {
                val manager = appContext.getSystemService(BluetoothManager::class.java)
                    ?: error("BluetoothManager non disponibile")
                val device = manager.adapter?.getRemoteDevice(address)
                    ?: error("Adattatore Bluetooth non disponibile")
                gatt = if (Build.VERSION.SDK_INT >= 37) {
                    val settings = BluetoothGattConnectionSettings.Builder()
                        .setAutoConnectEnabled(false)
                        .setTransport(BluetoothDevice.TRANSPORT_LE)
                        .build()
                    device.connectGatt(settings, appContext.mainExecutor, gattCallback)
                } else {
                    @Suppress("DEPRECATION")
                    device.connectGatt(
                        appContext,
                        false,
                        gattCallback,
                        BluetoothDevice.TRANSPORT_LE
                    )
                } ?: error("Impossibile avviare la connessione GATT")
            } catch (error: Exception) {
                continuation.resumeWithException(error)
            }
            continuation.invokeOnCancellation { close() }
        }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(
            bluetoothGatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (bluetoothGatt !== gatt || closed) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                fail(IllegalStateException("Connessione GATT fallita (status=$status)"))
            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                val requested = bluetoothGatt.requestConnectionPriority(
                    BluetoothGatt.CONNECTION_PRIORITY_HIGH
                )
                Log.i(TAG, "Requested high BLE connection priority: $requested")
                Log.i(TAG, "GATT connected; discovering MDS service")
                if (!bluetoothGatt.discoverServices()) {
                    fail(IllegalStateException("Ricerca dei servizi GATT non avviata"))
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                fail(IllegalStateException("Il dispositivo ha chiuso la connessione BLE"))
            }
        }

        override fun onServicesDiscovered(bluetoothGatt: BluetoothGatt, status: Int) {
            if (bluetoothGatt !== gatt || closed) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                fail(IllegalStateException("Ricerca servizi GATT fallita (status=$status)"))
                return
            }
            val service = bluetoothGatt.getService(MDS_SERVICE_UUID)
            writeCharacteristic = service?.getCharacteristic(MDS_WRITE_UUID)
            notifyCharacteristic = service?.getCharacteristic(MDS_NOTIFY_UUID)
            val notify = notifyCharacteristic
            if (service == null || writeCharacteristic == null || notify == null) {
                fail(IllegalStateException("Servizio MDS BLE incompleto sul dispositivo"))
                return
            }
            // The watch's initial Whiteboard hello exceeds ATT's default 20-byte payload.
            // Enable notifications only after MTU negotiation, otherwise Android can
            // receive a truncated hello and the native route is never established.
            if (!bluetoothGatt.requestMtu(247)) {
                fail(IllegalStateException("Negoziazione MTU non avviata"))
            }
        }

        override fun onMtuChanged(bluetoothGatt: BluetoothGatt, mtu: Int, status: Int) {
            if (bluetoothGatt !== gatt || closed) return
            Log.i(TAG, "Negotiated ATT MTU: $mtu status=$status")
            if (status != BluetoothGatt.GATT_SUCCESS || mtu < 64) {
                fail(IllegalStateException("MTU BLE insufficiente: $mtu (status=$status)"))
                return
            }
            val notify = notifyCharacteristic ?: return
            if (!bluetoothGatt.setCharacteristicNotification(notify, true)) {
                fail(IllegalStateException("Impossibile abilitare le notifiche MDS"))
                return
            }
            val descriptor = notify.getDescriptor(CCCD_UUID)
            if (descriptor == null || !writeCccd(bluetoothGatt, descriptor)) {
                fail(IllegalStateException("Impossibile avviare le notifiche MDS"))
            }
        }

        override fun onDescriptorWrite(
            bluetoothGatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (bluetoothGatt !== gatt || closed) return
            if (descriptor.uuid != CCCD_UUID || status != BluetoothGatt.GATT_SUCCESS) {
                fail(IllegalStateException("Attivazione notifiche MDS fallita (status=$status)"))
                return
            }
            setupJob = scope.launch {
                try {
                    initializeNativeBridge()
                    onReady?.invoke()
                    connectionContinuation?.let { continuation ->
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    connectionContinuation = null
                } catch (error: Exception) {
                    fail(error)
                }
            }
        }

        override fun onCharacteristicWrite(
            bluetoothGatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (bluetoothGatt === gatt && characteristic.uuid == MDS_WRITE_UUID && !closed) {
                onWriteCompleted(status == BluetoothGatt.GATT_SUCCESS)
            }
        }

        @Suppress("DEPRECATION")
        @Deprecated("Android 13 callback retained for older supported devices")
        override fun onCharacteristicChanged(
            bluetoothGatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (bluetoothGatt === gatt && characteristic.uuid == MDS_NOTIFY_UUID) {
                characteristic.value?.let(::onNotification)
            }
        }

        override fun onCharacteristicChanged(
            bluetoothGatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (bluetoothGatt === gatt && characteristic.uuid == MDS_NOTIFY_UUID) {
                onNotification(value)
            }
        }
    }

    private fun initializeNativeBridge() {
        val bridge = NGBLEWrapper()
        bridge.setDelegate(object : NGBLEDelegate {
            override fun connectCb(wbAddress: String): Boolean {
                Log.i(TAG, "Native Whiteboard connect callback: address=$wbAddress")
                mainHandler.post {
                    if (!closed) bridge.connectCompleted(wbAddress, true)
                }
                return true
            }

            override fun cancelConnectCb(wbAddress: String): Boolean = true

            override fun disconnectCb(wbAddress: String): Boolean {
                // This is the native teardown handshake. Acknowledge it before close()
                // releases the bridge/GATT; posting it to the main queue races with close
                // setting `closed` and used to leave MDS in a stale connected state.
                return runCatching {
                    bridge.disconnectCompleted(wbAddress, true)
                    Log.i(TAG, "Native Whiteboard disconnect acknowledged: address=$wbAddress")
                    true
                }.getOrElse { error ->
                    Log.e(TAG, "Native Whiteboard disconnect acknowledgement failed", error)
                    false
                }
            }

            override fun sendCb(
                wbAddress: String,
                data: ByteArray,
                size: Int,
                requestId: Long
            ): Boolean {
                Log.d(TAG, "Native Whiteboard send: address=$wbAddress bytes=$size requestId=$requestId")
                return enqueueWhiteboardPacket(data.copyOf(size), requestId)
            }

            override fun cancelSendCb(requestId: Long): Boolean = true

            override fun deviceToWhiteboardCb(deviceId: Int): Any =
                BLEWrapper.WbAddress(deviceId.toUInt().toString(16))

            override fun whiteboardToDevice(wbAddress: String): Int =
                wbAddress.toIntOrNull(16) ?: DEFAULT_WB_HANDLE

            override fun getConnectedBleDevicesCb(): Any =
                BLEWrapper.ConnectedDevices(arrayOf(WB_ADDRESS, "", ""))
        })
        ngBle = bridge
        bridge.bypassConnect(WB_ADDRESS)
        Log.i(TAG, "Native MDS transport attached at $WB_ADDRESS")
    }

    private fun onNotification(bytes: ByteArray) {
        Log.d(TAG, "RX raw=${bytes.joinToString("") { "%02x".format(it) }}")
        val frames = decoder.append(bytes)
        Log.d(TAG, "MDS BLE notification: bytes=${bytes.size} frames=${frames.size}")
        frames.forEach { frame ->
            Log.d(TAG, "MDS Whiteboard frame received: bytes=${frame.size}")
            ngBle?.dataReceived(WB_ADDRESS, frame, frame.size)
        }
    }

    private fun enqueueWhiteboardPacket(packet: ByteArray, requestId: Long): Boolean {
        if (closed || writeCharacteristic == null || gatt == null) {
            ngBle?.sendCompleted(requestId, false)
            return false
        }
        val encoded = WhiteboardSlipCodec.encode(packet)
        Log.d(TAG, "TX raw=${encoded.joinToString("") { "%02x".format(it) }}")
        synchronized(queueLock) {
            var offset = 0
            while (offset < encoded.size) {
                val end = minOf(offset + MAX_BLE_WRITE_BYTES, encoded.size)
                writeQueue.addLast(
                    WriteChunk(
                        data = encoded.copyOfRange(offset, end),
                        requestId = requestId,
                        isLast = end == encoded.size
                    )
                )
                offset = end
            }
        }
        writeNextChunk()
        return true
    }

    @SuppressLint("MissingPermission")
    private fun writeNextChunk() {
        val next = synchronized(queueLock) {
            if (writeInProgress || writeQueue.isEmpty() || closed) return
            writeInProgress = true
            writeQueue.removeFirst().also { currentWrite = it }
        }
        val bluetoothGatt = gatt ?: run {
            onWriteCompleted(false)
            return
        }
        val characteristic = writeCharacteristic ?: run {
            onWriteCompleted(false)
            return
        }
        val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bluetoothGatt.writeCharacteristic(
                characteristic,
                next.data,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            ) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            characteristic.value = next.data
            @Suppress("DEPRECATION")
            bluetoothGatt.writeCharacteristic(characteristic)
        }
        if (!started) onWriteCompleted(false)
    }

    private fun onWriteCompleted(success: Boolean) {
        val completed = synchronized(queueLock) {
            val previous = currentWrite
            currentWrite = null
            writeInProgress = false
            previous
        }
        if (completed?.isLast == true) {
            ngBle?.sendCompleted(completed.requestId, success)
        }
        writeNextChunk()
    }

    private fun writeCccd(
        bluetoothGatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor
    ): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        bluetoothGatt.writeDescriptor(
            descriptor,
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        ) == BluetoothStatusCodes.SUCCESS
    } else {
        @Suppress("DEPRECATION")
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        @Suppress("DEPRECATION")
        bluetoothGatt.writeDescriptor(descriptor)
    }

    private fun fail(error: Exception) {
        Log.e(TAG, "BLE/MDS session failed", error)
        connectionContinuation?.let { continuation ->
            if (continuation.isActive) continuation.resumeWithException(error)
        }
        connectionContinuation = null
        close()
    }

    @SuppressLint("MissingPermission")
    override fun close() {
        if (closed) return
        closed = true
        setupJob?.cancel()
        runCatching { ngBle?.bypassDisconnect(WB_ADDRESS) }
        runCatching { ngBle?.close() }
        ngBle = null
        val oldGatt = gatt
        gatt = null
        runCatching { oldGatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED) }
        runCatching { oldGatt?.disconnect() }
        runCatching { oldGatt?.close() }
        synchronized(queueLock) {
            writeQueue.clear()
            currentWrite = null
            writeInProgress = false
        }
    }

    private data class WriteChunk(
        val data: ByteArray,
        val requestId: Long,
        val isLast: Boolean
    )

    companion object {
        const val TAG = "SeentoNGBLE"
        const val WB_ADDRESS = "10000001"
        const val WHITEBOARD_ADDRESS = WB_ADDRESS
        const val DEFAULT_WB_HANDLE = 0x10000001
        const val MAX_BLE_WRITE_BYTES = 20
        val MDS_SERVICE_UUID: UUID = UUID.fromString("61353090-8231-49cc-b57a-886370740041")
        val MDS_WRITE_UUID: UUID = UUID.fromString("17816557-5652-417f-909f-3aee61e5fa85")
        val MDS_NOTIFY_UUID: UUID = UUID.fromString("34802252-7185-4d5d-b431-630e7050e8f0")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
