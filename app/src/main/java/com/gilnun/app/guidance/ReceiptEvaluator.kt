package com.gilnun.app.guidance

import com.gilnun.app.catalog.EventEffect
import com.gilnun.app.catalog.ServiceCatalog
import com.gilnun.app.catalog.ServiceEventType
import com.gilnun.app.catalog.ServiceId
import com.gilnun.app.data.ActionReceipt
import com.gilnun.app.data.GuidanceSource
import com.gilnun.app.data.PatchV1
import com.gilnun.app.data.ReceiptOutcome
import com.gilnun.app.web.BridgeEventV2

sealed interface ReceiptTransition {
    data object NoGuidance : ReceiptTransition

    data class Pending(
        val receipt: ActionReceipt,
    ) : ReceiptTransition

    data class Verified(
        val receipt: ActionReceipt,
    ) : ReceiptTransition

    data class Rejected(
        val receipt: ActionReceipt,
    ) : ReceiptTransition
}

/**
 * Observes the two independent facts required for a truthful receipt: the learner's exact
 * catalog action, then the exact catalog next checkpoint. A WebView click is never promoted to a
 * verified result by itself.
 */
class GuidanceReceiptCoordinator {
    private data class Session(
        val serviceId: ServiceId,
        val checkpoint: String,
        val patch: PatchV1,
        val source: GuidanceSource,
        var userActionObserved: Boolean = false,
    )

    private var session: Session? = null

    fun begin(
        serviceId: ServiceId,
        checkpoint: String,
        patch: PatchV1,
        source: GuidanceSource,
    ): Boolean {
        val expectedPatch = ServiceCatalog.builtInPatch(serviceId, checkpoint)
        if (expectedPatch == null || patch != expectedPatch) {
            session = null
            return false
        }
        session = Session(serviceId, checkpoint, patch, source)
        return true
    }

    fun onAction(event: BridgeEventV2.ActionOrHelp): ReceiptTransition {
        val active = session ?: return ReceiptTransition.NoGuidance
        val service = ServiceCatalog.require(active.serviceId)
        val primary =
            service
                .checkpoint(active.checkpoint)
                ?.primaryAction
                ?: return reject(active)
        val exactAction =
            event.schemaVersion == 2 &&
                event.type == ServiceEventType.ACTION &&
                event.serviceId == active.serviceId &&
                event.revision == service.revision &&
                event.checkpoint == active.checkpoint &&
                event.stableKey == active.patch.stableKey &&
                event.stableKey == primary.stableKey &&
                event.role == active.patch.role &&
                event.role == primary.role &&
                event.accessibleName == active.patch.accessibleName &&
                event.accessibleName == primary.accessibleName &&
                event.effect == EventEffect.PROGRESS &&
                event.effect == primary.effect
        if (!exactAction) return reject(active)

        active.userActionObserved = true
        return ReceiptTransition.Pending(active.receipt(ReceiptOutcome.UNVERIFIED))
    }

    fun onCheckpointChanged(event: BridgeEventV2.CheckpointChanged): ReceiptTransition {
        val active = session ?: return ReceiptTransition.NoGuidance
        val service = ServiceCatalog.require(active.serviceId)
        val exactPostcondition =
            active.userActionObserved &&
                event.schemaVersion == 2 &&
                event.serviceId == active.serviceId &&
                event.revision == service.revision &&
                event.checkpoint == active.patch.expectedState
        if (!exactPostcondition) return reject(active)

        val receipt =
            ActionReceipt(
                guidanceShown = true,
                userActionObserved = true,
                postconditionVerified = true,
                outcome = ReceiptOutcome.VERIFIED,
                source = active.source,
            )
        session = null
        return ReceiptTransition.Verified(receipt)
    }

    fun onTimeout(): ReceiptTransition {
        val active = session ?: return ReceiptTransition.NoGuidance
        return reject(active)
    }

    fun currentReceipt(): ActionReceipt? =
        session?.receipt(ReceiptOutcome.UNVERIFIED)

    fun clear() {
        session = null
    }

    private fun reject(active: Session): ReceiptTransition.Rejected {
        val receipt = active.receipt(ReceiptOutcome.FAILED)
        session = null
        return ReceiptTransition.Rejected(receipt)
    }

    private fun Session.receipt(outcome: ReceiptOutcome): ActionReceipt =
        ActionReceipt(
            guidanceShown = true,
            userActionObserved = userActionObserved,
            postconditionVerified = false,
            outcome = outcome,
            source = source,
        )
}
