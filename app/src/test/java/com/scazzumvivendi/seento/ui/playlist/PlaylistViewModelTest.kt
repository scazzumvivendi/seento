package com.scazzumvivendi.seento.ui.playlist

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.scazzumvivendi.seento.domain.model.Playlist
import com.scazzumvivendi.seento.domain.model.Track
import com.scazzumvivendi.seento.domain.repository.PlaylistRepository
import com.scazzumvivendi.seento.data.importer.ImportPlaylistUseCase
import com.scazzumvivendi.seento.data.parser.M3uParser
import com.scazzumvivendi.seento.data.ble.model.RemoteMusicTrack
import com.scazzumvivendi.seento.data.ble.model.RemotePlaylist

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads playlists successfully`() = runTest {
        val playlist = Playlist(
            id = 1L,
            name = "Workout",
            tracks = listOf(
                Track(
                    id = 10L,
                    title = "First",
                    path = "/music/first.mp3"
                )
            )
        )

        val viewModel = PlaylistViewModel(
            repository = FakePlaylistRepository(
                playlists = listOf(playlist)
            ),
            importPlaylistUseCase = importUseCase()
        )

        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(
            listOf(playlist),
            viewModel.uiState.value.playlists
        )
    }

    @Test
    fun `empty repository produces empty state`() = runTest {
        val viewModel = PlaylistViewModel(
            repository = FakePlaylistRepository(),
            importPlaylistUseCase = importUseCase()
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.playlists.isEmpty())
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `merges watch playlists and does not duplicate device tracks`() = runTest {
        val existing = Playlist(
            id = 1L,
            name = "Running",
            tracks = listOf(Track(1L, "A", "/a.mp3", deviceKey = "11"))
        )
        val repository = FakePlaylistRepository(playlists = listOf(existing))
        val viewModel = PlaylistViewModel(repository, importUseCase(repository))

        viewModel.mergeWatchPlaylists(
            remotePlaylists = listOf(
                RemotePlaylist(7L, "running", listOf(11L, 12L)),
                RemotePlaylist(8L, "From watch", listOf(12L))
            ),
            remoteTracks = listOf(
                RemoteMusicTrack(11L, "/a.mp3", "A", null, null, null),
                RemoteMusicTrack(12L, "/b.mp3", "B", null, null, null)
            )
        )
        advanceUntilIdle()

        val playlists = viewModel.uiState.value.playlists
        assertEquals(2, playlists.size)
        assertEquals(7L, playlists.first { it.id == 1L }.remotePlaylistId)
        assertEquals(listOf("11", "12"), playlists.first { it.id == 1L }
            .tracks.map { it.deviceKey })
        assertEquals("From watch", playlists.first { it.name == "From watch" }.name)
        assertEquals(8L, playlists.first { it.name == "From watch" }.remotePlaylistId)
    }

    @Test
    fun `keeping watch playlist replaces local songs and links remote id`() = runTest {
        val existing = Playlist(
            id = 1L,
            name = "Dinglestick",
            tracks = listOf(Track(1L, "Local version", "/local.mp3"))
        )
        val repository = FakePlaylistRepository(playlists = listOf(existing))
        val viewModel = PlaylistViewModel(repository, importUseCase(repository))

        viewModel.keepWatchPlaylist(
            localPlaylistId = 1L,
            remote = RemotePlaylist(42L, "Dinglestick", listOf(101L, 102L)),
            remoteTracks = listOf(
                RemoteMusicTrack(101L, "/watch/one.mp3", "Watch one", null, null, 90_000L),
                RemoteMusicTrack(102L, "/watch/two.mp3", "Watch two", null, null, 120_000L)
            )
        )
        advanceUntilIdle()

        val saved = viewModel.uiState.value.playlists.single()
        assertEquals(42L, saved.remotePlaylistId)
        assertEquals(listOf("101", "102"), saved.tracks.map { it.deviceKey })
        assertEquals(listOf("Watch one", "Watch two"), saved.tracks.map { it.title })
        assertEquals(listOf(90, 120), saved.tracks.map { it.durationSeconds })
    }

    @Test
    fun `adds selected device tracks to local playlist and skips existing keys`() = runTest {
        val existing = Playlist(
            id = 1L,
            name = "Local",
            tracks = listOf(Track(4L, "Already here", "/a.mp3", deviceKey = "11"))
        )
        val repository = FakePlaylistRepository(playlists = listOf(existing))
        val viewModel = PlaylistViewModel(repository, importUseCase(repository))
        advanceUntilIdle()

        viewModel.addDeviceTracks(
            playlistId = 1L,
            deviceTracks = listOf(
                RemoteMusicTrack(11L, "/a.mp3", "Already here", null, null, null),
                RemoteMusicTrack(12L, "/device/b.mp3", "New song", null, null, 98_000L)
            )
        )
        advanceUntilIdle()

        val tracks = viewModel.uiState.value.playlists.single().tracks
        assertEquals(2, tracks.size)
        assertEquals("New song", tracks.last().title)
        assertEquals("/device/b.mp3", tracks.last().path)
        assertEquals("12", tracks.last().deviceKey)
        assertEquals(98, tracks.last().durationSeconds)
    }

    @Test
    fun `repository failure produces error state`() = runTest {
        val viewModel = PlaylistViewModel(
            repository = FakePlaylistRepository(
                failure = IllegalStateException("database unavailable")
            ),
            importPlaylistUseCase = importUseCase()
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.playlists.isEmpty())
        assertEquals(
            "database unavailable",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun `importing playlist saves it and refreshes state`() = runTest {
        val repository = FakePlaylistRepository()
        val viewModel = PlaylistViewModel(
            repository = repository,
            importPlaylistUseCase = importUseCase(repository)
        )

        advanceUntilIdle()

        viewModel.importPlaylist(
            content = """
                #EXTM3U
                #EXTINF:120,Imported song
                /music/imported.mp3
            """.trimIndent(),
            playlistName = "Imported"
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isImporting)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(1, repository.createCalls)
        assertEquals(1, viewModel.uiState.value.playlists.size)
        assertEquals(
            "Imported",
            viewModel.uiState.value.playlists[0].name
        )
    }

    @Test
    fun `import failure produces error state`() = runTest {
        val repository = FakePlaylistRepository(
            createFailure = IllegalStateException("save failed")
        )
        val viewModel = PlaylistViewModel(
            repository = repository,
            importPlaylistUseCase = importUseCase(repository)
        )

        advanceUntilIdle()

        viewModel.importPlaylist(
            content = "/music/song.mp3",
            playlistName = "Broken import"
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isImporting)
        assertEquals(
            "save failed",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun `second import is ignored while first import is running`() = runTest {
        val repository = FakePlaylistRepository(blockCreate = true)
        val viewModel = PlaylistViewModel(
            repository = repository,
            importPlaylistUseCase = importUseCase(repository)
        )

        advanceUntilIdle()

        viewModel.importPlaylist(
            content = "/music/first.mp3",
            playlistName = "First"
        )

        runCurrent()
        repository.creationStarted.await()

        viewModel.importPlaylist(
            content = "/music/second.mp3",
            playlistName = "Second"
        )

        repository.allowCreation.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.createCalls)
        assertEquals(1, viewModel.uiState.value.playlists.size)
        assertEquals(
            "First",
            viewModel.uiState.value.playlists[0].name
        )
    }

    @Test
    fun `reporting file error updates state`() {
        val repository = FakePlaylistRepository()
        val viewModel = PlaylistViewModel(
            repository = repository,
            importPlaylistUseCase = importUseCase(repository)
        )

        viewModel.reportImportError("Cannot read file")

        assertFalse(viewModel.uiState.value.isImporting)
        assertEquals(
            "Cannot read file",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun `playlist mutations refresh the state`() = runTest {
        val repository = FakePlaylistRepository(
            playlists = listOf(
                Playlist(
                    id = 1L,
                    name = "Original",
                    tracks = listOf(
                        Track(1L, "First", "/first.mp3"),
                        Track(2L, "Second", "/second.mp3")
                    )
                )
            )
        )
        val viewModel = PlaylistViewModel(repository, importUseCase(repository))

        advanceUntilIdle()
        viewModel.renamePlaylist(1L, "Renamed")
        advanceUntilIdle()
        assertEquals("Renamed", viewModel.uiState.value.playlists.single().name)

        viewModel.removeTrackAt(1L, 0)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.playlists.single().tracks.size)

        viewModel.deletePlaylist(1L)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.playlists.isEmpty())
    }

    private class FakePlaylistRepository(
        playlists: List<Playlist> = emptyList(),
        private val failure: Throwable? = null,
        private val createFailure: Throwable? = null,
        private val blockCreate: Boolean = false
    ) : PlaylistRepository {

        private val storedPlaylists = playlists.toMutableList()
        var createCalls: Int = 0
            private set

        val creationStarted = CompletableDeferred<Unit>()
        val allowCreation = CompletableDeferred<Unit>()

        override suspend fun createPlaylist(
            name: String,
            tracks: List<Track>,
            remotePlaylistId: Long?
        ): Playlist {
            createCalls++
            creationStarted.complete(Unit)

            if (blockCreate) {
                allowCreation.await()
            }

            createFailure?.let { throw it }

            val playlist = Playlist(
                id = (storedPlaylists.size + 1).toLong(),
                name = name,
                tracks = tracks,
                remotePlaylistId = remotePlaylistId
            )

            storedPlaylists += playlist
            return playlist
        }

        override suspend fun getAllPlaylists(): List<Playlist> {
            failure?.let { throw it }
            return storedPlaylists.toList()
        }

        override suspend fun appendTracks(playlistId: Long, tracks: List<Track>) {
            val index = storedPlaylists.indexOfFirst { it.id == playlistId }
            if (index >= 0) {
                storedPlaylists[index] = storedPlaylists[index].copy(
                    tracks = storedPlaylists[index].tracks + tracks
                )
            }
        }

        override suspend fun replacePlaylistTracks(
            playlistId: Long,
            tracks: List<Track>,
            remotePlaylistId: Long
        ) {
            val index = storedPlaylists.indexOfFirst { it.id == playlistId }
            if (index >= 0) {
                storedPlaylists[index] = storedPlaylists[index].copy(
                    tracks = tracks,
                    remotePlaylistId = remotePlaylistId
                )
            }
        }

        override suspend fun getPlaylist(
            playlistId: Long
        ): Playlist? {
            return storedPlaylists.firstOrNull { it.id == playlistId }
        }

        override suspend fun renamePlaylist(
            playlistId: Long,
            newName: String
        ) {
            val index = storedPlaylists.indexOfFirst { it.id == playlistId }
            if (index >= 0) {
                storedPlaylists[index] = storedPlaylists[index].copy(name = newName)
            }
        }

        override suspend fun setRemotePlaylistId(playlistId: Long, remotePlaylistId: Long) {
            val index = storedPlaylists.indexOfFirst { it.id == playlistId }
            if (index >= 0) {
                storedPlaylists[index] = storedPlaylists[index].copy(
                    remotePlaylistId = remotePlaylistId
                )
            }
        }

        override suspend fun deletePlaylist(playlistId: Long) {
            storedPlaylists.removeAll { it.id == playlistId }
        }

        override suspend fun removeTrackAt(
            playlistId: Long,
            position: Int
        ) {
            val index = storedPlaylists.indexOfFirst { it.id == playlistId }
            if (index >= 0) {
                storedPlaylists[index] = storedPlaylists[index]
                    .removeTrackAt(position)
            }
        }

        override suspend fun reorderTracks(
            playlistId: Long,
            orderedTrackIds: List<Long>
        ) = Unit
    }

    private fun importUseCase(
        repository: PlaylistRepository = FakePlaylistRepository()
    ): ImportPlaylistUseCase {
        return ImportPlaylistUseCase(
            parser = M3uParser(),
            repository = repository
        )
    }
}
