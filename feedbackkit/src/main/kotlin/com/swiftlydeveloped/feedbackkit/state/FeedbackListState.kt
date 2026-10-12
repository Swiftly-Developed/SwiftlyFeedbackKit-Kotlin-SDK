package com.swiftlydeveloped.feedbackkit.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.swiftlydeveloped.feedbackkit.FeedbackKit
import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.models.ListFeedbackOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * State holder for feedback list with loading, error, and refresh states.
 */
class FeedbackListState(
    private val scope: CoroutineScope,
    initialStatusFilter: FeedbackStatus? = null,
    initialCategoryFilter: FeedbackCategory? = null,
    private val loadItems: suspend (ListFeedbackOptions) -> List<Feedback> = { options ->
        FeedbackKit.shared.feedback.list(options)
    }
) {
    /**
     * List of feedback items.
     */
    var feedbackItems by mutableStateOf<List<Feedback>>(emptyList())
        private set

    /**
     * Whether the list is loading.
     */
    var isLoading by mutableStateOf(false)
        private set

    /**
     * Whether the list is refreshing (pull-to-refresh).
     */
    var isRefreshing by mutableStateOf(false)
        private set

    /**
     * Current error, if any.
     */
    var error by mutableStateOf<FeedbackKitError?>(null)
        private set

    /**
     * Current status filter.
     */
    var statusFilter by mutableStateOf(initialStatusFilter)
        private set

    /**
     * Current category filter.
     */
    var categoryFilter by mutableStateOf(initialCategoryFilter)
        private set

    private var loadJob: Job? = null
    private var requestGeneration = 0

    /**
     * Load feedback items with optional filters.
     */
    fun load(
        status: FeedbackStatus? = statusFilter,
        category: FeedbackCategory? = categoryFilter
    ) {
        beginRequest(refreshing = false, status = status, category = category)
    }

    /**
     * Refresh the feedback list (pull-to-refresh).
     */
    fun refresh() {
        beginRequest(refreshing = true, status = statusFilter, category = categoryFilter)
    }

    /**
     * Reload when a parent passes new filters. An unchanged pair does not start
     * another request, so the first composition does not fetch twice.
     */
    fun syncExternalFilters(status: FeedbackStatus?, category: FeedbackCategory?) {
        if (status == statusFilter && category == categoryFilter) return
        load(status = status, category = category)
    }

    private fun beginRequest(
        refreshing: Boolean,
        status: FeedbackStatus?,
        category: FeedbackCategory?
    ) {
        val generation = ++requestGeneration
        loadJob?.cancel()
        loadJob = scope.launch {
            if (refreshing) {
                isRefreshing = true
            } else {
                isLoading = true
            }
            error = null

            try {
                val options = ListFeedbackOptions(
                    status = status,
                    category = category
                )
                feedbackItems = loadItems(options)
                statusFilter = status
                categoryFilter = category
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestGeneration == generation) {
                    error = FeedbackKitError.fromException(e)
                }
            } finally {
                if (requestGeneration == generation) {
                    isLoading = false
                    isRefreshing = false
                }
            }
        }
    }

    /**
     * Update a single feedback item in the list.
     */
    fun updateFeedback(updatedFeedback: Feedback) {
        feedbackItems = feedbackItems.map { feedback ->
            if (feedback.id == updatedFeedback.id) updatedFeedback else feedback
        }
    }

    /**
     * Add a new feedback item to the list.
     */
    fun addFeedback(feedback: Feedback) {
        feedbackItems = listOf(feedback) + feedbackItems
    }

    /**
     * Remove a feedback item from the list.
     */
    fun removeFeedback(feedbackId: String) {
        feedbackItems = feedbackItems.filter { it.id != feedbackId }
    }

    /**
     * Clear the current error.
     */
    fun clearError() {
        error = null
    }

    /**
     * Apply a status filter and reload.
     */
    fun filterByStatus(status: FeedbackStatus?) {
        load(status = status)
    }

    /**
     * Apply a category filter and reload.
     */
    fun filterByCategory(category: FeedbackCategory?) {
        load(category = category)
    }

    /**
     * Clear all filters and reload.
     */
    fun clearFilters() {
        load(status = null, category = null)
    }
}

/**
 * Create and remember a FeedbackListState.
 */
@androidx.compose.runtime.Composable
fun rememberFeedbackListState(
    initialStatusFilter: FeedbackStatus? = null,
    initialCategoryFilter: FeedbackCategory? = null,
    loadOnCreate: Boolean = true
): FeedbackListState {
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    return androidx.compose.runtime.remember(scope) {
        FeedbackListState(
            scope = scope,
            initialStatusFilter = initialStatusFilter,
            initialCategoryFilter = initialCategoryFilter
        ).also {
            if (loadOnCreate) {
                it.load()
            }
        }
    }
}
