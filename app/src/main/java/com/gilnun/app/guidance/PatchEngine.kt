package com.gilnun.app.guidance

import com.gilnun.app.data.PatchV1
import com.gilnun.app.data.hasValidSemanticFields
import com.gilnun.app.data.isValidSemanticField

/**
 * A semantic target observed immediately before guidance is applied.
 *
 * It intentionally mirrors the six PatchV1 fields. It contains no selector, coordinate, URL,
 * executable payload, or captured page content.
 */
data class SemanticTarget(
    val pageId: String,
    val compatibleRevision: String,
    val stableKey: String,
    val role: String,
    val accessibleName: String,
    val expectedState: String,
)

enum class PatchResolution {
    RESOLVED,
    PATCH_UNAVAILABLE,
}

/**
 * Resolves a patch only when its stable key identifies one target and all six semantic fields
 * match exactly. There is deliberately no fuzzy, positional, or first-match fallback.
 */
class PatchEngine {
    fun resolve(
        patch: PatchV1?,
        targets: Iterable<SemanticTarget>,
    ): PatchResolution =
        if (resolveTarget(patch, targets) == null) {
            PatchResolution.PATCH_UNAVAILABLE
        } else {
            PatchResolution.RESOLVED
        }

    fun resolveTarget(
        patch: PatchV1?,
        targets: Iterable<SemanticTarget>,
    ): SemanticTarget? {
        if (patch == null || !patch.hasValidSemanticFields()) return null

        var keyedTarget: SemanticTarget? = null
        for (target in targets) {
            if (target.stableKey != patch.stableKey) continue

            // A duplicate stable key is ambiguous even when only one duplicate matches the
            // remaining fields. Fail before considering any candidate.
            if (keyedTarget != null) return null
            keyedTarget = target
        }

        return keyedTarget?.takeIf { target ->
            target.hasValidSemanticFields() &&
                target.pageId == patch.pageId &&
                target.compatibleRevision == patch.compatibleRevision &&
                target.stableKey == patch.stableKey &&
                target.role == patch.role &&
                target.accessibleName == patch.accessibleName &&
                target.expectedState == patch.expectedState
        }
    }

    private fun SemanticTarget.hasValidSemanticFields(): Boolean =
        pageId.isValidSemanticField() &&
            compatibleRevision.isValidSemanticField() &&
            stableKey.isValidSemanticField() &&
            role.isValidSemanticField() &&
            accessibleName.isValidSemanticField() &&
            expectedState.isValidSemanticField()

    companion object {
        /** Safe four-hour fallback: guidance for one synthetic, non-submitting action only. */
        val PRELOADED_REVIEW_PATCH =
            PatchV1(
                pageId = "welfare-basic-class",
                compatibleRevision = "2026-07",
                stableKey = "review-next",
                role = "button",
                accessibleName = "신청 내용 확인",
                expectedState = "review-ready",
            )
    }
}
