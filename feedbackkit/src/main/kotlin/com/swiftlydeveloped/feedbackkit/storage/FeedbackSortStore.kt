package com.swiftlydeveloped.feedbackkit.storage

import android.content.Context
import android.content.SharedPreferences
import com.swiftlydeveloped.feedbackkit.models.FeedbackSortOption

/**
 * Persists the list's sort choice. Filters and search text are never persisted
 * (FEEDBACK01 Q8): a user who returns to a silently filtered list believes requests are
 * missing.
 */
internal interface FeedbackSortStore {
    fun read(): FeedbackSortOption
    fun write(option: FeedbackSortOption)
}

internal class SharedPreferencesSortStore(
    private val preferences: SharedPreferences
) : FeedbackSortStore {
    override fun read(): FeedbackSortOption =
        FeedbackSortOption.fromStoredName(preferences.getString(KEY, null))

    override fun write(option: FeedbackSortOption) {
        preferences.edit().putString(KEY, option.name).apply()
    }

    companion object {
        const val PREFS_NAME = "feedbackkit_list"
        const val KEY = "feedbackkit_list_sort"

        fun from(context: Context): SharedPreferencesSortStore =
            SharedPreferencesSortStore(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    }
}
