package com.scazzumvivendi.seento.data.ble

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransientMdsReadFailureTest {
    @Test
    fun retries408And502Responses() {
        assertTrue(TransientMdsReadFailure.matches(IllegalStateException("HTTP status 408")))
        assertTrue(TransientMdsReadFailure.matches(IllegalStateException("Gateway returned 502")))
    }

    @Test
    fun retriesTimeoutsWrappedAsCauses() {
        assertTrue(
            TransientMdsReadFailure.matches(
                IllegalStateException("MDS request failed", java.net.SocketTimeoutException("timed out"))
            )
        )
    }

    @Test
    fun doesNotRetryMissingRoutesOrOtherStatuses() {
        assertFalse(TransientMdsReadFailure.matches(IllegalStateException("HTTP status 404")))
        assertFalse(TransientMdsReadFailure.matches(IllegalStateException("HTTP status 403")))
    }
}
