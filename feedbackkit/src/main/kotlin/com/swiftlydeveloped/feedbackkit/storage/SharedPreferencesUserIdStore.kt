package com.swiftlydeveloped.feedbackkit.storage

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Anonymous user ids use the same shape as the Flutter and JS SDKs:
 * `local_` plus a lowercase UUID version 4. The prefs name and key match
 * Flutter's `feedbackkit_user_id`.
 *
 * SharedPreferences is the source of truth after the first [com.swiftlydeveloped.feedbackkit.FeedbackKit.configure].
 * When that store is empty, configure copies a non-blank id out of DataStore
 * (`user_id` in `feedbackkit_preferences`) before generating a new one.
 */
internal class SharedPreferencesUserIdStore(
    private val preferences: SharedPreferences
) {
    fun get(): String? = preferences.getString(KEY, null)?.trim()?.takeIf { it.isNotEmpty() }

    fun set(userId: String?) {
        val editor = preferences.edit()
        val trimmed = userId?.trim()?.takeIf { it.isNotEmpty() }
        if (trimmed == null) {
            editor.remove(KEY)
        } else {
            editor.putString(KEY, trimmed)
        }
        editor.commit()
    }

    companion object {
        const val PREFS_NAME = "feedbackkit_user_id"
        const val KEY = "feedbackkit_user_id"

        fun from(context: Context): SharedPreferencesUserIdStore {
            val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return SharedPreferencesUserIdStore(preferences)
        }
    }
}

/**
 * Explicit id, then a previously stored id, then a newly generated `local_` id.
 * Blank and whitespace-only values count as absent.
 */
internal fun resolveAnonymousUserId(
    configured: String?,
    stored: String?,
    generate: () -> String = ::generateLocalUserId
): String {
    val explicit = configured?.trim()?.takeIf { it.isNotEmpty() }
    if (explicit != null) return explicit
    val existing = stored?.trim()?.takeIf { it.isNotEmpty() }
    if (existing != null) return existing
    return generate()
}

internal fun generateLocalUserId(): String = "local_${UUID.randomUUID()}"
