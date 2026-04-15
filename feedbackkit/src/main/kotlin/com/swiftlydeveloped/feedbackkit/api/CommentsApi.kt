package com.swiftlydeveloped.feedbackkit.api

import com.swiftlydeveloped.feedbackkit.http.FeedbackKitHttpClient
import com.swiftlydeveloped.feedbackkit.models.Comment
import com.swiftlydeveloped.feedbackkit.models.CreateCommentRequest
import kotlinx.serialization.encodeToString

/**
 * API for comment operations.
 */
class CommentsApi internal constructor(
    private val http: FeedbackKitHttpClient
) {
    /**
     * List all comments for a feedback item.
     *
     * @param feedbackId The ID of the feedback.
     * @param page Optional page number for pagination.
     * @param perPage Optional number of items per page.
     * @return List of comments.
     */
    suspend fun list(
        feedbackId: String,
        page: Int? = null,
        perPage: Int? = null
    ): List<Comment> {
        val queryParams = buildMap {
            page?.let { put("page", it.toString()) }
            perPage?.let { put("per_page", it.toString()) }
        }

        return http.get(
            endpoint = "feedbacks/$feedbackId/comments",
            queryParams = queryParams
        ) { body ->
            http.jsonSerializer.decodeFromString<List<Comment>>(body)
        }
    }

    /**
     * Create a comment on a feedback item.
     *
     * @param feedbackId The ID of the feedback.
     * @param request The comment creation request.
     * @return The created comment.
     */
    suspend fun create(feedbackId: String, request: CreateCommentRequest): Comment {
        // Include user ID in the request if not already set
        val requestWithUserId = if (request.userId == null && http.userId != null) {
            request.copy(userId = http.userId)
        } else {
            request
        }

        val body = http.jsonSerializer.encodeToString(requestWithUserId)

        return http.post(
            endpoint = "feedbacks/$feedbackId/comments",
            body = body
        ) { responseBody ->
            http.jsonSerializer.decodeFromString<Comment>(responseBody)
        }
    }

    /**
     * Create a comment with individual parameters.
     *
     * @param feedbackId The ID of the feedback.
     * @param content The comment content.
     * @param userName Optional user display name.
     * @return The created comment.
     */
    suspend fun create(
        feedbackId: String,
        content: String,
        userName: String? = null
    ): Comment = create(
        feedbackId = feedbackId,
        request = CreateCommentRequest(
            content = content,
            userName = userName
        )
    )
}
