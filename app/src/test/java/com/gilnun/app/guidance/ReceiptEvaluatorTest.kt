package com.gilnun.app.guidance

import com.gilnun.app.catalog.EventEffect
import com.gilnun.app.catalog.ServiceCatalog
import com.gilnun.app.catalog.ServiceEventType
import com.gilnun.app.catalog.ServiceId
import com.gilnun.app.data.GuidanceSource
import com.gilnun.app.data.ReceiptOutcome
import com.gilnun.app.web.BridgeEventV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptEvaluatorTest {
    private val service = ServiceCatalog.require(ServiceId.BASIC_PENSION)
    private val step = service.steps.first()
    private val patch = requireNotNull(step.patch)
    private val action = requireNotNull(step.primaryAction)

    @Test
    fun `guided exact action stays pending until exact next checkpoint`() {
        val coordinator = GuidanceReceiptCoordinator()
        assertTrue(
            coordinator.begin(
                serviceId = service.id,
                checkpoint = step.id,
                patch = patch,
                source = GuidanceSource.PREVERIFIED,
            ),
        )

        val pending = coordinator.onAction(actionEvent()) as ReceiptTransition.Pending
        assertTrue(pending.receipt.guidanceShown)
        assertTrue(pending.receipt.userActionObserved)
        assertFalse(pending.receipt.postconditionVerified)
        assertEquals(ReceiptOutcome.UNVERIFIED, pending.receipt.outcome)

        val verified =
            coordinator.onCheckpointChanged(
                checkpointEvent(patch.expectedState),
            ) as ReceiptTransition.Verified
        assertEquals(ReceiptOutcome.VERIFIED, verified.receipt.outcome)
        assertEquals(GuidanceSource.PREVERIFIED, verified.receipt.source)
        assertTrue(verified.receipt.postconditionVerified)
    }

    @Test
    fun `checkpoint without user action fails closed`() {
        val coordinator = begunCoordinator()

        val rejected =
            coordinator.onCheckpointChanged(
                checkpointEvent(patch.expectedState),
            ) as ReceiptTransition.Rejected

        assertEquals(ReceiptOutcome.FAILED, rejected.receipt.outcome)
        assertFalse(rejected.receipt.userActionObserved)
        assertFalse(rejected.receipt.postconditionVerified)
    }

    @Test
    fun `wrong cross-service revision checkpoint and target fail closed`() {
        val mutations =
            listOf<(BridgeEventV2.ActionOrHelp) -> BridgeEventV2.ActionOrHelp>(
                { it.copy(serviceId = ServiceId.RESIDENT_RECORD) },
                { it.copy(revision = "2026-08") },
                { it.copy(checkpoint = service.steps[1].id) },
                { it.copy(stableKey = service.steps[1].primaryAction!!.stableKey) },
                { it.copy(accessibleName = "다른 버튼") },
                { it.copy(effect = EventEffect.NON_PROGRESS) },
            )

        mutations.forEach { mutate ->
            val result = begunCoordinator().onAction(mutate(actionEvent()))
            assertTrue(result is ReceiptTransition.Rejected)
            assertEquals(ReceiptOutcome.FAILED, (result as ReceiptTransition.Rejected).receipt.outcome)
        }
    }

    @Test
    fun `wrong postcondition and timeout fail closed`() {
        val wrongCheckpoint = begunCoordinator().also { it.onAction(actionEvent()) }
        assertTrue(
            wrongCheckpoint.onCheckpointChanged(checkpointEvent(step.id)) is
                ReceiptTransition.Rejected,
        )

        val timedOut = begunCoordinator().also { it.onAction(actionEvent()) }.onTimeout()
        assertTrue(timedOut is ReceiptTransition.Rejected)
    }

    @Test
    fun `begin rejects non-catalog duplicate or cross-service patches`() {
        val coordinator = GuidanceReceiptCoordinator()

        assertFalse(
            coordinator.begin(
                service.id,
                step.id,
                patch.copy(accessibleName = "비슷한 이름"),
                GuidanceSource.PREVERIFIED,
            ),
        )
        assertFalse(
            coordinator.begin(
                ServiceId.RESIDENT_RECORD,
                step.id,
                patch,
                GuidanceSource.SAME_DEVICE_HELPER,
            ),
        )
        assertNull(coordinator.currentReceipt())
    }

    private fun begunCoordinator() =
        GuidanceReceiptCoordinator().also {
            check(it.begin(service.id, step.id, patch, GuidanceSource.SAME_DEVICE_HELPER))
        }

    private fun actionEvent() =
        BridgeEventV2.ActionOrHelp(
            schemaVersion = 2,
            type = ServiceEventType.ACTION,
            serviceId = service.id,
            revision = service.revision,
            checkpoint = step.id,
            stableKey = action.stableKey,
            role = action.role,
            accessibleName = action.accessibleName,
            effect = action.effect,
        )

    private fun checkpointEvent(checkpoint: String) =
        BridgeEventV2.CheckpointChanged(
            schemaVersion = 2,
            serviceId = service.id,
            revision = service.revision,
            checkpoint = checkpoint,
        )
}
