package com.swiftlydeveloped.feedbackkit.state

import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackSortOption
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import java.text.Normalizer
import java.util.Locale

/**
 * Client-side list derivation shared by both list styles (FEEDBACK01 §6, Q8).
 *
 * Search text, the status filter and the category filter combine with AND. Sorting is
 * stable, so items that tie keep the server's order.
 */
internal object FeedbackListFiltering {

    fun displayedItems(
        items: List<Feedback>,
        searchQuery: String,
        status: FeedbackStatus?,
        category: FeedbackCategory?,
        sort: FeedbackSortOption
    ): List<Feedback> {
        val needle = normalize(searchQuery.trim())
        val filtered = items.filter { item ->
            (status == null || item.status == status) &&
                (category == null || item.category == category) &&
                (needle.isEmpty() || matches(item, needle))
        }
        return sorted(filtered, sort)
    }

    fun sorted(items: List<Feedback>, sort: FeedbackSortOption): List<Feedback> = when (sort) {
        FeedbackSortOption.VOTES -> items.sortedByDescending { it.voteCount }
        FeedbackSortOption.NEWEST -> items.sortedWith(compareByDescending(::createdAtSortKey))
        FeedbackSortOption.OLDEST -> items.sortedWith(compareBy(::createdAtSortKey))
        FeedbackSortOption.COMMENTS -> items.sortedByDescending { it.commentCount }
    }

    /**
     * The server sends UTC ISO-8601 timestamps with optional fractional seconds. Splitting
     * the whole seconds from the fraction keeps `…:20Z` ahead of `…:20.5Z` without a date
     * parser, because `java.time` needs API 26 and minSdk is 24.
     */
    internal fun createdAtSortKey(item: Feedback): String {
        val raw = item.createdAt.trim()
        val seconds = raw.take(19)
        val fraction = raw.drop(19).takeWhile { it == '.' || it.isDigit() }.drop(1)
        return seconds + fraction.padEnd(9, '0')
    }

    private fun matches(item: Feedback, needle: String): Boolean =
        normalize(item.title).contains(needle) || normalize(item.description).contains(needle)

    /** Case- and diacritic-insensitive, like Swift's `localizedStandardContains`. */
    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .lowercase(Locale.ROOT)

    private val COMBINING_MARKS = Regex("\\p{Mn}+")
}
