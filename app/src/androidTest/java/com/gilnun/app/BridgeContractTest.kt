package com.gilnun.app

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gilnun.app.web.BridgeError
import com.gilnun.app.web.BridgeResult
import com.gilnun.app.web.GilnunBridge
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BridgeContractTest {
    @Test
    fun exactOwnedOriginAndMinimalSchemaAreAccepted() {
        val result =
            GilnunBridge.accept(
                payload = validPayload(),
                sourceOrigin = Uri.parse(GilnunBridge.APP_ORIGIN),
                isMainFrame = true,
            )

        assertTrue(result is BridgeResult.Accepted)
    }

    @Test
    fun foreignOriginSubframeAndExtraDataFailClosed() {
        assertEquals(
            BridgeResult.Rejected(BridgeError.INVALID_ORIGIN),
            GilnunBridge.accept(
                payload = validPayload(),
                sourceOrigin = Uri.parse("https://example.invalid"),
                isMainFrame = true,
            ),
        )
        assertEquals(
            BridgeResult.Rejected(BridgeError.INVALID_FRAME),
            GilnunBridge.accept(
                payload = validPayload(),
                sourceOrigin = Uri.parse(GilnunBridge.APP_ORIGIN),
                isMainFrame = false,
            ),
        )
        assertEquals(
            BridgeResult.Rejected(BridgeError.INVALID_SCHEMA),
            GilnunBridge.parse(
                JSONObject(validPayload())
                    .put("capturedValue", "must never cross the bridge")
                    .toString(),
            ),
        )
    }

    private fun validPayload(): String =
        JSONObject()
            .put("schemaVersion", GilnunBridge.SCHEMA_VERSION)
            .put("type", "TARGET_TAP")
            .put("pageId", GilnunBridge.PAGE_ID)
            .put("compatibleRevision", GilnunBridge.COMPATIBLE_REVISION)
            .put("stableKey", "review-next")
            .put("role", "button")
            .put("accessibleName", "신청 내용 확인")
            .put("checkpoint", "review-ready")
            .put("monotonicMs", 42L)
            .toString()
}
