package com.scazzumvivendi.seento.ui.wearable

import com.scazzumvivendi.seento.data.ble.BleDevice
import com.scazzumvivendi.seento.data.ble.MdsConnectionStatus
import com.scazzumvivendi.seento.data.ble.model.RemoteMusicTrack
import com.scazzumvivendi.seento.data.ble.model.RemotePlaylist
import com.scazzumvivendi.seento.data.ble.model.CatalogReadProgress

data class WearableUiState(
    val isScanning: Boolean = false,
    val devices: List<BleDevice> = emptyList(),
    val connectionStatus: MdsConnectionStatus = MdsConnectionStatus.DISCONNECTED,
    val connectedSerial: String? = null,
    val lastDeviceAddress: String? = null,
    val lastDeviceName: String? = null,
    val isLoadingMusic: Boolean = false,
    val catalogProgress: CatalogReadProgress? = null,
    val isDeviceDataLoaded: Boolean = false,
    val isWritingPlaylist: Boolean = false,
    val tracks: List<RemoteMusicTrack> = emptyList(),
    val remotePlaylists: List<RemotePlaylist> = emptyList(),
    val playlistReadWarning: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
