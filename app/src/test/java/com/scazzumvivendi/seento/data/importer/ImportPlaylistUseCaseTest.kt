package com.scazzumvivendi.seento.data.importer

import com.scazzumvivendi.seento.data.parser.M3uParser
import com.scazzumvivendi.seento.domain.model.Playlist
import com.scazzumvivendi.seento.domain.model.Track
import com.scazzumvivendi.seento.domain.repository.PlaylistRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportPlaylistUseCaseTest {

    @Test
    fun `parses and saves imported playlist`() = runTest {
        val repository = RecordingPlaylistRepository()
        val useCase = ImportPlaylistUseCase(
            parser = M3uParser(),
            repository = repository
        )

        val result = useCase(
            content = """
                #EXTM3U
                #EXTINF:120,First song
                /music/first.mp3
            """.trimIndent(),
            playlistName = "Imported"
        )

        assertEquals("Imported", repository.savedName)
        assertEquals(1, repository.savedTracks.size)
        assertEquals("First song", repository.savedTracks[0].title)
        assertEquals("/music/first.mp3", result.tracks[0].path)
    }

    private class RecordingPlaylistRepository : PlaylistRepository {

        var savedName: String? = null
        var savedTracks: List<Track> = emptyList()

        override suspend fun createPlaylist(
            name: String,
            tracks: List<Track>,
            remotePlaylistId: Long?
        ): Playlist {
            savedName = name
            savedTracks = tracks

            return Playlist(
                id = 1L,
                name = name,
                tracks = tracks
            )
        }

        override suspend fun getAllPlaylists(): List<Playlist> = emptyList()

        override suspend fun appendTracks(playlistId: Long, tracks: List<Track>) = Unit

        override suspend fun replacePlaylistTracks(
            playlistId: Long,
            tracks: List<Track>,
            remotePlaylistId: Long
        ) = Unit

        override suspend fun getPlaylist(
            playlistId: Long
        ): Playlist? = null

        override suspend fun renamePlaylist(
            playlistId: Long,
            newName: String
        ) = Unit

        override suspend fun setRemotePlaylistId(playlistId: Long, remotePlaylistId: Long) = Unit

        override suspend fun deletePlaylist(playlistId: Long) = Unit

        override suspend fun removeTrackAt(
            playlistId: Long,
            position: Int
        ) = Unit

        override suspend fun reorderTracks(
            playlistId: Long,
            orderedTrackIds: List<Long>
        ) = Unit
    }
}
