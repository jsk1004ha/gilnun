package com.gilnun.app.guidance

import com.gilnun.app.data.ModelLimits
import com.gilnun.app.data.PatchV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PatchEngineTest {
    private val engine = PatchEngine()
    private val patch = PatchEngine.PRELOADED_REVIEW_PATCH
    private val exactTarget = patch.toTarget()

    @Test
    fun `one exact six-field target resolves`() {
        val unrelated = patch.copy(stableKey = "save-draft").toTarget()

        assertEquals(
            PatchResolution.RESOLVED,
            engine.resolve(patch, listOf(unrelated, exactTarget)),
        )
        assertSame(exactTarget, engine.resolveTarget(patch, listOf(unrelated, exactTarget)))
    }

    @Test
    fun `missing and duplicate stable keys fail closed`() {
        assertEquals(PatchResolution.PATCH_UNAVAILABLE, engine.resolve(patch, emptyList()))
        assertEquals(
            PatchResolution.PATCH_UNAVAILABLE,
            engine.resolve(
                patch,
                listOf(
                    exactTarget,
                    exactTarget.copy(accessibleName = "다른 이름"),
                ),
            ),
        )
        assertNull(engine.resolveTarget(patch, listOf(exactTarget, exactTarget.copy())))
    }

    @Test
    fun `each non-key semantic mismatch fails closed`() {
        val mismatches =
            listOf(
                exactTarget.copy(pageId = "other-page"),
                exactTarget.copy(compatibleRevision = "2026-06"),
                exactTarget.copy(role = "link"),
                exactTarget.copy(accessibleName = "확인"),
                exactTarget.copy(expectedState = "submitted"),
            )

        mismatches.forEach { mismatch ->
            assertEquals(
                "Unexpected resolution for $mismatch",
                PatchResolution.PATCH_UNAVAILABLE,
                engine.resolve(patch, listOf(mismatch)),
            )
        }
    }

    @Test
    fun `blank oversized control and case-normalized fields never match`() {
        val invalidPatches =
            listOf(
                patch.copy(pageId = ""),
                patch.copy(compatibleRevision = " "),
                patch.copy(stableKey = ""),
                patch.copy(role = "but\u0000ton"),
                patch.copy(accessibleName = "x".repeat(ModelLimits.MAX_SEMANTIC_FIELD_LENGTH + 1)),
                patch.copy(expectedState = ""),
            )
        invalidPatches.forEach { invalid ->
            assertEquals(
                PatchResolution.PATCH_UNAVAILABLE,
                engine.resolve(invalid, listOf(exactTarget)),
            )
        }

        assertEquals(
            PatchResolution.PATCH_UNAVAILABLE,
            engine.resolve(patch.copy(role = "BUTTON"), listOf(exactTarget)),
        )
        assertEquals(
            PatchResolution.PATCH_UNAVAILABLE,
            engine.resolve(patch.copy(accessibleName = " 신청 내용 확인"), listOf(exactTarget)),
        )
    }

    @Test
    fun `null patch and malformed observed target are unavailable`() {
        assertEquals(PatchResolution.PATCH_UNAVAILABLE, engine.resolve(null, listOf(exactTarget)))
        assertEquals(
            PatchResolution.PATCH_UNAVAILABLE,
            engine.resolve(patch, listOf(exactTarget.copy(role = ""))),
        )
    }

    private fun PatchV1.toTarget() =
        SemanticTarget(
            pageId = pageId,
            compatibleRevision = compatibleRevision,
            stableKey = stableKey,
            role = role,
            accessibleName = accessibleName,
            expectedState = expectedState,
        )
}
