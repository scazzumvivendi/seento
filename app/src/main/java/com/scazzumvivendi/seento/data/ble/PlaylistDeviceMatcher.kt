package com.scazzumvivendi.seento.data.ble

import com.scazzumvivendi.seento.data.ble.model.RemoteMusicTrack
import com.scazzumvivendi.seento.domain.model.Playlist

object PlaylistDeviceMatcher {

    fun resolveKeys(
        playlist: Playlist,
        remoteTracks: List<RemoteMusicTrack>
    ): List<Long>? {
        val usedKeys = mutableSetOf<Long>()
        val keys = playlist.tracks.mapNotNull { localTrack ->
            val localPath = normalize(localTrack.path)
            val localTitle = normalize(localTrack.title)

            val match = remoteTracks
                .asSequence()
                .filterNot { it.key in usedKeys }
                .mapNotNull { remoteTrack ->
                    val remotePath = normalize(remoteTrack.path)
                    val score = when {
                        localPath.isNotEmpty() && remotePath == localPath -> 4
                        localPath.isNotEmpty() && remotePath.endsWith("/$localPath") -> 3
                        localPath.isNotEmpty() && basename(remotePath) == basename(localPath) -> 2
                        localTitle.isNotEmpty() && normalize(remoteTrack.title) == localTitle -> 1
                        else -> 0
                    }
                    remoteTrack.takeIf { score > 0 }?.let { score to it }
                }
                .maxByOrNull { it.first }
                ?.second

            match?.key?.also { usedKeys += it }
        }

        return keys.takeIf { it.size == playlist.tracks.size }
    }

    private fun normalize(value: String?): String = value
        ?.trim()
        ?.replace('\\', '/')
        ?.lowercase()
        ?.removePrefix("./")
        ?: ""

    private fun basename(value: String): String =
        value.substringAfterLast('/')
}
