package com.gilnun.app.data

import android.content.SharedPreferences
import com.gilnun.app.catalog.ServiceId
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoStateStoreTest {
    @Test
    fun `load commits a successful version one migration back as version two`() {
        val fake = FakePreferences(mapOf(STATE_KEY to legacyState(helpLevel = 1)))
        val state = DemoStateStore(fake.preferences).load()

        assertEquals(setOf(1), state.services.values.map(ServiceProgress::helpLevel).toSet())
        assertEquals(1, fake.commitCalls)
        assertEquals(0, fake.applyCalls)
        assertTrue(fake.values.getValue(STATE_KEY).contains("\"schemaVersion\":2"))
    }

    @Test
    fun `failed migration commit returns migrated state and retries next load`() {
        val legacy = legacyState(helpLevel = 2)
        val fake =
            FakePreferences(
                initialValues = mapOf(STATE_KEY to legacy),
                commitResult = false,
            )
        val store = DemoStateStore(fake.preferences)

        repeat(2) {
            val state = store.load()
            assertEquals(
                setOf(2),
                ServiceId.entries.map { state.services.getValue(it).helpLevel }.toSet(),
            )
        }

        assertEquals(2, fake.commitCalls)
        assertEquals(legacy, fake.values.getValue(STATE_KEY))
    }

    @Test
    fun `regular save applies and clear removes only persisted practice state`() {
        val fake = FakePreferences(mapOf("unrelated" to "keep"))
        val store = DemoStateStore(fake.preferences)

        store.save(DemoState())

        assertEquals(1, fake.applyCalls)
        assertTrue(fake.values.getValue(STATE_KEY).contains("\"schemaVersion\":2"))

        store.clear()

        assertEquals(2, fake.applyCalls)
        assertNull(fake.values[STATE_KEY])
        assertEquals("keep", fake.values["unrelated"])
        assertFalse(fake.clearCalled)
    }

    private fun legacyState(helpLevel: Int): String =
        """{"schemaVersion":1,"patch":null,"helpLevel":$helpLevel,"lastReceipt":null}"""

    private class FakePreferences(
        initialValues: Map<String, String> = emptyMap(),
        private val commitResult: Boolean = true,
    ) {
        val values = initialValues.toMutableMap()
        var commitCalls = 0
        var applyCalls = 0
        var clearCalled = false

        val preferences: SharedPreferences =
            Proxy.newProxyInstance(
                SharedPreferences::class.java.classLoader,
                arrayOf(SharedPreferences::class.java),
            ) { _, method, arguments ->
                val args = arguments.orEmpty()
                when (method.name) {
                    "getAll" -> values.toMap()
                    "getString" -> values[args[0] as String] ?: args[1]
                    "contains" -> values.containsKey(args[0] as String)
                    "edit" -> editor()
                    "registerOnSharedPreferenceChangeListener",
                    "unregisterOnSharedPreferenceChangeListener",
                    -> null
                    else -> args.lastOrNull()
                }
            } as SharedPreferences

        private fun editor(): SharedPreferences.Editor {
            val pending = mutableMapOf<String, String?>()
            val removals = mutableSetOf<String>()
            var clearAll = false
            lateinit var proxy: SharedPreferences.Editor

            fun applyPending() {
                if (clearAll) values.clear()
                removals.forEach(values::remove)
                pending.forEach { (key, value) ->
                    if (value == null) values.remove(key) else values[key] = value
                }
            }

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
                        "remove" -> {
                            removals += args[0] as String
                            proxy
                        }
                        "clear" -> {
                            clearAll = true
                            clearCalled = true
                            proxy
                        }
                        "commit" -> {
                            commitCalls += 1
                            if (commitResult) applyPending()
                            commitResult
                        }
                        "apply" -> {
                            applyCalls += 1
                            applyPending()
                            null
                        }
                        else -> proxy
                    }
                } as SharedPreferences.Editor
            return proxy
        }
    }

    companion object {
        private const val STATE_KEY = "minimal_state"
    }
}
