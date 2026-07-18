package com.gilnun.app.guidance

/** Prevents TTS from silently selecting a Korean voice that needs a network connection. */
object KoreanVoicePolicy {
    fun isUsable(
        language: String,
        requiresNetwork: Boolean,
    ): Boolean = language.equals("ko", ignoreCase = true) && !requiresNetwork
}
