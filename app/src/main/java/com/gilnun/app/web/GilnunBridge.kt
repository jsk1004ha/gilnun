package com.gilnun.app.web

import android.net.Uri
import com.gilnun.app.data.InteractionEvent
import com.gilnun.app.data.InteractionEventTypes
import org.json.JSONObject

enum class BridgeError {
    INVALID_ORIGIN,
    INVALID_FRAME,
    INVALID_PAYLOAD,
    PAYLOAD_TOO_LARGE,
    INVALID_SCHEMA,
    INVALID_EVENT,
    INVALID_CONTRACT,
}

sealed interface BridgeResult {
    data class Accepted(val event: InteractionEvent) : BridgeResult

    data class Rejected(val error: BridgeError) : BridgeResult
}

/**
 * Parses only the privacy-minimal semantic event contract used by the owned welfare fixture.
 *
 * Origin and main-frame checks are repeated here even though the WebMessage listener also has an
 * exact origin rule. Payloads, URLs, coordinates, free-form DOM content, and form values are never
 * returned or logged.
 */
object GilnunBridge {
    const val APP_ORIGIN = "https://appassets.androidplatform.net"
    const val JS_OBJECT_NAME = "gilnun"
    const val PAGE_ID = "welfare-basic-class"
    const val COMPATIBLE_REVISION = "2026-07"
    const val SCHEMA_VERSION = 1
    const val MAX_PAYLOAD_BYTES = 4_096

    private val requiredFields = setOf(
        "schemaVersion",
        "type",
        "pageId",
        "compatibleRevision",
        "stableKey",
        "role",
        "accessibleName",
        "checkpoint",
        "monotonicMs",
    )

    private data class TargetContract(
        val type: String,
        val role: String,
        val accessibleName: String,
        val checkpoints: Set<String>,
    )

    private val contracts = mapOf(
        "save-draft" to TargetContract(
            type = InteractionEventTypes.TARGET_TAP,
            role = "button",
            accessibleName = "임시 저장",
            checkpoints = setOf("consent-ready"),
        ),
        "review-next" to TargetContract(
            type = InteractionEventTypes.TARGET_TAP,
            role = "button",
            accessibleName = "신청 내용 확인",
            checkpoints = setOf("review-ready"),
        ),
        "help-request" to TargetContract(
            type = InteractionEventTypes.HELP_REQUEST,
            role = "button",
            accessibleName = "도움 요청",
            checkpoints = setOf(
                "program-selection",
                "details-check",
                "consent-ready",
                "review-ready",
            ),
        ),
    )

    fun accept(
        payload: String?,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
    ): BridgeResult {
        if (!isExactOrigin(sourceOrigin)) {
            return BridgeResult.Rejected(BridgeError.INVALID_ORIGIN)
        }
        if (!isMainFrame) {
            return BridgeResult.Rejected(BridgeError.INVALID_FRAME)
        }
        if (payload == null) {
            return BridgeResult.Rejected(BridgeError.INVALID_PAYLOAD)
        }
        return parse(payload)
    }

    fun parse(payload: String): BridgeResult {
        if (payload.toByteArray(Charsets.UTF_8).size > MAX_PAYLOAD_BYTES) {
            return BridgeResult.Rejected(BridgeError.PAYLOAD_TOO_LARGE)
        }

        val json = try {
            JSONObject(payload)
        } catch (_: Exception) {
            return BridgeResult.Rejected(BridgeError.INVALID_PAYLOAD)
        }
        if (json.fieldNames() != requiredFields) {
            return BridgeResult.Rejected(BridgeError.INVALID_SCHEMA)
        }

        val schemaVersion = json.strictInt("schemaVersion")
            ?: return BridgeResult.Rejected(BridgeError.INVALID_SCHEMA)
        if (schemaVersion != SCHEMA_VERSION) {
            return BridgeResult.Rejected(BridgeError.INVALID_SCHEMA)
        }

        val type = json.strictString("type", 32)
            ?: return BridgeResult.Rejected(BridgeError.INVALID_EVENT)
        if (type != InteractionEventTypes.TARGET_TAP &&
            type != InteractionEventTypes.HELP_REQUEST
        ) {
            return BridgeResult.Rejected(BridgeError.INVALID_EVENT)
        }

        val pageId = json.strictString("pageId", 64)
            ?: return BridgeResult.Rejected(BridgeError.INVALID_CONTRACT)
        val revision = json.strictString("compatibleRevision", 32)
            ?: return BridgeResult.Rejected(BridgeError.INVALID_CONTRACT)
        val stableKey = json.strictString("stableKey", 64)
            ?: return BridgeResult.Rejected(BridgeError.INVALID_CONTRACT)
        val role = json.strictString("role", 32)
            ?: return BridgeResult.Rejected(BridgeError.INVALID_CONTRACT)
        val accessibleName = json.strictString("accessibleName", 80)
            ?: return BridgeResult.Rejected(BridgeError.INVALID_CONTRACT)
        val checkpoint = json.strictString("checkpoint", 64)
            ?: return BridgeResult.Rejected(BridgeError.INVALID_CONTRACT)
        val monotonicMs = json.strictLong("monotonicMs")
            ?: return BridgeResult.Rejected(BridgeError.INVALID_EVENT)

        if (pageId != PAGE_ID || revision != COMPATIBLE_REVISION || monotonicMs < 0L) {
            return BridgeResult.Rejected(BridgeError.INVALID_CONTRACT)
        }

        val contract = contracts[stableKey]
            ?: return BridgeResult.Rejected(BridgeError.INVALID_CONTRACT)
        if (contract.type != type ||
            contract.role != role ||
            contract.accessibleName != accessibleName ||
            checkpoint !in contract.checkpoints
        ) {
            return BridgeResult.Rejected(BridgeError.INVALID_CONTRACT)
        }

        return BridgeResult.Accepted(
            InteractionEvent(
                schemaVersion = schemaVersion,
                type = type,
                pageId = pageId,
                compatibleRevision = revision,
                stableKey = stableKey,
                role = role,
                accessibleName = accessibleName,
                checkpoint = checkpoint,
                monotonicMs = monotonicMs,
            ),
        )
    }

    fun isExactOrigin(origin: Uri): Boolean =
        origin.scheme == "https" &&
            origin.host == "appassets.androidplatform.net" &&
            origin.port == -1 &&
            origin.path.orEmpty().let { it.isEmpty() || it == "/" } &&
            origin.query == null &&
            origin.fragment == null

    private fun JSONObject.fieldNames(): Set<String> {
        val names = mutableSetOf<String>()
        val iterator = keys()
        while (iterator.hasNext()) {
            names += iterator.next()
        }
        return names
    }

    private fun JSONObject.strictString(name: String, maximumLength: Int): String? {
        val value = try {
            get(name)
        } catch (_: Exception) {
            return null
        }
        return (value as? String)?.takeIf { it.isNotEmpty() && it.length <= maximumLength }
    }

    private fun JSONObject.strictInt(name: String): Int? =
        when (val value = try {
            get(name)
        } catch (_: Exception) {
            return null
        }) {
            is Int -> value
            is Long -> value.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
            else -> null
        }

    private fun JSONObject.strictLong(name: String): Long? =
        when (val value = try {
            get(name)
        } catch (_: Exception) {
            return null
        }) {
            is Int -> value.toLong()
            is Long -> value
            else -> null
        }
}
