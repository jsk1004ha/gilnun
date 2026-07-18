package com.gilnun.app.guidance

import com.gilnun.app.catalog.ServiceId
import java.util.ArrayDeque

enum class StruggleCandidateSource {
    REPEATED_NON_PROGRESS_TAP,
    DIRECT_HELP,
}

data class NonProgressObservation(
    val serviceId: ServiceId,
    val revision: String,
    val stableKey: String,
    val checkpoint: String,
    val monotonicMs: Long,
)

/** A consent-prompt candidate, never a diagnosis or an instruction to perform an action. */
data class StruggleCandidate(
    val serviceId: ServiceId,
    val revision: String,
    val stableKey: String,
    val checkpoint: String,
    val source: StruggleCandidateSource,
    val monotonicMs: Long,
)

/** Detects three same catalog non-progress actions in six seconds. */
class StruggleDetector(
    private val monotonicClockMs: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private data class EpisodeKey(
        val serviceId: ServiceId,
        val revision: String,
        val stableKey: String,
        val checkpoint: String,
    )

    private val tapTimesMs = ArrayDeque<Long>()
    private var episodeKey: EpisodeKey? = null
    private var lastTapMs: Long? = null
    private var candidateEmittedForEpisode = false
    private var cooldownUntilMs: Long? = null

    fun observeNonProgress(observation: NonProgressObservation): StruggleCandidate? {
        if (!observation.hasUsableIdentity()) {
            clearEpisode()
            return null
        }
        if (isInRejectionCooldown(observation.monotonicMs)) {
            clearEpisode()
            return null
        }
        if (lastTapMs?.let { observation.monotonicMs < it } == true) clearEpisode()

        val incomingKey =
            EpisodeKey(
                observation.serviceId,
                observation.revision,
                observation.stableKey,
                observation.checkpoint,
            )
        if (episodeKey != incomingKey) {
            clearEpisode()
            episodeKey = incomingKey
        }

        lastTapMs = observation.monotonicMs
        while (
            tapTimesMs.isNotEmpty() &&
            observation.monotonicMs - tapTimesMs.first > TAP_WINDOW_MS
        ) {
            tapTimesMs.removeFirst()
        }
        tapTimesMs.addLast(observation.monotonicMs)
        if (tapTimesMs.size < REQUIRED_TAPS || candidateEmittedForEpisode) return null

        candidateEmittedForEpisode = true
        return StruggleCandidate(
            serviceId = observation.serviceId,
            revision = observation.revision,
            stableKey = observation.stableKey,
            checkpoint = observation.checkpoint,
            source = StruggleCandidateSource.REPEATED_NON_PROGRESS_TAP,
            monotonicMs = observation.monotonicMs,
        )
    }

    /** Explicit native help bypasses the automatic-prompt cooldown. */
    fun directHelp(
        serviceId: ServiceId,
        revision: String,
        checkpoint: String,
        atMonotonicMs: Long = monotonicClockMs(),
    ): StruggleCandidate {
        clearEpisode()
        return StruggleCandidate(
            serviceId = serviceId,
            revision = revision,
            stableKey = DIRECT_HELP_KEY,
            checkpoint = checkpoint,
            source = StruggleCandidateSource.DIRECT_HELP,
            monotonicMs = atMonotonicMs,
        )
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

    private fun NonProgressObservation.hasUsableIdentity(): Boolean =
        revision.isNotBlank() &&
            stableKey.isNotBlank() &&
            checkpoint.isNotBlank() &&
            monotonicMs >= 0

    companion object {
        const val REQUIRED_TAPS = 3
        const val TAP_WINDOW_MS = 6_000L
        const val REJECTION_COOLDOWN_MS = 30_000L
        private const val DIRECT_HELP_KEY = "native-help-request"
    }
}
