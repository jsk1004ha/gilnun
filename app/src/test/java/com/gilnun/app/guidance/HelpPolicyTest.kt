package com.gilnun.app.guidance

import com.gilnun.app.data.ActionReceipt
import com.gilnun.app.data.ReceiptOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpPolicyTest {
    private val policy = HelpPolicy()

    @Test
    fun `truthful verified success lowers at most one level`() {
        val verified =
            ActionReceipt(
                guidanceShown = true,
                userActionObserved = true,
                postconditionVerified = true,
                outcome = ReceiptOutcome.VERIFIED,
            )

        assertEquals(2, policy.onReceipt(3, verified))
        assertEquals(1, policy.onReceipt(2, verified))
        assertEquals(0, policy.onReceipt(1, verified))
        assertEquals(0, policy.onReceipt(0, verified))
    }

    @Test
    fun `request struggle unverified and failed immediately restore one level`() {
        assertEquals(2, policy.onHelpRequested(1))
        assertEquals(2, policy.onStruggleCandidate(1))
        assertEquals(
            2,
            policy.onReceipt(
                1,
                receipt(ReceiptOutcome.UNVERIFIED),
            ),
        )
        assertEquals(
            2,
            policy.onReceipt(
                1,
                receipt(ReceiptOutcome.FAILED),
            ),
        )
        assertEquals(3, policy.onHelpRequested(3))
    }

    @Test
    fun `contradictory verified label never reduces help`() {
        val untruthful =
            ActionReceipt(
                guidanceShown = true,
                userActionObserved = true,
                postconditionVerified = false,
                outcome = ReceiptOutcome.VERIFIED,
            )

        assertEquals(3, policy.onReceipt(2, untruthful))
    }

    @Test
    fun `all event sequences stay within zero to three and move by one`() {
        for (start in 0..3) {
            HelpPolicyEvent.entries.forEach { event ->
                val next = policy.transition(start, event)
                assertTrue(next in 0..3)
                assertTrue(kotlin.math.abs(next - start) <= 1)
            }
        }

        assertEquals(0, policy.transition(Int.MIN_VALUE, HelpPolicyEvent.VERIFIED_SUCCESS))
        assertEquals(3, policy.transition(Int.MAX_VALUE, HelpPolicyEvent.HELP_REQUESTED))
        assertEquals(
            HelpLevel.LEVEL_2,
            policy.transition(HelpLevel.LEVEL_3, HelpPolicyEvent.VERIFIED_SUCCESS),
        )
    }

    private fun receipt(outcome: ReceiptOutcome) =
        ActionReceipt(
            guidanceShown = true,
            userActionObserved = true,
            postconditionVerified = false,
            outcome = outcome,
        )
}
