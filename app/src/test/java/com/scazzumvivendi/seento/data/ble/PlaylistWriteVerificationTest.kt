package com.scazzumvivendi.seento.data.ble

import com.scazzumvivendi.seento.data.ble.model.RemotePlaylist
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistWriteVerificationTest {
    private val expected = RemotePlaylist(42, "Running", listOf(7, 8, 7))

    @Test
    fun waitsForDeviceToApplyChanges() = runTest {
        var reads = 0
        assertTrue(PlaylistWriteVerification.confirm(42, "Running", expected.songKeys) {
            reads++
            listOf(if (reads < 3) expected.copy(songKeys = listOf(8)) else expected)
        })
        assertEquals(3, reads)
    }

    @Test
    fun rejectsMissingPlaylistWrongIdNameOrderAndDuplicates() = runTest {
        for (playlists in listOf(
            emptyList(),
            listOf(expected.copy(id = 43)),
            listOf(expected.copy(name = "Old name")),
            listOf(expected.copy(songKeys = listOf(7, 7, 8))),
            listOf(expected.copy(songKeys = listOf(7, 8)))
        )) {
            assertFalse(PlaylistWriteVerification.confirm(42, "Running", expected.songKeys) {
                playlists
            })
        }
    }
}
