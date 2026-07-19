package com.gilnun.app.ui

import com.gilnun.app.web.BridgeError
import com.gilnun.app.web.BridgeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GilnunPresentationPolicyTest {
    @Test
    fun `disabled system animation skips startup delay`() {
        assertEquals(0L, GilnunPresentationPolicy.startupDelayMs(animationsEnabled = false))
        assertTrue(GilnunPresentationPolicy.startupDelayMs(animationsEnabled = true) > 0L)
    }

    @Test
    fun `enabled startup presentation remains brief and finite`() {
        val delayMs = GilnunPresentationPolicy.startupDelayMs(animationsEnabled = true)

        assertTrue(delayMs in 1L..1_000L)
    }

    @Test
    fun `bridge rejection keeps loading protection visible`() {
        val rejected = BridgeStatus.Rejected(BridgeError.INVALID_SCHEMA)

        assertFalse(GilnunPresentationPolicy.dismissLoading(rejected))
    }

    @Test
    fun `loading ends only for page ready or terminal page failure`() {
        assertFalse(GilnunPresentationPolicy.dismissLoading(BridgeStatus.Available))
        assertFalse(GilnunPresentationPolicy.dismissLoading(BridgeStatus.Unavailable))
        assertTrue(GilnunPresentationPolicy.dismissLoading(BridgeStatus.PageReady))
        assertTrue(GilnunPresentationPolicy.dismissLoading(BridgeStatus.PageFailed()))
    }
}
