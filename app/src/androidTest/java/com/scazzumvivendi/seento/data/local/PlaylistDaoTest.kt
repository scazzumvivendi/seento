package com.scazzumvivendi.seento.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.scazzumvivendi.seento.data.local.entity.PlaylistEntity
import com.scazzumvivendi.seento.data.local.entity.PlaylistTrackEntity
import com.scazzumvivendi.seento.data.local.entity.TrackEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistDaoTest {

    private lateinit var database: SeentoDatabase
    private lateinit var dao: PlaylistDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        database = Room.inMemoryDatabaseBuilder(
            context,
            SeentoDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = database.playlistDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun tracksAreReadInPlaylistPositionOrder() = runBlocking {
        val playlistId = dao.insertPlaylist(
            PlaylistEntity(name = "Workout")
        )

        val firstTrackId = dao.insertTrack(
            TrackEntity(
                title = "First",
                path = "/music/first.mp3",
                durationSeconds = 100,
                deviceKey = null
            )
        )

        val secondTrackId = dao.insertTrack(
            TrackEntity(
                title = "Second",
                path = "/music/second.mp3",
                durationSeconds = 200,
                deviceKey = null
            )
        )

        dao.insertPlaylistTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = secondTrackId,
                position = 1
            )
        )

        dao.insertPlaylistTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = firstTrackId,
                position = 0
            )
        )

        val tracks = dao.getTracksForPlaylist(playlistId)

        assertEquals(2, tracks.size)
        assertEquals(firstTrackId, tracks[0].id)
        assertEquals(secondTrackId, tracks[1].id)
    }

    @Test
    fun sameTrackCanAppearTwiceInPlaylist() = runBlocking {
        val playlistId = dao.insertPlaylist(
            PlaylistEntity(name = "Duplicates")
        )

        val trackId = dao.insertTrack(
            TrackEntity(
                title = "Repeated",
                path = "/music/repeated.mp3",
                durationSeconds = 150,
                deviceKey = null
            )
        )

        dao.insertPlaylistTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = trackId,
                position = 0
            )
        )

        dao.insertPlaylistTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = trackId,
                position = 1
            )
        )

        val tracks = dao.getTracksForPlaylist(playlistId)

        assertEquals(2, tracks.size)
        assertEquals(trackId, tracks[0].id)
        assertEquals(trackId, tracks[1].id)
    }

    @Test
    fun missingPlaylistHasNoTracks() = runBlocking {
        val tracks = dao.getTracksForPlaylist(999L)

        assertTrue(tracks.isEmpty())
        assertEquals(null, dao.getPlaylistById(999L))
    }

    @Test
    fun playlistCanBeRenamedAndRelationsDeleted() = runBlocking {
        val playlistId = dao.insertPlaylist(
            PlaylistEntity(name = "Original")
        )

        dao.renamePlaylist(playlistId, "Renamed")
        dao.deletePlaylistTracks(playlistId)

        assertEquals("Renamed", dao.getPlaylistById(playlistId)?.name)
        assertTrue(dao.getPlaylistTrackRelations(playlistId).isEmpty())
    }
}
