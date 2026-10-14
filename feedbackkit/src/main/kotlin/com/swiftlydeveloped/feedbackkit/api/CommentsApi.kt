package com.swiftlydeveloped.feedbackkit.api

import com.swiftlydeveloped.feedbackkit.http.FeedbackKitHttpClient
import com.swiftlydeveloped.feedbackkit.models.Comment
import com.swiftlydeveloped.feedbackkit.models.CreateCommentRequest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

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
        val first = fetch(feedbackId, page, perPage)
        if (first.legacyArray || page != null) {
            return first.items
        }
        if (first.perPage < 1 || first.items.isEmpty() || first.page >= first.totalPages) {
            return first.items
        }
        val items = first.items.toMutableList()
        var next = first.page + 1
        var size = first.perPage
        var totalPages = first.totalPages
        while (next <= totalPages && next <= 100) {
            val more = fetch(feedbackId, next, size)
            if (more.legacyArray || more.items.isEmpty() || more.perPage < 1) {
                break
            }
            items += more.items
            if (more.page >= more.totalPages) {
                break
            }
            next = more.page + 1
            size = more.perPage
            totalPages = more.totalPages
        }
        return items
    }

    private suspend fun fetch(feedbackId: String, page: Int?, perPage: Int?): DecodedCommentPage {
        val queryParams = buildMap {
            put("paged", "1")
            page?.let { put("page", it.toString()) }
            perPage?.let { put("per_page", it.toString()) }
        }
        return http.get(
            endpoint = "feedbacks/$feedbackId/comments",
            queryParams = queryParams
        ) { body ->
            decodeCommentPage(http.jsonSerializer, body)
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

private data class DecodedCommentPage(
    val items: List<Comment>,
    val page: Int,
    val perPage: Int,
    val totalPages: Int,
    val legacyArray: Boolean
)

private fun decodeCommentPage(json: Json, body: String): DecodedCommentPage {
    val payload = json.parseToJsonElement(body)
    if (payload is JsonArray) {
        val items = payload.map { json.decodeFromJsonElement<Comment>(it) }
        return DecodedCommentPage(items, 1, items.size, 1, true)
    }
    val objectPayload = payload as? JsonObject
        ?: throw SerializationException("Comment list payload is not a JSON array or page")
    val rows = objectPayload["items"] as? JsonArray
        ?: throw SerializationException("Comment list page is missing items")
    val items = rows.map { json.decodeFromJsonElement<Comment>(it) }
    val meta = objectPayload["meta"] as? JsonObject
    return DecodedCommentPage(
        items = items,
        page = meta?.get("page")?.jsonPrimitive?.intOrNull ?: 1,
        perPage = meta?.get("per_page")?.jsonPrimitive?.intOrNull ?: items.size,
        totalPages = meta?.get("total_pages")?.jsonPrimitive?.intOrNull ?: 1,
        legacyArray = false
    )
}
