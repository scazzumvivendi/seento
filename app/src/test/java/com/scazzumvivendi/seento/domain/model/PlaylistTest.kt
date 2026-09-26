package com.scazzumvivendi.seento.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistTest {

    private val firstTrack = Track(
        id = 1,
        title = "First song",
        path = "/music/first.mp3"
    )

    private val secondTrack = Track(
        id = 2,
        title = "Second song",
        path = "/music/second.mp3"
    )

    @Test
    fun `new playlist is empty`() {
        val playlist = Playlist(
            id = 1,
            name = "My playlist"
        )

        assertTrue(playlist.tracks.isEmpty())
    }

    @Test
    fun `adding tracks increases playlist size`() {
        val playlist = Playlist(
            id = 2,
            name = "My playlist"
        )

        val updatedPlaylist = playlist
            .addTrack(firstTrack)
            .addTrack(secondTrack)

        assertEquals(2, updatedPlaylist.tracks.size)
        assertEquals("First song", updatedPlaylist.tracks[0].title)
        assertEquals("Second song", updatedPlaylist.tracks[1].title)
    }

    @Test
    fun `removing a track removes only the selected track`() {
        val playlist = Playlist(
            id = 1,
            name = "My playlist",
            tracks = listOf(firstTrack, secondTrack)
        )

        val updatedPlaylist = playlist.removeTrack(1)

        assertEquals(1, updatedPlaylist.tracks.size)
        assertEquals(2, updatedPlaylist.tracks[0].id)
    }

    @Test
    fun `removing a track by position preserves duplicate ids`() {
        val playlist = Playlist(
            id = 1,
            name = "My playlist",
            tracks = listOf(firstTrack, firstTrack, secondTrack)
        )

        val updatedPlaylist = playlist.removeTrackAt(1)

        assertEquals(listOf(firstTrack, secondTrack), updatedPlaylist.tracks)
    }

    @Test
    fun `moving a track changes its position`() {
        val playlist = Playlist(
            id = 2,
            name = "My playlist",
            tracks = listOf(firstTrack, secondTrack)
        )

        val reorderedPlaylist = playlist.moveTrack(
            fromIndex = 0,
            toIndex = 1
        )

        assertEquals(2, reorderedPlaylist.tracks[0].id)
        assertEquals(1, reorderedPlaylist.tracks[1].id)
    }
}
