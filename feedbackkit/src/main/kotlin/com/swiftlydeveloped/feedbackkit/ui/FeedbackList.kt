package com.swiftlydeveloped.feedbackkit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swiftlydeveloped.feedbackkit.R
import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.errors.userMessage
import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.models.VoteResponse
import com.swiftlydeveloped.feedbackkit.state.FeedbackListState
import com.swiftlydeveloped.feedbackkit.state.LocalFeedbackKitTheme
import com.swiftlydeveloped.feedbackkit.state.rememberFeedbackListState
import com.swiftlydeveloped.feedbackkit.theme.FeedbackKitTheme

/**
 * List composable that displays feedback items.
 *
 * @param modifier Modifier for the list.
 * @param state State holder for the feedback list.
 * @param onFeedbackClick Callback when a feedback item is clicked.
 * @param onVoteChange Callback when a vote changes.
 * @param theme Optional theme override.
 * @param emptyContent Custom content to show when the list is empty.
 * @param errorContent Custom content to show when there's an error.
 * @param loadingContent Custom content to show while loading.
 */
@Composable
fun FeedbackList(
    modifier: Modifier = Modifier,
    state: FeedbackListState = rememberFeedbackListState(),
    onFeedbackClick: ((Feedback) -> Unit)? = null,
    onVoteChange: ((Feedback, VoteResponse) -> Unit)? = null,
    theme: FeedbackKitTheme = LocalFeedbackKitTheme.current,
    emptyContent: @Composable () -> Unit = { DefaultEmptyContent(theme) },
    errorContent: @Composable (FeedbackKitError) -> Unit = { error ->
        DefaultErrorContent(error = error, onRetry = { state.load() }, theme = theme)
    },
    loadingContent: @Composable () -> Unit = { DefaultLoadingContent(theme) }
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            // Initial loading
            state.isLoading && state.feedbackItems.isEmpty() -> {
                loadingContent()
            }

            // Error state
            state.error != null && state.feedbackItems.isEmpty() -> {
                errorContent(state.error!!)
            }

            // Empty state
            state.feedbackItems.isEmpty() -> {
                emptyContent()
            }

            // List content
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(theme.spacing.dp * 2),
                    verticalArrangement = Arrangement.spacedBy(theme.spacing.dp * 2)
                ) {
                    items(
                        items = state.feedbackItems,
                        key = { it.id }
                    ) { feedback ->
                        FeedbackCard(
                            feedback = feedback,
                            onClick = { onFeedbackClick?.invoke(feedback) },
                            onVoteChange = { response ->
                                state.updateFeedback(
                                    feedback.withVote(response.hasVoted, response.voteCount)
                                )
                                onVoteChange?.invoke(feedback, response)
                            },
                            theme = theme
                        )
                    }
                }
            }
        }

        // Show refresh indicator when refreshing
        if (state.isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                color = theme.primaryColor
            )
        }
    }
}

/**
 * Simplified FeedbackList that manages its own state.
 */
@Composable
fun FeedbackList(
    modifier: Modifier = Modifier,
    statusFilter: FeedbackStatus? = null,
    categoryFilter: FeedbackCategory? = null,
    onFeedbackClick: ((Feedback) -> Unit)? = null,
    theme: FeedbackKitTheme = LocalFeedbackKitTheme.current
) {
    val state = rememberFeedbackListState(
        initialStatusFilter = statusFilter,
        initialCategoryFilter = categoryFilter
    )

    FeedbackList(
        modifier = modifier,
        state = state,
        onFeedbackClick = onFeedbackClick,
        theme = theme
    )
}

@Composable
private fun DefaultEmptyContent(theme: FeedbackKitTheme) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(theme.spacing.dp * 4)
        ) {
            Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = null,
                tint = theme.secondaryTextColor,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(theme.spacing.dp * 2))
            Text(
                text = stringResource(R.string.feedbackkit_list_empty),
                color = theme.textColor,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(theme.spacing.dp))
            Text(
                text = stringResource(R.string.feedbackkit_list_empty_description),
                color = theme.secondaryTextColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DefaultErrorContent(
    error: FeedbackKitError,
    onRetry: () -> Unit,
    theme: FeedbackKitTheme
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(theme.spacing.dp * 4)
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = theme.errorColor,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(theme.spacing.dp * 2))
            Text(
                text = stringResource(R.string.feedbackkit_list_error),
                color = theme.textColor,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(theme.spacing.dp))
            Text(
                text = error.userMessage,
                color = theme.secondaryTextColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(theme.spacing.dp * 2))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.feedbackkit_list_retry))
            }
        }
    }
}

@Composable
private fun DefaultLoadingContent(theme: FeedbackKitTheme) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = theme.primaryColor
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedbackListEmptyPreview() {
    Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
        DefaultEmptyContent(FeedbackKitTheme.Light)
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedbackListErrorPreview() {
    Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
        DefaultErrorContent(
            error = FeedbackKitError.NetworkError("Unable to connect"),
            onRetry = {},
            theme = FeedbackKitTheme.Light
        )
    }
}
