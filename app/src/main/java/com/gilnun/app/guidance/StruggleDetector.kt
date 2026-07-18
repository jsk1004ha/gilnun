package com.gilnun.app.guidance

import com.gilnun.app.data.InteractionEvent
import com.gilnun.app.data.InteractionEventTypes
import java.util.ArrayDeque

enum class StruggleCandidateSource {
    REPEATED_NON_PROGRESS_TAP,
    DIRECT_HELP,
}

/** A consent-prompt candidate, never a diagnosis or an instruction to perform an action. */
data class StruggleCandidate(
    val pageId: String,
    val compatibleRevision: String,
    val stableKey: String,
    val checkpoint: String,
    val source: StruggleCandidateSource,
    val monotonicMs: Long,
)

/**
 * Detects only explicit help and three same-target taps without checkpoint progress.
 *
 * Loading and input focus are explicit state. Dwell, scrolling, age, and content are never used.
 */
class StruggleDetector(
    private val monotonicClockMs: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private data class EpisodeKey(
        val pageId: String,
        val compatibleRevision: String,
        val stableKey: String,
        val checkpoint: String,
    )

    private val tapTimesMs = ArrayDeque<Long>()
    private var episodeKey: EpisodeKey? = null
    private var lastTapMs: Long? = null
    private var candidateEmittedForEpisode = false
    private var loading = false
    private var inputFocused = false
    private var cooldownUntilMs: Long? = null

    fun observe(event: InteractionEvent): StruggleCandidate? =
        when (event.type) {
            InteractionEventTypes.HELP_REQUEST -> directHelp(event)
            InteractionEventTypes.TARGET_TAP -> observeNonProgressTap(event)
            else -> null
        }

    fun observeNonProgressTap(event: InteractionEvent): StruggleCandidate? {
        if (loading || inputFocused || !event.hasUsableTapIdentity()) {
            clearEpisode()
            return null
        }
        if (isInRejectionCooldown(event.monotonicMs)) {
            clearEpisode()
            return null
        }
        if (lastTapMs?.let { event.monotonicMs < it } == true) {
            clearEpisode()
        }

        val incomingKey = EpisodeKey(
            event.pageId,
            event.compatibleRevision,
            event.stableKey,
            event.checkpoint,
        )
        if (episodeKey != incomingKey) {
            clearEpisode()
            episodeKey = incomingKey
        }

        lastTapMs = event.monotonicMs
        while (tapTimesMs.isNotEmpty() &&
            event.monotonicMs - tapTimesMs.first > TAP_WINDOW_MS
        ) {
            tapTimesMs.removeFirst()
        }
        tapTimesMs.addLast(event.monotonicMs)

        if (tapTimesMs.size < REQUIRED_TAPS || candidateEmittedForEpisode) {
            return null
        }
        candidateEmittedForEpisode = true
        return event.toCandidate(StruggleCandidateSource.REPEATED_NON_PROGRESS_TAP)
    }

    /** Explicit help is immediate and bypasses automatic-prompt cooldown. */
    fun directHelp(event: InteractionEvent): StruggleCandidate {
        clearEpisode()
        return event.toCandidate(StruggleCandidateSource.DIRECT_HELP)
    }

    fun setLoading(isLoading: Boolean) {
        if (loading != isLoading || isLoading) clearEpisode()
        loading = isLoading
    }

    fun setInputFocused(isFocused: Boolean) {
        if (inputFocused != isFocused || isFocused) clearEpisode()
        inputFocused = isFocused
    }

    fun onProgress() = clearEpisode()

    fun onCheckpointChanged() = clearEpisode()

    fun rejectCandidate(atMonotonicMs: Long = monotonicClockMs()) {
        clearEpisode()
        cooldownUntilMs =
            if (atMonotonicMs > Long.MAX_VALUE - REJECTION_COOLDOWN_MS) {
                Long.MAX_VALUE
            } else {
                atMonotonicMs + REJECTION_COOLDOWN_MS
            }
    }

    fun reset() {
        clearEpisode()
        loading = false
        inputFocused = false
        cooldownUntilMs = null
    }

    private fun isInRejectionCooldown(atMonotonicMs: Long): Boolean {
        val until = cooldownUntilMs ?: return false
        if (atMonotonicMs < until) return true
        cooldownUntilMs = null
        return false
    }

    private fun clearEpisode() {
        tapTimesMs.clear()
        episodeKey = null
        lastTapMs = null
        candidateEmittedForEpisode = false
    }

    private fun InteractionEvent.hasUsableTapIdentity(): Boolean =
        pageId.isNotBlank() &&
            compatibleRevision.isNotBlank() &&
            stableKey.isNotBlank() &&
            checkpoint.isNotBlank() &&
            monotonicMs >= 0

    private fun InteractionEvent.toCandidate(source: StruggleCandidateSource) =
        StruggleCandidate(
            pageId,
            compatibleRevision,
            stableKey,
            checkpoint,
            source,
            monotonicMs,
        )

    companion object {
        const val REQUIRED_TAPS = 3
        const val TAP_WINDOW_MS = 6_000L
        const val REJECTION_COOLDOWN_MS = 30_000L
    }
}
