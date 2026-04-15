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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State holder for feedback list with loading, error, and refresh states.
 */
class FeedbackListState(
    private val scope: CoroutineScope,
    initialStatusFilter: FeedbackStatus? = null,
    initialCategoryFilter: FeedbackCategory? = null
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

    /**
     * Load feedback items with optional filters.
     */
    fun load(
        status: FeedbackStatus? = statusFilter,
        category: FeedbackCategory? = categoryFilter
    ) {
        loadJob?.cancel()
        loadJob = scope.launch {
            isLoading = true
            error = null

            try {
                val options = ListFeedbackOptions(
                    status = status,
                    category = category
                )

                feedbackItems = withContext(Dispatchers.IO) {
                    FeedbackKit.shared.feedback.list(options)
                }

                statusFilter = status
                categoryFilter = category
            } catch (e: Exception) {
                error = FeedbackKitError.fromException(e)
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Refresh the feedback list (pull-to-refresh).
     */
    fun refresh() {
        loadJob?.cancel()
        loadJob = scope.launch {
            isRefreshing = true
            error = null

            try {
                val options = ListFeedbackOptions(
                    status = statusFilter,
                    category = categoryFilter
                )

                feedbackItems = withContext(Dispatchers.IO) {
                    FeedbackKit.shared.feedback.list(options)
                }
            } catch (e: Exception) {
                error = FeedbackKitError.fromException(e)
            } finally {
                isRefreshing = false
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
