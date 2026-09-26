package com.scazzumvivendi.seento.data.ble

internal object WatchPlaylistPolicy {
    fun isUserPlaylist(id: Long, name: String, sortId: Int): Boolean =
        id != ALL_SONGS_PLAYLIST_ID &&
            sortId != ALL_SONGS_SORT_ID &&
            !name.equals(ALL_SONGS_NAME, ignoreCase = true)

    private const val ALL_SONGS_PLAYLIST_ID = 65_535L
    private const val ALL_SONGS_SORT_ID = 0
    private const val ALL_SONGS_NAME = "All songs"
}
