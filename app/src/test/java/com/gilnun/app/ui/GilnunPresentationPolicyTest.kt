package com.gilnun.app.ui

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
    fun `loading ends only for page ready or terminal page failure`() {
        assertFalse(GilnunPresentationPolicy.dismissLoading(BridgeStatus.Available))
        assertFalse(GilnunPresentationPolicy.dismissLoading(BridgeStatus.Unavailable))
        assertTrue(GilnunPresentationPolicy.dismissLoading(BridgeStatus.PageReady))
        assertTrue(GilnunPresentationPolicy.dismissLoading(BridgeStatus.PageFailed()))
    }
}
