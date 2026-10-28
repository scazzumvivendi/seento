package com.scazzumvivendi.seento.data.ble

internal object WatchPlaylistPolicy {
    fun isUserPlaylist(name: String): Boolean =
        !name.equals(ALL_SONGS_NAME, ignoreCase = true)

    private const val ALL_SONGS_NAME = "All songs"
}
