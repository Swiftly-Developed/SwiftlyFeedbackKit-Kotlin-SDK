package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.swiftlydeveloped.feedbackkit.FeedbackKit
import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.models.VoteResponse
import com.swiftlydeveloped.feedbackkit.ui.VoteToggleOutcome
import com.swiftlydeveloped.feedbackkit.ui.toggleVote

/** The two network calls the vote capsule makes. A seam so tests run without a server. */
internal interface VoteActions {
    suspend fun vote(feedbackId: String): VoteResponse
    suspend fun unvote(feedbackId: String): VoteResponse
}

private object SharedVoteActions : VoteActions {
    override suspend fun vote(feedbackId: String) = FeedbackKit.shared.votes.vote(feedbackId)
    override suspend fun unvote(feedbackId: String) = FeedbackKit.shared.votes.unvote(feedbackId)
}

internal val LocalVoteActions = staticCompositionLocalOf<VoteActions> { SharedVoteActions }

/**
 * Optimistic vote state for one card. Same behaviour as the Classic [VoteButton][com.swiftlydeveloped.feedbackkit.ui.VoteButton]:
 * flip at once, settle on the server's answer, roll back on failure.
 */
@Stable
internal class VoteController(
    private val feedbackId: String,
    initialHasVoted: Boolean,
    initialVoteCount: Int,
    private val actions: VoteActions
) {
    var hasVoted by mutableStateOf(initialHasVoted)
        private set
    var voteCount by mutableIntStateOf(initialVoteCount)
        private set
    var isBusy by mutableStateOf(false)
        private set
    var error by mutableStateOf<FeedbackKitError?>(null)
        private set

    suspend fun toggle(
        onApplied: (VoteResponse) -> Unit = {},
        onFailed: (FeedbackKitError) -> Unit = {}
    ) {
        if (isBusy) return
        val previousHasVoted = hasVoted
        val previousVoteCount = voteCount
        hasVoted = !previousHasVoted
        voteCount = previousVoteCount + if (previousHasVoted) -1 else 1
        isBusy = true
        error = null
        try {
            when (val outcome = toggleVote(previousHasVoted, feedbackId, actions::vote, actions::unvote)) {
                is VoteToggleOutcome.Applied -> {
                    hasVoted = outcome.response.hasVoted
                    voteCount = outcome.response.voteCount
                    onApplied(outcome.response)
                }
                is VoteToggleOutcome.Failed -> {
                    hasVoted = previousHasVoted
                    voteCount = previousVoteCount
                    error = outcome.error
                    onFailed(outcome.error)
                }
            }
        } finally {
            isBusy = false
        }
    }
}
