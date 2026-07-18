package com.gilnun.app.guidance

import android.content.Context
import android.speech.tts.TextToSpeech

/** Thin Android implementation; catalog whitelisting remains in [GuidanceSpeechCoordinator]. */
class AndroidKoreanSpeechEngine(
    context: Context,
) : KoreanSpeechEngine {
    @Volatile
    private var ready = false
    private var engine: TextToSpeech? = null

    init {
        engine =
            TextToSpeech(context.applicationContext) { status ->
                val tts = engine
                val installedKoreanVoice =
                    tts
                        ?.voices
                        ?.asSequence()
                        ?.filter { voice ->
                            KoreanVoicePolicy.isUsable(
                                language = voice.locale.language,
                                requiresNetwork = voice.isNetworkConnectionRequired,
                            )
                        }
                        ?.sortedBy { voice -> voice.name }
                        ?.firstOrNull()
                ready =
                    status == TextToSpeech.SUCCESS &&
                        tts != null &&
                        installedKoreanVoice != null &&
                        tts.setVoice(installedKoreanVoice) == TextToSpeech.SUCCESS
            }
    }

    override fun speak(
        text: String,
        rate: Float,
    ): Boolean {
        val tts = engine ?: return false
        if (!ready) return false
        if (tts.setSpeechRate(rate) != TextToSpeech.SUCCESS) return false
        return tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "gilnun-fixed-guidance",
        ) == TextToSpeech.SUCCESS
    }

    override fun isSpeaking(): Boolean = engine?.isSpeaking == true

    override fun stop() {
        engine?.stop()
    }

    override fun shutdown() {
        ready = false
        engine?.stop()
        engine?.shutdown()
        engine = null
    }
}
