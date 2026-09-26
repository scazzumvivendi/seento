package com.scazzumvivendi.seento.data.ble

import androidx.test.platform.app.InstrumentationRegistry
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in, read-only test against a real, paired watch. Never modifies playlists. */
class WatchCatalogHardwareTest {
    @Test
    fun readsCatalogAgainAfterDisconnect() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val address = arguments.getString("watchAddress")
        val serial = arguments.getString("watchSerial")
        assumeTrue("Pass watchAddress and watchSerial to run hardware tests", address != null && serial != null)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        if (Build.VERSION.SDK_INT >= 31) {
            assertEquals("Grant Bluetooth permission in the app before running hardware tests",
                PackageManager.PERMISSION_GRANTED,
                instrumentation.targetContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT))
        }
        val client = MdsMusicClient(InstrumentationRegistry.getInstrumentation().targetContext)
        try {
            var previousKeys: List<Long>? = null
            repeat(2) {
                withTimeout(120_000) {
                    assertEquals(serial, client.connect(address!!, serial!!))
                    val catalog = client.readCatalog()
                    assertEquals("Expected all songs from the connected watch", 29, catalog.tracks.size)
                    assertFalse("Expected song metadata, not fallback entries",
                        catalog.tracks.any { it.path.startsWith("device-key:") })
                    assertEquals("All playlist reads must succeed", null, catalog.playlistReadWarning)
                    assertEquals("Expected the two user playlists on the test watch", 2, catalog.playlists.size)
                    val keys = catalog.tracks.map { track -> track.key }
                    previousKeys?.let { previous -> assertEquals(previous, keys) }
                    previousKeys = keys
                    client.disconnect()
                }
            }
        } finally {
            client.close()
        }
    }
}
