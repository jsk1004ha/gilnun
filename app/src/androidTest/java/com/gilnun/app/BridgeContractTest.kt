package com.gilnun.app

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gilnun.app.catalog.ServiceCatalog
import com.gilnun.app.catalog.ServiceId
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
    fun exactOwnedOriginMainFrameAndV2CatalogEventAreAccepted() {
        val result =
            GilnunBridge.accept(
                payload = validPayload(),
                sourceOrigin = Uri.parse(GilnunBridge.APP_ORIGIN),
                isMainFrame = true,
            )

        assertTrue(result is BridgeResult.Accepted)
    }

    @Test
    fun foreignOriginSubframeExtraDataAndCrossServiceFailClosed() {
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
        assertEquals(
            BridgeResult.Rejected(BridgeError.INVALID_CONTRACT),
            GilnunBridge.parse(
                JSONObject(validPayload())
                    .put("serviceId", ServiceId.RESIDENT_RECORD.persistedKey)
                    .toString(),
            ),
        )
    }

    private fun validPayload(): String {
        val service = ServiceCatalog.require(ServiceId.BASIC_PENSION)
        val checkpoint = service.steps.first()
        val action = requireNotNull(checkpoint.primaryAction)
        return JSONObject()
            .put("schemaVersion", GilnunBridge.SCHEMA_VERSION)
            .put("type", action.type.name)
            .put("serviceId", service.id.persistedKey)
            .put("revision", service.revision)
            .put("checkpoint", checkpoint.id)
            .put("stableKey", action.stableKey)
            .put("role", action.role)
            .put("accessibleName", action.accessibleName)
            .put("effect", action.effect.name)
            .toString()
    }
}
