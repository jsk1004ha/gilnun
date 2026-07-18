package com.gilnun.app.guidance

import com.gilnun.app.data.ActionReceipt
import com.gilnun.app.data.PatchV1
import com.gilnun.app.data.ReceiptOutcome
import com.gilnun.app.data.hasValidSemanticFields

/** Minimal post-action observation; it never contains form values or free-form page content. */
data class ReceiptObservation(
    val guidanceShown: Boolean,
    val userActionObserved: Boolean,
    val pageId: String,
    val compatibleRevision: String,
    val checkpoint: String?,
    val timedOut: Boolean = false,
)

/**
 * Produces a truthful receipt by keeping guidance, user action, and postcondition observations
 * independent. A click is never promoted to success without the exact supported postcondition.
 */
class ReceiptEvaluator {
    fun evaluate(
        patch: PatchV1?,
        observation: ReceiptObservation,
    ): ActionReceipt {
        val validPatch = patch?.takeIf { it.hasValidSemanticFields() }
        val contextMatches =
            validPatch != null &&
                observation.pageId == validPatch.pageId &&
                observation.compatibleRevision == validPatch.compatibleRevision
        val supportedPostcondition =
            validPatch?.expectedState == VERIFIED_CHECKPOINT
        val postconditionVerified =
            !observation.timedOut &&
                contextMatches &&
                supportedPostcondition &&
                observation.checkpoint == validPatch.expectedState

        val outcome =
            when {
                observation.guidanceShown &&
                    observation.userActionObserved &&
                    postconditionVerified -> ReceiptOutcome.VERIFIED
                observation.timedOut ||
                    validPatch == null ||
                    !contextMatches ||
                    !supportedPostcondition -> ReceiptOutcome.FAILED
                else -> ReceiptOutcome.UNVERIFIED
            }

        return ActionReceipt(
            guidanceShown = observation.guidanceShown,
            userActionObserved = observation.userActionObserved,
            postconditionVerified = postconditionVerified,
            outcome = outcome,
        )
    }

    fun evaluate(
        patch: PatchV1?,
        guidanceShown: Boolean,
        userActionObserved: Boolean,
        observedPageId: String,
        observedRevision: String,
        observedCheckpoint: String?,
        timedOut: Boolean = false,
    ): ActionReceipt =
        evaluate(
            patch = patch,
            observation =
                ReceiptObservation(
                    guidanceShown = guidanceShown,
                    userActionObserved = userActionObserved,
                    pageId = observedPageId,
                    compatibleRevision = observedRevision,
                    checkpoint = observedCheckpoint,
                    timedOut = timedOut,
                ),
        )

    companion object {
        const val VERIFIED_CHECKPOINT = "review-ready"
    }
}
