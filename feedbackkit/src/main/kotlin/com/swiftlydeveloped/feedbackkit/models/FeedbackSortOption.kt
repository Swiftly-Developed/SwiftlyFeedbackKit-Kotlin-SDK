package com.swiftlydeveloped.feedbackkit.models

import androidx.annotation.StringRes
import com.getfeedbackkit.feedbackkit.R

/**
 * Client-side sort orders for the feedback list. The same four cases as the Swift and
 * JavaScript SDKs.
 *
 * The enum [name] is persisted, so renaming a case resets every user's saved choice.
 */
enum class FeedbackSortOption {
    /** Most votes first. The server already returns this order. */
    VOTES,

    /** Most recently created first. */
    NEWEST,

    /** Least recently created first. */
    OLDEST,

    /** Most comments first. */
    COMMENTS;

    /**
     * String resource ID for the localized display name.
     */
    @get:StringRes
    val displayNameRes: Int
        get() = when (this) {
            VOTES -> R.string.feedbackkit_sort_votes
            NEWEST -> R.string.feedbackkit_sort_newest
            OLDEST -> R.string.feedbackkit_sort_oldest
            COMMENTS -> R.string.feedbackkit_sort_comments
        }

    companion object {
        /** The sort used when nothing is stored. */
        val DEFAULT: FeedbackSortOption = VOTES

        /** Parses a stored [name]. Unknown or missing values return [DEFAULT]. */
        fun fromStoredName(value: String?): FeedbackSortOption =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}
