package com.gilnun.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoStateCodecTest {
    private val patch =
        PatchV1(
            pageId = "welfare-basic-class",
            compatibleRevision = "2026-07",
            stableKey = "review-next",
            role = "button",
            accessibleName = "신청 내용 확인",
            expectedState = "review-ready",
        )
    private val receipt =
        ActionReceipt(
            guidanceShown = true,
            userActionObserved = true,
            postconditionVerified = true,
            outcome = ReceiptOutcome.VERIFIED,
        )

    @Test
    fun `minimal state round trips exactly`() {
        val state = DemoState(patch = patch, helpLevel = 2, lastReceipt = receipt)

        assertEquals(state, DemoStateCodec.decode(DemoStateCodec.encode(state)))
    }

    @Test
    fun `encoded state contains no raw events or prohibited targeting data`() {
        val encoded = DemoStateCodec.encode(DemoState(patch, 2, receipt))

        assertTrue(encoded.contains("\"schemaVersion\":1"))
        assertTrue(encoded.contains("\"accessibleName\":\"신청 내용 확인\""))
        assertFalse(encoded.contains("monotonicMs"))
        assertFalse(encoded.contains("checkpoint"))
        assertFalse(encoded.contains("selector"))
        assertFalse(encoded.contains("coordinate"))
        assertFalse(encoded.contains("url", ignoreCase = true))
    }

    @Test
    fun `null blank truncated and oversized json recover to default`() {
        val default = DemoState()

        assertEquals(default, DemoStateCodec.decode(null))
        assertEquals(default, DemoStateCodec.decode(" "))
        assertEquals(default, DemoStateCodec.decode("""{"schemaVersion":1,"patch":{"""))
        assertEquals(default, DemoStateCodec.decode("x".repeat(DemoStateCodec.MAX_ENCODED_LENGTH + 1)))
    }

    @Test
    fun `unknown schema and fields recover to default`() {
        assertEquals(
            DemoState(),
            DemoStateCodec.decode(
                """{"schemaVersion":999,"patch":null,"helpLevel":0,"lastReceipt":null}""",
            ),
        )
        assertEquals(
            DemoState(),
            DemoStateCodec.decode(
                """{"schemaVersion":1,"patch":null,"helpLevel":0,"lastReceipt":null,"rawEvents":[]}""",
            ),
        )
    }

    @Test
    fun `missing oversized and duplicate semantic fields recover to default`() {
        val valid = DemoStateCodec.encode(DemoState(patch = patch))
        val missingStableKey = valid.replace(""""stableKey":"review-next",""", "")
        val oversized = valid.replace("review-next", "x".repeat(ModelLimits.MAX_SEMANTIC_FIELD_LENGTH + 1))
        val duplicate =
            valid.replace(
                """"stableKey":"review-next"""",
                """"stableKey":"review-next","stableKey":"review-next"""",
            )

        assertEquals(DemoState(), DemoStateCodec.decode(missingStableKey))
        assertEquals(DemoState(), DemoStateCodec.decode(oversized))
        assertEquals(DemoState(), DemoStateCodec.decode(duplicate))
    }

    @Test
    fun `out of range help and contradictory receipts recover to default`() {
        assertEquals(
            DemoState(),
            DemoStateCodec.decode(
                """{"schemaVersion":1,"patch":null,"helpLevel":4,"lastReceipt":null}""",
            ),
        )
        assertEquals(
            DemoState(),
            DemoStateCodec.decode(
                """{"schemaVersion":1,"patch":null,"helpLevel":1,"lastReceipt":{"guidanceShown":true,"userActionObserved":true,"postconditionVerified":false,"outcome":"VERIFIED"}}""",
            ),
        )
    }

    @Test
    fun `escaped strings decode and invalid in-memory state encodes as default`() {
        val encoded =
            DemoStateCodec.encode(
                DemoState(
                    patch = patch.copy(accessibleName = "신청 \"내용\" 확인"),
                    helpLevel = 1,
                ),
            )
        assertEquals("신청 \"내용\" 확인", DemoStateCodec.decode(encoded).patch?.accessibleName)

        val safeDefault = DemoStateCodec.decode(DemoStateCodec.encode(DemoState(helpLevel = 99)))
        assertEquals(DemoState(), safeDefault)
    }
}
