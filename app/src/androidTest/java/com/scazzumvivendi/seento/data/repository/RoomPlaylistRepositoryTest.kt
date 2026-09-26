package com.scazzumvivendi.seento.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.scazzumvivendi.seento.data.local.SeentoDatabase
import com.scazzumvivendi.seento.domain.model.Track
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomPlaylistRepositoryTest {

    private lateinit var database: SeentoDatabase
    private lateinit var repository: RoomPlaylistRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        database = Room.inMemoryDatabaseBuilder(
            context,
            SeentoDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = RoomPlaylistRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createAndReadPlaylistPreservesDataAndOrder() = runBlocking {
        val created = repository.createPlaylist(
            name = "Workout",
            tracks = listOf(
                Track(
                    id = 0L,
                    title = "First",
                    path = "/music/first.mp3",
                    durationSeconds = 100,
                    deviceKey = "device-1"
                ),
                Track(
                    id = 0L,
                    title = "Second",
                    path = "/music/second.mp3",
                    durationSeconds = 200,
                    deviceKey = null
                )
            )
        )

        val loaded = repository.getPlaylist(created.id)

        assertNotNull(loaded)
        assertEquals(created.id, loaded?.id)
        assertEquals("Workout", loaded?.name)
        assertEquals(2, loaded?.tracks?.size)
        assertEquals("First", loaded?.tracks?.get(0)?.title)
        assertEquals("Second", loaded?.tracks?.get(1)?.title)
        assertEquals("device-1", loaded?.tracks?.get(0)?.deviceKey)
    }

    @Test
    fun getAllPlaylistsReturnsAllSavedPlaylists() = runBlocking {
        repository.createPlaylist("One", emptyList())
        repository.createPlaylist("Two", emptyList())

        val playlists = repository.getAllPlaylists()

        assertEquals(2, playlists.size)
        assertEquals("One", playlists[0].name)
        assertEquals("Two", playlists[1].name)
    }

    @Test
    fun missingPlaylistReturnsNull() = runBlocking {
        val playlist = repository.getPlaylist(999L)

        assertEquals(null, playlist)
    }

    @Test
    fun renameAndRemoveTrackUpdatePlaylist() = runBlocking {
        val created = repository.createPlaylist(
            name = "Original",
            tracks = listOf(
                Track(0L, "First", "/first.mp3"),
                Track(0L, "Second", "/second.mp3")
            )
        )

        repository.renamePlaylist(created.id, "Renamed")
        repository.removeTrackAt(created.id, 0)

        val updated = repository.getPlaylist(created.id)
        assertEquals("Renamed", updated?.name)
        assertEquals(1, updated?.tracks?.size)
        assertEquals("Second", updated?.tracks?.single()?.title)
    }

    @Test
    fun deletePlaylistRemovesPlaylistAndRelations() = runBlocking {
        val created = repository.createPlaylist(
            name = "Temporary",
            tracks = listOf(Track(0L, "Song", "/song.mp3"))
        )

        repository.deletePlaylist(created.id)

        assertEquals(null, repository.getPlaylist(created.id))
        assertEquals(0, repository.getAllPlaylists().size)
    }
}
