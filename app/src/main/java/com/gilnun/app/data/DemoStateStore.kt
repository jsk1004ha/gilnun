package com.gilnun.app.data

import android.content.Context

/**
 * Persists only the small, reviewed demo state. Interaction events and form content never enter
 * SharedPreferences.
 */
class DemoStateStore(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): DemoState {
        val encoded = preferences.getString(STATE_KEY, null)
        return if (encoded == null) {
            DemoState(helpLevel = DEFAULT_HELP_LEVEL)
        } else {
            DemoStateCodec.decodeOrDefault(encoded)
        }
    }

    fun save(state: DemoState) {
        preferences
            .edit()
            .putString(STATE_KEY, DemoStateCodec.encode(state))
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "gilnun_demo_state_v1"
        private const val STATE_KEY = "minimal_state"
        private const val DEFAULT_HELP_LEVEL = 3
    }
}
