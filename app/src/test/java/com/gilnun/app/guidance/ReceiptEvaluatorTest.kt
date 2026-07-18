package com.gilnun.app.guidance

import com.gilnun.app.data.PatchV1
import com.gilnun.app.data.ReceiptOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptEvaluatorTest {
    private val evaluator = ReceiptEvaluator()
    private val patch = PatchEngine.PRELOADED_REVIEW_PATCH

    @Test
    fun `exact guided user action followed by review ready is verified`() {
        val receipt = evaluator.evaluate(patch, observation(checkpoint = "review-ready"))

        assertTrue(receipt.guidanceShown)
        assertTrue(receipt.userActionObserved)
        assertTrue(receipt.postconditionVerified)
        assertEquals(ReceiptOutcome.VERIFIED, receipt.outcome)
    }

    @Test
    fun `click alone is unverified and never reported complete`() {
        val receipt = evaluator.evaluate(patch, observation(checkpoint = "consent-ready"))

        assertTrue(receipt.guidanceShown)
        assertTrue(receipt.userActionObserved)
        assertFalse(receipt.postconditionVerified)
        assertEquals(ReceiptOutcome.UNVERIFIED, receipt.outcome)
    }

    @Test
    fun `postcondition without observed user action remains unverified`() {
        val receipt =
            evaluator.evaluate(
                patch,
                observation(checkpoint = "review-ready").copy(userActionObserved = false),
            )

        assertTrue(receipt.postconditionVerified)
        assertEquals(ReceiptOutcome.UNVERIFIED, receipt.outcome)
    }

    @Test
    fun `postcondition without shown guidance remains unverified`() {
        val receipt =
            evaluator.evaluate(
                patch,
                observation(checkpoint = "review-ready").copy(guidanceShown = false),
            )

        assertTrue(receipt.postconditionVerified)
        assertEquals(ReceiptOutcome.UNVERIFIED, receipt.outcome)
    }

    @Test
    fun `timeout and context changes fail without verified postcondition`() {
        val failures =
            listOf(
                observation(checkpoint = null).copy(timedOut = true),
                observation(checkpoint = "review-ready").copy(compatibleRevision = "2026-08"),
                observation(checkpoint = "review-ready").copy(pageId = "other-page"),
            )

        failures.forEach { observation ->
            val receipt = evaluator.evaluate(patch, observation)
            assertFalse(receipt.postconditionVerified)
            assertEquals(ReceiptOutcome.FAILED, receipt.outcome)
        }
    }

    @Test
    fun `unsupported expected state and invalid patch fail closed`() {
        val unsupported = patch.copy(expectedState = "submitted")
        val invalid = patch.copy(stableKey = "")

        assertEquals(
            ReceiptOutcome.FAILED,
            evaluator.evaluate(unsupported, observation(checkpoint = "submitted")).outcome,
        )
        assertEquals(
            ReceiptOutcome.FAILED,
            evaluator.evaluate(invalid, observation(checkpoint = "review-ready")).outcome,
        )
        assertEquals(
            ReceiptOutcome.FAILED,
            evaluator.evaluate(null, observation(checkpoint = "review-ready")).outcome,
        )
    }

    @Test
    fun `argument overload preserves the same truthfulness rules`() {
        val receipt =
            evaluator.evaluate(
                patch = patch,
                guidanceShown = true,
                userActionObserved = true,
                observedPageId = patch.pageId,
                observedRevision = patch.compatibleRevision,
                observedCheckpoint = patch.expectedState,
            )

        assertEquals(ReceiptOutcome.VERIFIED, receipt.outcome)
    }

    private fun observation(checkpoint: String?) =
        ReceiptObservation(
            guidanceShown = true,
            userActionObserved = true,
            pageId = patch.pageId,
            compatibleRevision = patch.compatibleRevision,
            checkpoint = checkpoint,
        )
}
