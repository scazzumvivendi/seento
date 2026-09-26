package com.scazzumvivendi.seento.domain.model

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
class PlaylistLibraryTest {

    private val firstPlaylist = Playlist(
        id = 1L,
        name = "Workout"
    )

    private val secondPlaylist = Playlist(
        id = 2L,
        name = "Favorites"
    )

    @Test
    fun `new library is empty`() {
        val library = PlaylistLibrary()

        Assert.assertTrue(library.playlists.isEmpty())
    }

    @Test
    fun `adding playlists increases library size`() {
        val library = PlaylistLibrary()
            .addPlaylist(firstPlaylist)
            .addPlaylist(secondPlaylist)

        assertEquals(2, library.playlists.size)
    }

    @Test
    fun `removing playlist removes only selected playlist`() {
        val library = PlaylistLibrary(
            playlists = listOf(firstPlaylist, secondPlaylist)
        )

        val updatedLibrary = library.removePlaylist(1L)

        assertEquals(1, updatedLibrary.playlists.size)
        assertEquals(2L, updatedLibrary.playlists[0].id)
    }

    @Test
    fun `renaming playlist changes only its name`() {
        val library = PlaylistLibrary(
            playlists = listOf(firstPlaylist, secondPlaylist)
        )

        val updatedLibrary = library.renamePlaylist(
            playlistId = 1L,
            newName = "Morning workout"
        )

        assertEquals(
            "Morning workout",
            updatedLibrary.playlists[0].name
        )

        assertEquals(
            "Favorites",
            updatedLibrary.playlists[1].name
        )
    }

    @Test
    fun `find playlist returns matching playlist`() {
        val library = PlaylistLibrary(
            playlists = listOf(firstPlaylist, secondPlaylist)
        )

        val result = library.findPlaylist(2L)

        assertEquals("Favorites", result?.name)
    }
}