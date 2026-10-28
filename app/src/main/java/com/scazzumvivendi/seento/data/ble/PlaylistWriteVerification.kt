package com.scazzumvivendi.seento.data.ble

import com.scazzumvivendi.seento.data.ble.model.RemotePlaylist
import kotlinx.coroutines.delay

internal object PlaylistWriteVerification {
    suspend fun confirm(
        id: Long,
        name: String,
        songKeys: List<Long>,
        read: suspend () -> List<RemotePlaylist>
    ): Boolean {
        repeat(5) { attempt ->
            if (attempt > 0) delay(1_000)
            if (read().any { it.id == id && it.name == name && it.songKeys == songKeys }) {
                return true
            }
        }
        return false
    }
}
