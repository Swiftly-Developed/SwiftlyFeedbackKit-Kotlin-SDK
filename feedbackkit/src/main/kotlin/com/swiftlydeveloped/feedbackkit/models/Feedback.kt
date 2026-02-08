package com.swiftlydeveloped.feedbackkit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a feedback item.
 */
@Serializable
data class Feedback(
    val id: String,
    val title: String,
    val description: String,
    val status: FeedbackStatus,
    val category: FeedbackCategory,
    @SerialName("vote_count")
    val voteCount: Int,
    @SerialName("has_voted")
    val hasVoted: Boolean,
    @SerialName("comment_count")
    val commentCount: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    val email: String? = null
) {
    /**
     * Whether this feedback can be voted on (based on status).
     */
    val canVote: Boolean
        get() = status.canVote

    /**
     * Create a copy with updated vote state.
     */
    fun withVote(hasVoted: Boolean, voteCount: Int): Feedback = copy(
        hasVoted = hasVoted,
        voteCount = voteCount
    )

    /**
     * Create a copy with updated comment count.
     */
    fun withCommentCount(count: Int): Feedback = copy(commentCount = count)

    /**
     * Create a copy with updated status.
     */
    fun withStatus(status: FeedbackStatus): Feedback = copy(status = status)
}

/**
 * Request object for creating feedback.
 */
@Serializable
data class CreateFeedbackRequest(
    val title: String,
    val description: String,
    val category: FeedbackCategory,
    val email: String? = null,
    @SerialName("user_id")
    val userId: String? = null
)

/**
 * Options for listing feedback.
 */
data class ListFeedbackOptions(
    val status: FeedbackStatus? = null,
    val category: FeedbackCategory? = null,
    val page: Int? = null,
    val perPage: Int? = null
) {
    /**
     * Convert to query parameters map.
     */
    fun toQueryParams(): Map<String, String> = buildMap {
        status?.let { put("status", it.jsonValue) }
        category?.let { put("category", it.jsonValue) }
        page?.let { put("page", it.toString()) }
        perPage?.let { put("per_page", it.toString()) }
    }
}
