package com.swiftlydeveloped.feedbackkit.state

import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.models.ListFeedbackOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackListStateTest {

    @Test
    fun `a superseded load does not record cancellation as an error`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val state = FeedbackListState(
            scope = this,
            loadItems = { options ->
                calls += 1
                if (calls == 1) {
                    gate.await()
                    listOf(sample("stale", options))
                } else {
                    listOf(sample("fresh", options))
                }
            }
        )

        state.load(status = FeedbackStatus.PENDING)
        advanceUntilIdle()
        state.syncExternalFilters(FeedbackStatus.APPROVED, FeedbackCategory.BUG_REPORT)
        advanceUntilIdle()

        assertNull(state.error)
        assertEquals(listOf("fresh"), state.feedbackItems.map { it.id })
        assertEquals(FeedbackStatus.APPROVED, state.statusFilter)
        assertEquals(FeedbackCategory.BUG_REPORT, state.categoryFilter)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `syncExternalFilters does not reload when the filters are unchanged`() = runTest {
        var calls = 0
        val state = FeedbackListState(
            scope = this,
            initialStatusFilter = FeedbackStatus.PENDING,
            loadItems = {
                calls += 1
                listOf(sample("a", it))
            }
        )

        state.load()
        advanceUntilIdle()
        state.syncExternalFilters(FeedbackStatus.PENDING, null)
        advanceUntilIdle()

        assertEquals(1, calls)
        assertEquals(listOf("a"), state.feedbackItems.map { it.id })
    }

    private fun sample(id: String, options: ListFeedbackOptions): Feedback = Feedback(
        id = id,
        title = options.status?.jsonValue ?: "title",
        description = "description",
        status = options.status ?: FeedbackStatus.PENDING,
        category = options.category ?: FeedbackCategory.OTHER,
        voteCount = 1,
        hasVoted = false,
        commentCount = 0,
        createdAt = "2023-11-15T00:13:20Z"
    )
}
