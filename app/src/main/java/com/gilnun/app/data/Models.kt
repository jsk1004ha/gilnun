package com.gilnun.app.data

import com.gilnun.app.catalog.ServiceCatalog
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
 * The legacy accessors and constructor are temporary compilation adapters for the pre-catalog
 * ViewModel. They project only the basic-pension slot and are not part of the V2 wire format.
 */
data class DemoState(
    val services: Map<ServiceId, ServiceProgress> = defaultServices(),
) {
    init {
        require(services.keys == REQUIRED_SERVICE_IDS) {
            "DemoState must contain exactly the three catalog services"
        }
    }

    constructor(
        patch: PatchV1?,
        helpLevel: Int = ServiceProgress.DEFAULT_HELP_LEVEL,
        lastReceipt: ActionReceipt? = null,
    ) : this(
        services =
            defaultServices() +
                (
                    ServiceId.BASIC_PENSION to
                        ServiceProgress(
                            helperPatchesByCheckpoint =
                                legacyBasicPatches(patch),
                            helpLevel = helpLevel,
                            lastReceipt = lastReceipt,
                        )
                ),
    )

    val patch: PatchV1?
        get() =
            services
                .getValue(ServiceId.BASIC_PENSION)
                .helperPatchesByCheckpoint
                .values
                .firstOrNull()

    val helpLevel: Int
        get() = services.getValue(ServiceId.BASIC_PENSION).helpLevel

    val lastReceipt: ActionReceipt?
        get() = services.getValue(ServiceId.BASIC_PENSION).lastReceipt

    companion object {
        private val REQUIRED_SERVICE_IDS = ServiceId.entries.toSet()

        private fun defaultServices(): Map<ServiceId, ServiceProgress> =
            ServiceId.entries.associateWith { ServiceProgress() }

        private fun legacyBasicPatches(patch: PatchV1?): Map<String, PatchV1> {
            val matchingCheckpoint =
                ServiceCatalog
                    .require(ServiceId.BASIC_PENSION)
                    .steps
                    .singleOrNull { checkpoint -> checkpoint.patch == patch }
                    ?: return emptyMap()
            return mapOf(matchingCheckpoint.id to checkNotNull(patch))
        }
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
