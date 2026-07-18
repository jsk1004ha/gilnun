package com.gilnun.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

/**
 * Persists only the small, reviewed demo state. Interaction events and form content never enter
 * SharedPreferences.
 */
class DemoStateStore internal constructor(
    private val preferences: SharedPreferences,
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
    )

    @SuppressLint("ApplySharedPref") // Migration must confirm the rewrite before considering it done.
    fun load(): DemoState {
        val encoded = preferences.getString(STATE_KEY, null) ?: return DemoState()
        val decoded = DemoStateCodec.decodeWithMetadata(encoded)
        if (decoded.migratedFromV1) {
            preferences
                .edit()
                .putString(STATE_KEY, DemoStateCodec.encode(decoded.state))
                .commit()
        }
        return decoded.state
    }

    fun save(state: DemoState) {
        preferences
            .edit()
            .putString(STATE_KEY, DemoStateCodec.encode(state))
            .apply()
    }

    fun clear() {
        preferences.edit().remove(STATE_KEY).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "gilnun_demo_state_v1"
        private const val STATE_KEY = "minimal_state"
    }
}
