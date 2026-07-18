package com.gilnun.app.guidance

import com.gilnun.app.catalog.ServiceCatalog
import com.gilnun.app.catalog.ServiceId

interface KoreanSpeechEngine {
    fun speak(
        text: String,
        rate: Float,
    ): Boolean

    fun isSpeaking(): Boolean

    fun stop()

    fun shutdown()
}

enum class SpeechRequestResult {
    STARTED,
    DUPLICATE_SUPPRESSED,
    UNAVAILABLE,
}

/**
 * Restricts speech to static catalog copy. No DOM text, bridge strings, form values, or caller
 * supplied narration can reach the speech engine.
 */
class GuidanceSpeechCoordinator(
    private val engine: KoreanSpeechEngine,
) {
    private data class ActiveNarration(
        val serviceId: ServiceId,
        val checkpoint: String,
    )

    private var active: ActiveNarration? = null

    fun read(
        serviceId: ServiceId,
        checkpoint: String,
    ): SpeechRequestResult {
        val key = ActiveNarration(serviceId, checkpoint)
        if (active == key && engine.isSpeaking()) {
            return SpeechRequestResult.DUPLICATE_SUPPRESSED
        }
        val narration =
            ServiceCatalog
                .find(serviceId)
                ?.checkpoint(checkpoint)
                ?.narration
                ?: return SpeechRequestResult.UNAVAILABLE

        if (engine.isSpeaking()) engine.stop()
        return if (engine.speak(narration, SPEECH_RATE)) {
            active = key
            SpeechRequestResult.STARTED
        } else {
            active = null
            SpeechRequestResult.UNAVAILABLE
        }
    }

    fun stop() {
        active = null
        engine.stop()
    }

    fun shutdown() {
        active = null
        engine.stop()
        engine.shutdown()
    }

    companion object {
        const val SPEECH_RATE = 0.85f
    }
}
