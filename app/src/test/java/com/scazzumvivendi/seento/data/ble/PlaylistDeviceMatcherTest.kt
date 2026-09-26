package com.scazzumvivendi.seento.data.ble

import com.scazzumvivendi.seento.data.ble.model.RemoteMusicTrack
import com.scazzumvivendi.seento.domain.model.Playlist
import com.scazzumvivendi.seento.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistDeviceMatcherTest {

    @Test
    fun `resolves keys preserving local playlist order`() {
        val playlist = Playlist(
            id = 1L,
            name = "Run",
            tracks = listOf(
                Track(1L, "Second", "second.mp3"),
                Track(2L, "First", "first.mp3")
            )
        )
        val remote = listOf(
            remoteTrack(10L, "/music/first.mp3", "First"),
            remoteTrack(20L, "/music/second.mp3", "Second")
        )

        assertEquals(
            listOf(20L, 10L),
            PlaylistDeviceMatcher.resolveKeys(playlist, remote)
        )
    }

    @Test
    fun `matches by basename when paths differ`() {
        val playlist = Playlist(
            id = 1L,
            name = "Run",
            tracks = listOf(Track(1L, null, "./folder/song.mp3"))
        )

        assertEquals(
            listOf(7L),
            PlaylistDeviceMatcher.resolveKeys(
                playlist,
                listOf(remoteTrack(7L, "/watch/song.mp3", "Other title"))
            )
        )
    }

    @Test
    fun `returns null when one track is missing`() {
        val playlist = Playlist(
            id = 1L,
            name = "Run",
            tracks = listOf(Track(1L, "Missing", "missing.mp3"))
        )

        assertNull(PlaylistDeviceMatcher.resolveKeys(playlist, emptyList()))
    }

    @Test
    fun `does not map two same-title local tracks to one device key`() {
        val playlist = Playlist(
            id = 1L,
            name = "Run",
            tracks = listOf(
                Track(1L, "Intro", "first/intro.mp3"),
                Track(2L, "Intro", "second/intro.mp3")
            )
        )

        assertEquals(
            listOf(7L, 8L),
            PlaylistDeviceMatcher.resolveKeys(
                playlist,
                listOf(
                    remoteTrack(7L, "/watch/first/intro.mp3", "Intro"),
                    remoteTrack(8L, "/watch/second/intro.mp3", "Intro")
                )
            )
        )
    }

    private fun remoteTrack(key: Long, path: String, title: String) =
        RemoteMusicTrack(key, path, title, null, null, null)
}
