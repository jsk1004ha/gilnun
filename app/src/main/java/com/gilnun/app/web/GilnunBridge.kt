package com.gilnun.app.web

import android.net.Uri

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
    data class Accepted(
        val event: BridgeEventV2,
    ) : BridgeResult

    data class Rejected(
        val error: BridgeError,
    ) : BridgeResult
}

/** Android origin/main-frame boundary around the dependency-free strict V2 parser. */
object GilnunBridge {
    const val APP_ORIGIN = PracticeUrlPolicy.APP_ORIGIN
    const val JS_OBJECT_NAME = "gilnun"
    const val SCHEMA_VERSION = BridgeEventV2Parser.SCHEMA_VERSION

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
        return parse(payload)
    }

    fun parse(payload: String?): BridgeResult =
        when (val result = BridgeEventV2Parser.parse(payload)) {
            is BridgeEventV2Result.Accepted -> BridgeResult.Accepted(result.event)
            is BridgeEventV2Result.Rejected ->
                BridgeResult.Rejected(
                    when (result.error) {
                        BridgeEventV2Error.INVALID_PAYLOAD -> BridgeError.INVALID_PAYLOAD
                        BridgeEventV2Error.PAYLOAD_TOO_LARGE -> BridgeError.PAYLOAD_TOO_LARGE
                        BridgeEventV2Error.INVALID_SCHEMA -> BridgeError.INVALID_SCHEMA
                        BridgeEventV2Error.INVALID_EVENT -> BridgeError.INVALID_EVENT
                        BridgeEventV2Error.INVALID_CONTRACT -> BridgeError.INVALID_CONTRACT
                    },
                )
        }

    fun isExactOrigin(origin: Uri): Boolean =
        origin.scheme == "https" &&
            origin.host == "appassets.androidplatform.net" &&
            origin.port == -1 &&
            origin.userInfo == null &&
            origin.path.orEmpty().let { it.isEmpty() || it == "/" } &&
            origin.query == null &&
            origin.fragment == null
}
