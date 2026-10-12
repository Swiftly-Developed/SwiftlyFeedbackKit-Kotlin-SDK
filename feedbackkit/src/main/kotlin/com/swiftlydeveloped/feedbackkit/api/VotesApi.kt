package com.swiftlydeveloped.feedbackkit.api

import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.http.FeedbackKitHttpClient
import com.swiftlydeveloped.feedbackkit.models.VoteResponse
import kotlinx.serialization.SerialName
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
     * @param notifyOnStatusChange Deprecated name for [notifyStatusChange]. Still
     *   accepted so existing positional and named call sites compile. The server
     *   reads `notify_status_change`. [notifyStatusChange] wins when both are set.
     * @return The vote response with updated counts.
     * @throws FeedbackKitError.ValidationError if no user ID is set.
     * @throws FeedbackKitError.ConflictError if already voted.
     */
    suspend fun vote(
        feedbackId: String,
        notifyOnStatusChange: Boolean = false,
        subscribeToMailingList: Boolean? = null,
        mailingListEmailTypes: List<String>? = null,
        email: String? = null,
        notifyStatusChange: Boolean? = null
    ): VoteResponse {
        val userId = http.userId
            ?: throw FeedbackKitError.ValidationError("User ID is required for voting")

        val request = VoteRequest(
            userId = userId,
            email = email,
            notifyOnStatusChange = notifyStatusChange ?: notifyOnStatusChange,
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

        // The server decodes CreateVoteDTO from the DELETE body. A query
        // parameter never reaches that decoder, so the unvote 400s.
        val body = http.jsonSerializer.encodeToString(UnvoteRequest(userId = userId))

        return http.delete(
            endpoint = "feedbacks/$feedbackId/votes",
            body = body
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
        mailingListEmailTypes: List<String>? = null,
        email: String? = null,
        notifyStatusChange: Boolean? = null
    ): VoteResponse {
        return if (hasVoted) {
            unvote(feedbackId)
        } else {
            vote(
                feedbackId = feedbackId,
                subscribeToMailingList = subscribeToMailingList,
                mailingListEmailTypes = mailingListEmailTypes,
                email = email,
                notifyStatusChange = notifyStatusChange ?: notifyOnStatusChange
            )
        }
    }

    @Serializable
    internal data class VoteRequest(
        @SerialName("user_id")
        val userId: String,
        val email: String? = null,
        @SerialName("notify_status_change")
        val notifyOnStatusChange: Boolean = false,
        @SerialName("subscribe_to_mailing_list")
        val subscribeToMailingList: Boolean? = null,
        @SerialName("mailing_list_email_types")
        val mailingListEmailTypes: List<String>? = null
    )

    @Serializable
    internal data class UnvoteRequest(
        @SerialName("user_id")
        val userId: String
    )
}
