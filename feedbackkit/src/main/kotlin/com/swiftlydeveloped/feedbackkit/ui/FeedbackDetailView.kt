package com.swiftlydeveloped.feedbackkit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swiftlydeveloped.feedbackkit.FeedbackKit
import com.swiftlydeveloped.feedbackkit.R
import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.models.Comment
import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.models.VoteResponse
import com.swiftlydeveloped.feedbackkit.state.LocalFeedbackKitTheme
import com.swiftlydeveloped.feedbackkit.theme.FeedbackKitTheme

/**
 * Detail view composable that displays a full feedback item with comments.
 *
 * @param feedback The feedback item to display.
 * @param modifier Modifier for the view.
 * @param onBack Callback when the back button is pressed.
 * @param onVoteChange Callback when the vote state changes.
 * @param showAppBar Whether to show the app bar with back button.
 * @param theme Optional theme override.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDetailView(
    feedback: Feedback,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onVoteChange: ((VoteResponse) -> Unit)? = null,
    showAppBar: Boolean = true,
    theme: FeedbackKitTheme = LocalFeedbackKitTheme.current
) {
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var isLoadingComments by remember { mutableStateOf(true) }
    var commentsError by remember { mutableStateOf<FeedbackKitError?>(null) }

    // Load comments
    LaunchedEffect(feedback.id) {
        try {
            comments = FeedbackKit.shared.comments.list(feedback.id)
        } catch (e: Exception) {
            commentsError = FeedbackKitError.fromException(e)
        } finally {
            isLoadingComments = false
        }
    }

    val content: @Composable () -> Unit = {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(theme.spacing.dp * 2),
            verticalArrangement = Arrangement.spacedBy(theme.spacing.dp * 2)
        ) {
            // Header with vote button
            item {
                FeedbackDetailHeader(
                    feedback = feedback,
                    onVoteChange = onVoteChange,
                    theme = theme
                )
            }

            // Description
            item {
                FeedbackDetailDescription(
                    description = feedback.description,
                    theme = theme
                )
            }

            // Divider
            item {
                Divider(color = theme.borderColor)
            }

            // Comments section header
            item {
                Text(
                    text = stringResource(R.string.feedbackkit_detail_comments, feedback.commentCount),
                    color = theme.textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Comments
            when {
                isLoadingComments -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(theme.spacing.dp * 4),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = theme.primaryColor)
                        }
                    }
                }

                commentsError != null -> {
                    item {
                        Text(
                            text = stringResource(R.string.feedbackkit_detail_comments_error),
                            color = theme.errorColor,
                            fontSize = 14.sp
                        )
                    }
                }

                comments.isEmpty() -> {
                    item {
                        Text(
                            text = stringResource(R.string.feedbackkit_detail_comments_empty),
                            color = theme.secondaryTextColor,
                            fontSize = 14.sp
                        )
                    }
                }

                else -> {
                    items(comments, key = { it.id }) { comment ->
                        CommentCard(comment = comment, theme = theme)
                    }
                }
            }
        }
    }

    if (showAppBar) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.feedbackkit_detail_title)) },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.feedbackkit_back)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = theme.backgroundColor
                    )
                )
            },
            containerColor = theme.backgroundColor
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                content()
            }
        }
    } else {
        content()
    }
}

@Composable
private fun FeedbackDetailHeader(
    feedback: Feedback,
    onVoteChange: ((VoteResponse) -> Unit)?,
    theme: FeedbackKitTheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.dp * 2)
    ) {
        VoteButton(
            feedback = feedback,
            onVoteChange = onVoteChange,
            theme = theme
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(theme.spacing.dp)) {
                StatusBadge(status = feedback.status, theme = theme)
                CategoryBadge(category = feedback.category, theme = theme)
            }

            Spacer(modifier = Modifier.height(theme.spacing.dp))

            Text(
                text = feedback.title,
                color = theme.textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FeedbackDetailDescription(
    description: String,
    theme: FeedbackKitTheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = theme.cardBackgroundColor),
        shape = RoundedCornerShape(theme.borderRadius.dp)
    ) {
        Text(
            text = description,
            color = theme.textColor,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            modifier = Modifier.padding(theme.spacing.dp * 2)
        )
    }
}

@Composable
private fun CommentCard(
    comment: Comment,
    theme: FeedbackKitTheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (comment.isOfficial) {
                theme.primaryColor.copy(alpha = 0.1f)
            } else {
                theme.cardBackgroundColor
            }
        ),
        shape = RoundedCornerShape(theme.borderRadius.dp)
    ) {
        Column(
            modifier = Modifier.padding(theme.spacing.dp * 2)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = theme.secondaryTextColor
                )
                Spacer(modifier = Modifier.width(theme.spacing.dp))
                Text(
                    text = comment.userName ?: stringResource(R.string.feedbackkit_anonymous),
                    color = theme.textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (comment.isOfficial) {
                    Spacer(modifier = Modifier.width(theme.spacing.dp))
                    Text(
                        text = stringResource(R.string.feedbackkit_detail_comment_official),
                        color = theme.primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(theme.spacing.dp))

            Text(
                text = comment.content,
                color = theme.textColor,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedbackDetailViewPreview() {
    FeedbackDetailView(
        feedback = Feedback(
            id = "1",
            title = "Add dark mode support",
            description = "It would be great to have a dark mode option for the app to reduce eye strain at night. This is a highly requested feature and would improve the user experience significantly.",
            status = FeedbackStatus.APPROVED,
            category = FeedbackCategory.FEATURE_REQUEST,
            voteCount = 42,
            hasVoted = false,
            commentCount = 5,
            createdAt = "2024-01-15T10:30:00Z"
        ),
        showAppBar = false
    )
}
