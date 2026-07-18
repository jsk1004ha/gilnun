package com.gilnun.app.guidance

import com.gilnun.app.catalog.ServiceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StruggleDetectorTest {
    @Test
    fun `three same non-progress taps inside six seconds emit exactly once`() {
        val detector = StruggleDetector()

        assertNull(detector.observeNonProgress(tap(atMs = 0)))
        assertNull(detector.observeNonProgress(tap(atMs = 2_500)))
        val candidate = detector.observeNonProgress(tap(atMs = 5_000))
        assertEquals(StruggleCandidateSource.REPEATED_NON_PROGRESS_TAP, candidate?.source)
        assertEquals("pension-save-draft", candidate?.stableKey)
        assertNull(detector.observeNonProgress(tap(atMs = 5_500)))
    }

    @Test
    fun `three taps spanning more than six seconds do not emit`() {
        val detector = StruggleDetector()

        assertNull(detector.observeNonProgress(tap(atMs = 0)))
        assertNull(detector.observeNonProgress(tap(atMs = 3_000)))
        assertNull(detector.observeNonProgress(tap(atMs = 7_000)))
    }

    @Test
    fun `service checkpoint and stable key changes start new episodes`() {
        val detector = StruggleDetector()

        assertNull(detector.observeNonProgress(tap(atMs = 0)))
        assertNull(detector.observeNonProgress(tap(atMs = 1_000)))
        assertNull(detector.observeNonProgress(tap(atMs = 2_000, checkpoint = "pension-method")))
        assertNull(detector.observeNonProgress(tap(atMs = 3_000, stableKey = "other-target")))
        assertNull(detector.observeNonProgress(tap(atMs = 4_000, stableKey = "other-target")))
        assertNotNull(detector.observeNonProgress(tap(atMs = 5_000, stableKey = "other-target")))

        detector.reset()
        assertNull(detector.observeNonProgress(tap(atMs = 6_000)))
        assertNull(
            detector.observeNonProgress(
                tap(atMs = 7_000).copy(serviceId = ServiceId.RESIDENT_RECORD),
            ),
        )
    }

    @Test
    fun `progress and checkpoint callbacks clear accumulated taps`() {
        val detector = StruggleDetector()

        detector.observeNonProgress(tap(atMs = 0))
        detector.observeNonProgress(tap(atMs = 1_000))
        detector.onProgress()
        assertNull(detector.observeNonProgress(tap(atMs = 2_000)))
        assertNull(detector.observeNonProgress(tap(atMs = 3_000)))
        detector.onCheckpointChanged()
        assertNull(detector.observeNonProgress(tap(atMs = 4_000)))
    }

    @Test
    fun `rejection suppresses automatic prompt for thirty seconds`() {
        val detector = StruggleDetector()
        detector.rejectCandidate(atMonotonicMs = 0)

        assertNull(detector.observeNonProgress(tap(atMs = 1_000)))
        assertNull(detector.observeNonProgress(tap(atMs = 2_000)))
        assertNull(detector.observeNonProgress(tap(atMs = 3_000)))
        assertNull(detector.observeNonProgress(tap(atMs = 30_000)))
        assertNull(detector.observeNonProgress(tap(atMs = 32_500)))
        assertNotNull(detector.observeNonProgress(tap(atMs = 35_000)))
    }

    @Test
    fun `native direct help is immediate during automatic cooldown`() {
        val detector = StruggleDetector()
        detector.rejectCandidate(atMonotonicMs = 0)

        val candidate =
            detector.directHelp(
                serviceId = ServiceId.BASIC_PENSION,
                revision = "2026-07",
                checkpoint = "pension-review",
                atMonotonicMs = 1_000,
            )

        assertEquals(StruggleCandidateSource.DIRECT_HELP, candidate.source)
        assertEquals(ServiceId.BASIC_PENSION, candidate.serviceId)
        assertEquals("pension-review", candidate.checkpoint)
    }

    @Test
    fun `malformed and backwards observations never emit`() {
        val detector = StruggleDetector()

        assertNull(detector.observeNonProgress(tap(atMs = -1)))
        assertNull(detector.observeNonProgress(tap(atMs = 1).copy(stableKey = "")))
        assertNull(detector.observeNonProgress(tap(atMs = 3_000)))
        assertNull(detector.observeNonProgress(tap(atMs = 2_000)))
    }

    private fun tap(
        atMs: Long,
        stableKey: String = "pension-save-draft",
        checkpoint: String = "pension-review",
    ) = NonProgressObservation(
        serviceId = ServiceId.BASIC_PENSION,
        revision = "2026-07",
        stableKey = stableKey,
        checkpoint = checkpoint,
        monotonicMs = atMs,
    )
}
