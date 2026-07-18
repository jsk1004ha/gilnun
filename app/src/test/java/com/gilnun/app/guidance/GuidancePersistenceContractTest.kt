package com.gilnun.app.guidance

import android.content.SharedPreferences
import com.gilnun.app.catalog.ServiceCatalog
import com.gilnun.app.catalog.ServiceEventType
import com.gilnun.app.catalog.ServiceId
import com.gilnun.app.data.DemoState
import com.gilnun.app.data.DemoStateStore
import com.gilnun.app.data.GuidanceSource
import com.gilnun.app.data.ReceiptOutcome
import com.gilnun.app.data.ServiceProgress
import com.gilnun.app.web.BridgeEventV2
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuidancePersistenceContractTest {
    @Test
    fun `same-device helper producer persists only service patch and truthful sourced receipt`() {
        val service = ServiceCatalog.require(ServiceId.RESIDENT_RECORD)
        val step = service.steps.first()
        val patch = requireNotNull(step.patch)
        val action = requireNotNull(step.primaryAction)
        val producer = GuidanceReceiptCoordinator()
        check(producer.begin(service.id, step.id, patch, GuidanceSource.SAME_DEVICE_HELPER))
        producer.onAction(
            BridgeEventV2.ActionOrHelp(
                schemaVersion = 2,
                type = ServiceEventType.ACTION,
                serviceId = service.id,
                revision = service.revision,
                checkpoint = step.id,
                stableKey = action.stableKey,
                role = action.role,
                accessibleName = action.accessibleName,
                effect = action.effect,
            ),
        )
        val receipt =
            (
                producer.onCheckpointChanged(
                    BridgeEventV2.CheckpointChanged(
                        schemaVersion = 2,
                        serviceId = service.id,
                        revision = service.revision,
                        checkpoint = patch.expectedState,
                    ),
                ) as ReceiptTransition.Verified
            ).receipt

        val fake = MemoryPreferences()
        val store = DemoStateStore(fake.preferences)
        val services = DemoState().services.toMutableMap()
        services[service.id] =
            ServiceProgress(
                helperPatchesByCheckpoint = mapOf(step.id to patch),
                helpLevel = 2,
                lastReceipt = receipt,
            )
        store.save(DemoState(services))

        val restored = store.load()
        val restoredProgress = restored.services.getValue(service.id)
        assertEquals(mapOf(step.id to patch), restoredProgress.helperPatchesByCheckpoint)
        assertEquals(ReceiptOutcome.VERIFIED, restoredProgress.lastReceipt?.outcome)
        assertEquals(GuidanceSource.SAME_DEVICE_HELPER, restoredProgress.lastReceipt?.source)
        assertEquals(true, restoredProgress.lastReceipt?.guidanceShown)
        assertEquals(true, restoredProgress.lastReceipt?.userActionObserved)
        assertEquals(true, restoredProgress.lastReceipt?.postconditionVerified)
        ServiceId.entries.filterNot { it == service.id }.forEach { other ->
            assertEquals(emptyMap<String, Any>(), restored.services.getValue(other).helperPatchesByCheckpoint)
            assertNull(restored.services.getValue(other).lastReceipt)
        }
    }

    private class MemoryPreferences {
        private val values = mutableMapOf<String, String>()

        val preferences: SharedPreferences =
            Proxy.newProxyInstance(
                SharedPreferences::class.java.classLoader,
                arrayOf(SharedPreferences::class.java),
            ) { _, method, arguments ->
                val args = arguments.orEmpty()
                when (method.name) {
                    "getString" -> values[args[0] as String] ?: args[1]
                    "edit" -> editor()
                    else -> args.lastOrNull()
                }
            } as SharedPreferences

        private fun editor(): SharedPreferences.Editor {
            val pending = mutableMapOf<String, String?>()
            lateinit var proxy: SharedPreferences.Editor
            proxy =
                Proxy.newProxyInstance(
                    SharedPreferences.Editor::class.java.classLoader,
                    arrayOf(SharedPreferences.Editor::class.java),
                ) { _, method, arguments ->
                    val args = arguments.orEmpty()
                    when (method.name) {
                        "putString" -> {
                            pending[args[0] as String] = args[1] as String?
                            proxy
                        }
                        "apply" -> {
                            pending.forEach { (key, value) ->
                                if (value == null) values.remove(key) else values[key] = value
                            }
                            null
                        }
                        else -> proxy
                    }
                } as SharedPreferences.Editor
            return proxy
        }
    }
}
