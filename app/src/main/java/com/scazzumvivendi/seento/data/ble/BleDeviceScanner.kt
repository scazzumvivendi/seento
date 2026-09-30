package com.scazzumvivendi.seento.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.scazzumvivendi.seento.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleDeviceScanner(context: Context) {

    private val appContext = context.applicationContext

    private fun localizedString(id: Int, value: String): String =
        ContextCompat.getContextForLanguage(appContext).getString(id, value)

    private val mdsServiceUuid = ParcelUuid.fromString(
        "61353090-8231-49cc-b57a-886370740041"
    )

    private val bluetoothManager = context
        .getSystemService(BluetoothManager::class.java)

    private val adapter = bluetoothManager?.adapter

    private val scanner: BluetoothLeScanner?
        get() = adapter?.bluetoothLeScanner

    private val _devices = MutableStateFlow<List<BleDevice>>(emptyList())
    val devices: StateFlow<List<BleDevice>> = _devices.asStateFlow()

    private val _scanError = MutableStateFlow<Int?>(null)
    val scanError: StateFlow<Int?> = _scanError.asStateFlow()

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            addDevice(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::addDevice)
        }

        override fun onScanFailed(errorCode: Int) {
            _scanError.value = errorCode
            _devices.value = emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        _devices.value = emptyList()
        _scanError.value = null
        val activeScanner = scanner
        if (activeScanner == null) {
            _scanError.value = -1
            return
        }
        try {
            activeScanner.startScan(
                null,
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build(),
                callback
            )
        } catch (_: SecurityException) {
            _scanError.value = -2
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        scanner?.stopScan(callback)
    }

    @SuppressLint("MissingPermission")
    private fun addDevice(result: ScanResult) {
        val device = result.device
        val name = device.name
            ?: result.scanRecord?.deviceName
            ?: localizedString(R.string.ble_device_name, device.address.takeLast(5))

        addDevice(
            device = device,
            name = name,
            hasMdsService = result.scanRecord?.serviceUuids
                ?.contains(mdsServiceUuid) == true
        )
    }

    private fun addDevice(
        device: BluetoothDevice,
        name: String,
        hasMdsService: Boolean
    ) {
        val discovered = BleDevice(
            name = name,
            address = device.address,
            isLikelyCandidate = name.contains("suunto", ignoreCase = true) ||
                hasMdsService
        )

        _devices.value = (_devices.value
            .filterNot { it.address == discovered.address } + discovered)
            .sortedBy { it.name.lowercase() }
    }
}
