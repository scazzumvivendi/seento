package com.scazzumvivendi.seento.data.ble

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchPlaylistPolicyTest {

    @Test
    fun `all songs system list is not imported as a playlist`() {
        assertFalse(WatchPlaylistPolicy.isUserPlaylist("All songs"))
        assertFalse(WatchPlaylistPolicy.isUserPlaylist("all songs"))
        assertFalse(WatchPlaylistPolicy.isUserPlaylist("ALL SONGS"))
    }

    @Test
    fun `named device playlists remain importable`() {
        assertTrue(WatchPlaylistPolicy.isUserPlaylist("Running"))
        assertTrue(WatchPlaylistPolicy.isUserPlaylist("Paolo Brosio"))
    }
}
