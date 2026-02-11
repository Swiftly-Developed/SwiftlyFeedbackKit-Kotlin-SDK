package com.swiftlydeveloped.feedbackkit.api

import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.http.FeedbackKitHttpClient
import com.swiftlydeveloped.feedbackkit.models.VoteResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * API for vote operations.
 */
class VotesApi internal constructor(
    private val http: FeedbackKitHttpClient
) {
    /**
     * Vote for a feedback item.
     *
     * @param feedbackId The ID of the feedback to vote for.
     * @param notifyOnStatusChange Whether to receive email notifications on status changes.
     * @return The vote response with updated counts.
     * @throws FeedbackKitError.ValidationError if no user ID is set.
     * @throws FeedbackKitError.ConflictError if already voted.
     */
    suspend fun vote(
        feedbackId: String,
        notifyOnStatusChange: Boolean = false,
        subscribeToMailingList: Boolean? = null,
        mailingListEmailTypes: List<String>? = null
    ): VoteResponse {
        val userId = http.userId
            ?: throw FeedbackKitError.ValidationError("User ID is required for voting")

        val request = VoteRequest(
            userId = userId,
            notifyOnStatusChange = notifyOnStatusChange,
            subscribeToMailingList = subscribeToMailingList,
            mailingListEmailTypes = mailingListEmailTypes
        )

        val body = http.jsonSerializer.encodeToString(request)

        return http.post(
            endpoint = "feedbacks/$feedbackId/votes",
            body = body
        ) { responseBody ->
            http.jsonSerializer.decodeFromString<VoteResponse>(responseBody)
        }
    }

    /**
     * Remove a vote from a feedback item.
     *
     * @param feedbackId The ID of the feedback to unvote.
     * @return The vote response with updated counts.
     * @throws FeedbackKitError.ValidationError if no user ID is set.
     * @throws FeedbackKitError.NotFoundError if not voted.
     */
    suspend fun unvote(feedbackId: String): VoteResponse {
        val userId = http.userId
            ?: throw FeedbackKitError.ValidationError("User ID is required for unvoting")

        return http.delete(
            endpoint = "feedbacks/$feedbackId/votes",
            queryParams = mapOf("user_id" to userId)
        ) { responseBody ->
            http.jsonSerializer.decodeFromString<VoteResponse>(responseBody)
        }
    }

    /**
     * Toggle the vote state for a feedback item.
     *
     * @param feedbackId The ID of the feedback.
     * @param hasVoted Current vote state.
     * @param notifyOnStatusChange Whether to receive email notifications (only used when voting).
     * @return The vote response with updated counts.
     */
    suspend fun toggleVote(
        feedbackId: String,
        hasVoted: Boolean,
        notifyOnStatusChange: Boolean = false,
        subscribeToMailingList: Boolean? = null,
        mailingListEmailTypes: List<String>? = null
    ): VoteResponse {
        return if (hasVoted) {
            unvote(feedbackId)
        } else {
            vote(feedbackId, notifyOnStatusChange, subscribeToMailingList, mailingListEmailTypes)
        }
    }

    @Serializable
    private data class VoteRequest(
        val userId: String,
        val notifyOnStatusChange: Boolean = false,
        val subscribeToMailingList: Boolean? = null,
        val mailingListEmailTypes: List<String>? = null
    )
}
