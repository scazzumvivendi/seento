package com.scazzumvivendi.seento.data.ble

data class BleDevice(
    val name: String,
    val address: String,
    val isLikelyCandidate: Boolean = name.contains("suunto", ignoreCase = true)
)
