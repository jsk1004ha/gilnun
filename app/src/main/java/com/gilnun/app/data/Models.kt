package com.gilnun.app.data

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

/**
 * Privacy-minimal event received from the owned demo page.
 *
 * Form values, URLs, coordinates, and free-form text are intentionally absent.
 */
data class InteractionEvent(
    val schemaVersion: Int,
    val type: String,
    val pageId: String,
    val compatibleRevision: String,
    val stableKey: String,
    val role: String,
    val accessibleName: String,
    val checkpoint: String,
    val monotonicMs: Long,
)

object InteractionEventTypes {
    const val TARGET_TAP = "TARGET_TAP"
    const val HELP_REQUEST = "HELP_REQUEST"
}

enum class ReceiptOutcome {
    VERIFIED,
    UNVERIFIED,
    FAILED,
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
)

/**
 * The complete durable demo state. Raw interaction events are intentionally not represented.
 */
data class DemoState(
    val patch: PatchV1? = null,
    val helpLevel: Int = 0,
    val lastReceipt: ActionReceipt? = null,
)

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
