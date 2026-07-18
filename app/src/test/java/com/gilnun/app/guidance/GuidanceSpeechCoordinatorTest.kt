package com.gilnun.app.guidance

import com.gilnun.app.catalog.ServiceCatalog
import com.gilnun.app.catalog.ServiceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidanceSpeechCoordinatorTest {
    @Test
    fun `only fixed catalog narration is spoken at point eight five speed`() {
        val engine = FakeSpeechEngine()
        val coordinator = GuidanceSpeechCoordinator(engine)
        val service = ServiceCatalog.require(ServiceId.BASIC_PENSION)
        val checkpoint = service.steps.first()

        assertEquals(
            SpeechRequestResult.STARTED,
            coordinator.read(service.id, checkpoint.id),
        )
        assertEquals(listOf(checkpoint.narration), engine.spoken)
        assertEquals(0.85f, engine.lastRate)
        assertFalse(engine.spoken.single().contains("자유 입력"))
    }

    @Test
    fun `duplicate active request is suppressed and transition stops speech`() {
        val engine = FakeSpeechEngine()
        val coordinator = GuidanceSpeechCoordinator(engine)

        assertEquals(
            SpeechRequestResult.STARTED,
            coordinator.read(ServiceId.RESIDENT_RECORD, "resident-type"),
        )
        assertEquals(
            SpeechRequestResult.DUPLICATE_SUPPRESSED,
            coordinator.read(ServiceId.RESIDENT_RECORD, "resident-type"),
        )
        assertEquals(1, engine.spoken.size)

        engine.finishSpeaking()
        assertEquals(
            SpeechRequestResult.STARTED,
            coordinator.read(ServiceId.RESIDENT_RECORD, "resident-type"),
        )
        assertEquals(2, engine.spoken.size)

        coordinator.stop()
        assertEquals(1, engine.stopCalls)
        assertEquals(
            SpeechRequestResult.STARTED,
            coordinator.read(ServiceId.RESIDENT_RECORD, "resident-type"),
        )
    }

    @Test
    fun `unknown checkpoint and unavailable engine keep visual flow available`() {
        val engine = FakeSpeechEngine(available = false)
        val coordinator = GuidanceSpeechCoordinator(engine)

        assertEquals(
            SpeechRequestResult.UNAVAILABLE,
            coordinator.read(ServiceId.HEALTH_SCREENING, "health-person"),
        )
        assertEquals(
            SpeechRequestResult.UNAVAILABLE,
            coordinator.read(ServiceId.HEALTH_SCREENING, "unknown"),
        )
        assertTrue(engine.spoken.isEmpty())
    }

    @Test
    fun `only installed offline Korean voices are usable`() {
        assertTrue(KoreanVoicePolicy.isUsable(language = "ko", requiresNetwork = false))
        assertTrue(KoreanVoicePolicy.isUsable(language = "KO", requiresNetwork = false))
        assertFalse(KoreanVoicePolicy.isUsable(language = "ko", requiresNetwork = true))
        assertFalse(KoreanVoicePolicy.isUsable(language = "en", requiresNetwork = false))
    }

    private class FakeSpeechEngine(
        private val available: Boolean = true,
    ) : KoreanSpeechEngine {
        val spoken = mutableListOf<String>()
        var lastRate = 0f
        var stopCalls = 0
        private var speaking = false

        override fun speak(
            text: String,
            rate: Float,
        ): Boolean {
            if (!available) return false
            spoken += text
            lastRate = rate
            speaking = true
            return true
        }

        override fun isSpeaking(): Boolean = speaking

        override fun stop() {
            stopCalls += 1
            speaking = false
        }

        fun finishSpeaking() {
            speaking = false
        }

        override fun shutdown() = Unit
    }
}
