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
    private var pendingMigrationRewrite: String? = null

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
    )

    @SuppressLint("ApplySharedPref") // Migration must confirm the rewrite before considering it done.
    fun load(): DemoState {
        pendingMigrationRewrite?.let { pending ->
            if (commitMigrationWithRetry(pending)) {
                pendingMigrationRewrite = null
            }
        }

        val encoded = preferences.getString(STATE_KEY, null) ?: return DemoState()
        val decoded = DemoStateCodec.decodeWithMetadata(encoded)
        if (decoded.migratedFromV1) {
            val migrated = DemoStateCodec.encode(decoded.state)
            pendingMigrationRewrite = migrated
            if (commitMigrationWithRetry(migrated)) {
                pendingMigrationRewrite = null
            }
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
        pendingMigrationRewrite = null
        preferences.edit().remove(STATE_KEY).apply()
    }

    @SuppressLint("ApplySharedPref")
    private fun commitMigrationWithRetry(encoded: String): Boolean {
        repeat(MIGRATION_COMMIT_ATTEMPTS) {
            if (preferences.edit().putString(STATE_KEY, encoded).commit()) {
                return true
            }
        }
        return false
    }

    companion object {
        private const val PREFERENCES_NAME = "gilnun_demo_state_v1"
        private const val STATE_KEY = "minimal_state"
        private const val MIGRATION_COMMIT_ATTEMPTS = 2
    }
}
