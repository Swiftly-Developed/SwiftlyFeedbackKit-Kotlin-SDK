package com.swiftlydeveloped.feedbackkit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swiftlydeveloped.feedbackkit.FeedbackKit
import com.swiftlydeveloped.feedbackkit.R
import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.VoteResponse
import com.swiftlydeveloped.feedbackkit.state.LocalFeedbackKitTheme
import com.swiftlydeveloped.feedbackkit.theme.FeedbackKitTheme
import kotlinx.coroutines.launch

/**
 * Vote button composable with optimistic updates.
 *
 * @param feedback The feedback item to vote on.
 * @param modifier Modifier for the button.
 * @param enabled Whether the button is enabled.
 * @param onVoteChange Callback when the vote state changes.
 * @param theme Optional theme override.
 */
@Composable
fun VoteButton(
    feedback: Feedback,
    modifier: Modifier = Modifier,
    enabled: Boolean = feedback.canVote,
    onVoteChange: ((VoteResponse) -> Unit)? = null,
    theme: FeedbackKitTheme = LocalFeedbackKitTheme.current
) {
    // Local state for optimistic updates
    var localHasVoted by remember(feedback.id, feedback.hasVoted) {
        mutableStateOf(feedback.hasVoted)
    }
    var localVoteCount by remember(feedback.id, feedback.voteCount) {
        mutableIntStateOf(feedback.voteCount)
    }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val backgroundColor = if (localHasVoted) {
        theme.voteActiveColor.copy(alpha = 0.1f)
    } else {
        theme.cardBackgroundColor
    }

    val contentColor = if (localHasVoted) {
        theme.voteActiveColor
    } else {
        theme.voteInactiveColor
    }

    Surface(
        onClick = {
            if (!enabled || isLoading) return@Surface

            scope.launch {
                val previousHasVoted = localHasVoted
                val previousVoteCount = localVoteCount

                // Optimistic update
                localHasVoted = !localHasVoted
                localVoteCount += if (localHasVoted) 1 else -1
                isLoading = true

                try {
                    val response = if (previousHasVoted) {
                        FeedbackKit.shared.votes.unvote(feedback.id)
                    } else {
                        FeedbackKit.shared.votes.vote(feedback.id)
                    }

                    // Update with server response
                    localHasVoted = response.hasVoted
                    localVoteCount = response.voteCount
                    onVoteChange?.invoke(response)
                } catch (e: Exception) {
                    // Revert on error
                    localHasVoted = previousHasVoted
                    localVoteCount = previousVoteCount
                } finally {
                    isLoading = false
                }
            }
        },
        modifier = modifier.size(56.dp),
        shape = RoundedCornerShape(theme.borderRadius.dp),
        color = backgroundColor,
        enabled = enabled
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = stringResource(if (localHasVoted) R.string.feedbackkit_remove_vote else R.string.feedbackkit_vote),
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = localVoteCount.toString(),
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = if (localHasVoted) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Standalone vote button that doesn't require a Feedback object.
 */
@Composable
fun VoteButton(
    feedbackId: String,
    voteCount: Int,
    hasVoted: Boolean,
    canVote: Boolean = true,
    modifier: Modifier = Modifier,
    onVoteChange: ((VoteResponse) -> Unit)? = null,
    theme: FeedbackKitTheme = LocalFeedbackKitTheme.current
) {
    // Local state for optimistic updates
    var localHasVoted by remember(feedbackId, hasVoted) {
        mutableStateOf(hasVoted)
    }
    var localVoteCount by remember(feedbackId, voteCount) {
        mutableIntStateOf(voteCount)
    }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val backgroundColor = if (localHasVoted) {
        theme.voteActiveColor.copy(alpha = 0.1f)
    } else {
        theme.cardBackgroundColor
    }

    val contentColor = if (localHasVoted) {
        theme.voteActiveColor
    } else {
        theme.voteInactiveColor
    }

    Surface(
        onClick = {
            if (!canVote || isLoading) return@Surface

            scope.launch {
                val previousHasVoted = localHasVoted
                val previousVoteCount = localVoteCount

                // Optimistic update
                localHasVoted = !localHasVoted
                localVoteCount += if (localHasVoted) 1 else -1
                isLoading = true

                try {
                    val response = if (previousHasVoted) {
                        FeedbackKit.shared.votes.unvote(feedbackId)
                    } else {
                        FeedbackKit.shared.votes.vote(feedbackId)
                    }

                    // Update with server response
                    localHasVoted = response.hasVoted
                    localVoteCount = response.voteCount
                    onVoteChange?.invoke(response)
                } catch (e: Exception) {
                    // Revert on error
                    localHasVoted = previousHasVoted
                    localVoteCount = previousVoteCount
                } finally {
                    isLoading = false
                }
            }
        },
        modifier = modifier.size(56.dp),
        shape = RoundedCornerShape(theme.borderRadius.dp),
        color = backgroundColor,
        enabled = canVote
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = stringResource(if (localHasVoted) R.string.feedbackkit_remove_vote else R.string.feedbackkit_vote),
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = localVoteCount.toString(),
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = if (localHasVoted) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VoteButtonPreview() {
    VoteButton(
        feedbackId = "1",
        voteCount = 42,
        hasVoted = false
    )
}

@Preview(showBackground = true)
@Composable
private fun VoteButtonVotedPreview() {
    VoteButton(
        feedbackId = "1",
        voteCount = 43,
        hasVoted = true
    )
}
