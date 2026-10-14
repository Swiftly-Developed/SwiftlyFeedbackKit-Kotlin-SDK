package com.swiftlydeveloped.feedbackkit.ui

import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.models.VoteResponse
import kotlinx.coroutines.CancellationException

/**
 * Result of one vote or unvote attempt.
 *
 * Cancellation is rethrown. Leaving the screen must not be stored as a failed vote.
 */
internal sealed class VoteToggleOutcome {
    data class Applied(val response: VoteResponse) : VoteToggleOutcome()
    data class Failed(val error: FeedbackKitError) : VoteToggleOutcome()
}

internal suspend fun toggleVote(
    currentlyVoted: Boolean,
    feedbackId: String,
    vote: suspend (String) -> VoteResponse,
    unvote: suspend (String) -> VoteResponse
): VoteToggleOutcome {
    try {
        val response = if (currentlyVoted) unvote(feedbackId) else vote(feedbackId)
        return VoteToggleOutcome.Applied(response)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        return VoteToggleOutcome.Failed(FeedbackKitError.fromException(e))
    }
}
