package com.swiftlydeveloped.feedbackkit.state

import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackSortOption
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.ui.showcase.feedback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackListFilteringTest {

    private val items = listOf(
        feedback("a", title = "Crème brûlée recipes", status = FeedbackStatus.APPROVED, category = FeedbackCategory.FEATURE_REQUEST, votes = 5, comments = 1, createdAt = "2026-10-03T08:00:00Z"),
        feedback("b", title = "Crash on launch", description = "Happens after the CREME update", status = FeedbackStatus.APPROVED, category = FeedbackCategory.BUG_REPORT, votes = 9, comments = 4, createdAt = "2026-10-01T08:00:00Z"),
        feedback("c", title = "Dark mode", status = FeedbackStatus.IN_PROGRESS, category = FeedbackCategory.FEATURE_REQUEST, votes = 9, comments = 0, createdAt = "2026-10-02T08:00:00Z"),
        feedback("d", title = "Tune the sync", status = FeedbackStatus.COMPLETED, category = FeedbackCategory.IMPROVEMENT, votes = 1, comments = 7, createdAt = "2026-10-02T08:00:00.5Z")
    )

    private fun ids(
        query: String = "",
        status: FeedbackStatus? = null,
        category: FeedbackCategory? = null,
        sort: FeedbackSortOption = FeedbackSortOption.VOTES
    ) = FeedbackListFiltering.displayedItems(items, query, status, category, sort).map { it.id }

    @Test
    fun `search ignores case and diacritics and matches title or description`() {
        assertEquals(listOf("b", "a"), ids(query = "creme"))
        assertEquals(listOf("b", "a"), ids(query = "  CRÈME "))
        assertEquals(listOf("a"), ids(query = "brulee"))
    }

    @Test
    fun `blank search shows everything`() {
        assertEquals(4, ids(query = "   ").size)
    }

    @Test
    fun `status, category and search combine with AND`() {
        assertEquals(listOf("b", "a"), ids(status = FeedbackStatus.APPROVED))
        assertEquals(listOf("a"), ids(status = FeedbackStatus.APPROVED, category = FeedbackCategory.FEATURE_REQUEST))
        assertEquals(listOf("b"), ids(query = "crash", status = FeedbackStatus.APPROVED))
    }

    @Test
    fun `an empty intersection returns no items`() {
        assertTrue(ids(status = FeedbackStatus.COMPLETED, category = FeedbackCategory.BUG_REPORT).isEmpty())
        assertTrue(ids(query = "kangaroo").isEmpty())
    }

    @Test
    fun `votes sort is descending and stable for ties`() {
        assertEquals(listOf("b", "c", "a", "d"), ids(sort = FeedbackSortOption.VOTES))
    }

    @Test
    fun `comments sort is descending`() {
        assertEquals(listOf("d", "b", "a", "c"), ids(sort = FeedbackSortOption.COMMENTS))
    }

    @Test
    fun `newest and oldest order by created time including fractional seconds`() {
        assertEquals(listOf("a", "d", "c", "b"), ids(sort = FeedbackSortOption.NEWEST))
        assertEquals(listOf("b", "c", "d", "a"), ids(sort = FeedbackSortOption.OLDEST))
    }

    @Test
    fun `created at sort key puts whole seconds before fractional ones`() {
        val whole = FeedbackListFiltering.createdAtSortKey(feedback("x", createdAt = "2026-10-02T08:00:20Z"))
        val half = FeedbackListFiltering.createdAtSortKey(feedback("y", createdAt = "2026-10-02T08:00:20.5Z"))
        val micros = FeedbackListFiltering.createdAtSortKey(feedback("z", createdAt = "2026-10-02T08:00:20.000001Z"))
        assertTrue(whole < micros)
        assertTrue(micros < half)
    }
}
