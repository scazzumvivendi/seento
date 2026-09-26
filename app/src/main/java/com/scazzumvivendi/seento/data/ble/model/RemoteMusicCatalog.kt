package com.scazzumvivendi.seento.data.ble.model

data class RemoteMusicCatalog(
    val serial: String,
    val playlistName: String,
    val tracks: List<RemoteMusicTrack>,
    val playlists: List<RemotePlaylist> = emptyList(),
    val playlistReadWarning: String? = null
)

enum class CatalogReadPhase {
    PREPARING,
    READING_SONGS,
    READING_PLAYLISTS
}

data class CatalogReadProgress(
    val phase: CatalogReadPhase,
    val completed: Int = 0,
    val total: Int = 0
)

data class RemotePlaylist(
    val id: Long,
    val name: String,
    val songKeys: List<Long>
)

data class RemoteMusicTrack(
    val key: Long,
    val path: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMillis: Long?,
    val deviceKey: String = key.toString()
)
