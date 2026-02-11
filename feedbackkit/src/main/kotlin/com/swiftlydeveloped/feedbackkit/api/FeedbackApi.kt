package com.swiftlydeveloped.feedbackkit.api

import com.swiftlydeveloped.feedbackkit.http.FeedbackKitHttpClient
import com.swiftlydeveloped.feedbackkit.models.CreateFeedbackRequest
import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.ListFeedbackOptions
import kotlinx.serialization.encodeToString

/**
 * API for feedback operations.
 */
class FeedbackApi internal constructor(
    private val http: FeedbackKitHttpClient
) {
    /**
     * List all feedback items.
     *
     * @param options Optional filters for status, category, and pagination.
     * @return List of feedback items.
     */
    suspend fun list(options: ListFeedbackOptions = ListFeedbackOptions()): List<Feedback> {
        val queryParams = options.toQueryParams().toMutableMap()

        // Add user ID for hasVoted calculation
        http.userId?.let { queryParams["user_id"] = it }

        return http.get(
            endpoint = "feedbacks",
            queryParams = queryParams
        ) { body ->
            http.jsonSerializer.decodeFromString<List<Feedback>>(body)
        }
    }

    /**
     * Get a single feedback item by ID.
     *
     * @param id The feedback ID.
     * @return The feedback item.
     */
    suspend fun get(id: String): Feedback {
        val queryParams = buildMap {
            http.userId?.let { put("user_id", it) }
        }

        return http.get(
            endpoint = "feedbacks/$id",
            queryParams = queryParams
        ) { body ->
            http.jsonSerializer.decodeFromString<Feedback>(body)
        }
    }

    /**
     * Create a new feedback item.
     *
     * @param request The feedback creation request.
     * @return The created feedback item.
     */
    suspend fun create(request: CreateFeedbackRequest): Feedback {
        // Include user ID in the request
        val requestWithUserId = if (request.userId == null && http.userId != null) {
            request.copy(userId = http.userId)
        } else {
            request
        }

        val body = http.jsonSerializer.encodeToString(requestWithUserId)

        return http.post(
            endpoint = "feedbacks",
            body = body
        ) { responseBody ->
            http.jsonSerializer.decodeFromString<Feedback>(responseBody)
        }
    }

    /**
     * Create a new feedback item with individual parameters.
     *
     * @param title The feedback title.
     * @param description The feedback description.
     * @param category The feedback category.
     * @param email Optional email for the submitter.
     * @param subscribeToMailingList Whether the user consents to join the project's mailing list.
     * @param mailingListEmailTypes Email preference types (e.g. listOf("operational", "marketing")). Defaults to both when null.
     * @return The created feedback item.
     */
    suspend fun create(
        title: String,
        description: String,
        category: com.swiftlydeveloped.feedbackkit.models.FeedbackCategory,
        email: String? = null,
        subscribeToMailingList: Boolean? = null,
        mailingListEmailTypes: List<String>? = null
    ): Feedback = create(
        CreateFeedbackRequest(
            title = title,
            description = description,
            category = category,
            email = email,
            subscribeToMailingList = subscribeToMailingList,
            mailingListEmailTypes = mailingListEmailTypes
        )
    )
}
