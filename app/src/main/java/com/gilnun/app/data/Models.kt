package com.gilnun.app.data

import com.gilnun.app.catalog.ServiceId

/** Limits shared by persistence and semantic matching. */
object ModelLimits {
    const val MAX_SEMANTIC_FIELD_LENGTH = 256
}

/**
 * A single semantic repair point.
 *
 * The patch deliberately contains no selector, coordinate, URL, action payload, or captured
 * content. All six fields must match the current semantic target before guidance can be shown.
 */
data class PatchV1(
    val pageId: String,
    val compatibleRevision: String,
    val stableKey: String,
    val role: String,
    val accessibleName: String,
    val expectedState: String,
)

enum class ReceiptOutcome {
    VERIFIED,
    UNVERIFIED,
    FAILED,
}

enum class GuidanceSource {
    PREVERIFIED,
    SAME_DEVICE_HELPER,
}

/**
 * A truthful, minimal receipt. The three observations remain separate even when [outcome] is
 * VERIFIED so a click can never be presented as a verified result on its own.
 */
data class ActionReceipt(
    val guidanceShown: Boolean,
    val userActionObserved: Boolean,
    val postconditionVerified: Boolean,
    val outcome: ReceiptOutcome,
    val source: GuidanceSource? = null,
)

/**
 * Durable progress for one fixed service. Raw interaction events and screen state are not
 * represented.
 */
data class ServiceProgress(
    val helperPatchesByCheckpoint: Map<String, PatchV1> = emptyMap(),
    val helpLevel: Int = DEFAULT_HELP_LEVEL,
    val lastReceipt: ActionReceipt? = null,
) {
    companion object {
        const val DEFAULT_HELP_LEVEL = 3
    }
}

/**
 * The complete durable demo state. It always has one service-scoped progress entry for each
 * catalog service.
 *
 */
data class DemoState(
    val services: Map<ServiceId, ServiceProgress> = defaultServices(),
) {
    init {
        require(services.keys == REQUIRED_SERVICE_IDS) {
            "DemoState must contain exactly the three catalog services"
        }
    }

    companion object {
        private val REQUIRED_SERVICE_IDS = ServiceId.entries.toSet()

        private fun defaultServices(): Map<ServiceId, ServiceProgress> =
            ServiceId.entries.associateWith { ServiceProgress() }
    }
}

internal fun String.isValidSemanticField(): Boolean =
    isNotBlank() &&
        length <= ModelLimits.MAX_SEMANTIC_FIELD_LENGTH &&
        none { character -> character.isISOControl() || character.isSurrogate() }

internal fun PatchV1.hasValidSemanticFields(): Boolean =
    pageId.isValidSemanticField() &&
        compatibleRevision.isValidSemanticField() &&
        stableKey.isValidSemanticField() &&
        role.isValidSemanticField() &&
        accessibleName.isValidSemanticField() &&
        expectedState.isValidSemanticField()
