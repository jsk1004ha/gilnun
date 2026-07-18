package com.gilnun.app.guidance

import com.gilnun.app.data.InteractionEvent
import com.gilnun.app.data.InteractionEventTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class StruggleDetectorTest {
    @Test
    fun `three same non-progress taps inside six seconds emit exactly once`() {
        val detector = StruggleDetector()

        assertNull(detector.observe(tap(atMs = 0)))
        assertNull(detector.observe(tap(atMs = 2_500)))
        val candidate = detector.observe(tap(atMs = 5_000))
        assertEquals(StruggleCandidateSource.REPEATED_NON_PROGRESS_TAP, candidate?.source)
        assertEquals("save-draft", candidate?.stableKey)
        assertNull(detector.observe(tap(atMs = 5_500)))
    }

    @Test
    fun `three taps spanning more than six seconds do not emit`() {
        val detector = StruggleDetector()

        assertNull(detector.observe(tap(atMs = 0)))
        assertNull(detector.observe(tap(atMs = 3_000)))
        assertNull(detector.observe(tap(atMs = 7_000)))
    }

    @Test
    fun `checkpoint and stable key changes start new episodes`() {
        val detector = StruggleDetector()

        assertNull(detector.observe(tap(atMs = 0)))
        assertNull(detector.observe(tap(atMs = 1_000)))
        assertNull(detector.observe(tap(atMs = 2_000, checkpoint = "review-ready")))
        assertNull(detector.observe(tap(atMs = 3_000, stableKey = "other-target")))
        assertNull(detector.observe(tap(atMs = 4_000, stableKey = "other-target")))
        assertNotNull(detector.observe(tap(atMs = 5_000, stableKey = "other-target")))
    }

    @Test
    fun `progress and checkpoint callbacks clear accumulated taps`() {
        val detector = StruggleDetector()

        detector.observe(tap(atMs = 0))
        detector.observe(tap(atMs = 1_000))
        detector.onProgress()
        assertNull(detector.observe(tap(atMs = 2_000)))
        assertNull(detector.observe(tap(atMs = 3_000)))
        detector.onCheckpointChanged()
        assertNull(detector.observe(tap(atMs = 4_000)))
    }

    @Test
    fun `loading and focused input suppress and reset automatic detection`() {
        val detector = StruggleDetector()

        detector.observe(tap(atMs = 0))
        detector.observe(tap(atMs = 1_000))
        detector.setLoading(true)
        assertNull(detector.observe(tap(atMs = 2_000)))
        detector.setLoading(false)
        assertNull(detector.observe(tap(atMs = 3_000)))
        assertNull(detector.observe(tap(atMs = 4_000)))

        detector.setInputFocused(true)
        assertNull(detector.observe(tap(atMs = 5_000)))
        detector.setInputFocused(false)
        assertNull(detector.observe(tap(atMs = 6_000)))
        assertNull(detector.observe(tap(atMs = 7_000)))
        assertNotNull(detector.observe(tap(atMs = 8_000)))
    }

    @Test
    fun `rejection suppresses automatic prompt for thirty seconds`() {
        val detector = StruggleDetector()
        detector.rejectCandidate(atMonotonicMs = 0)

        assertNull(detector.observe(tap(atMs = 1_000)))
        assertNull(detector.observe(tap(atMs = 2_000)))
        assertNull(detector.observe(tap(atMs = 3_000)))
        assertNull(detector.observe(tap(atMs = 30_000)))
        assertNull(detector.observe(tap(atMs = 32_500)))
        assertNotNull(detector.observe(tap(atMs = 35_000)))
    }

    @Test
    fun `direct help is immediate and bypasses loading focus and cooldown`() {
        val detector = StruggleDetector()
        detector.setLoading(true)
        detector.setInputFocused(true)
        detector.rejectCandidate(atMonotonicMs = 0)

        val candidate =
            detector.observe(
                tap(atMs = 1_000).copy(type = InteractionEventTypes.HELP_REQUEST),
            )

        assertEquals(StruggleCandidateSource.DIRECT_HELP, candidate?.source)
    }

    @Test
    fun `unknown malformed and backwards tap events never emit`() {
        val detector = StruggleDetector()

        assertNull(detector.observe(tap(atMs = 0).copy(type = "SCROLL")))
        assertNull(detector.observe(tap(atMs = -1)))
        assertNull(detector.observe(tap(atMs = 1).copy(stableKey = "")))
        assertNull(detector.observe(tap(atMs = 3_000)))
        assertNull(detector.observe(tap(atMs = 2_000)))
    }

    private fun tap(
        atMs: Long,
        stableKey: String = "save-draft",
        checkpoint: String = "consent-ready",
    ) = InteractionEvent(
        schemaVersion = 1,
        type = InteractionEventTypes.TARGET_TAP,
        pageId = "welfare-basic-class",
        compatibleRevision = "2026-07",
        stableKey = stableKey,
        role = "button",
        accessibleName = "임시 저장",
        checkpoint = checkpoint,
        monotonicMs = atMs,
    )
}
