package com.scazzumvivendi.seento.data.ble

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchPlaylistPolicyTest {

    @Test
    fun `all songs system list is not imported as a playlist`() {
        assertFalse(WatchPlaylistPolicy.isUserPlaylist(65_535L, "All songs", 0))
        assertFalse(WatchPlaylistPolicy.isUserPlaylist(12L, "all songs", 0))
    }

    @Test
    fun `named device playlists remain importable`() {
        assertTrue(WatchPlaylistPolicy.isUserPlaylist(17L, "Running", 4))
    }
}
