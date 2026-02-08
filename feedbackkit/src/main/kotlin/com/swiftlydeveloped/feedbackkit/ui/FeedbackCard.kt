package com.swiftlydeveloped.feedbackkit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.models.VoteResponse
import androidx.compose.material3.ExperimentalMaterial3Api
import com.swiftlydeveloped.feedbackkit.state.LocalFeedbackKitTheme
import com.swiftlydeveloped.feedbackkit.theme.FeedbackKitTheme

/**
 * Card composable that displays a feedback item.
 *
 * @param feedback The feedback item to display.
 * @param modifier Modifier for the card.
 * @param onClick Callback when the card is clicked.
 * @param onVoteChange Callback when the vote state changes.
 * @param theme Optional theme override.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackCard(
    feedback: Feedback,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onVoteChange: ((VoteResponse) -> Unit)? = null,
    theme: FeedbackKitTheme = LocalFeedbackKitTheme.current
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(theme.borderRadius.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.cardBackgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = theme.cardElevation.dp
        ),
        onClick = onClick ?: {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(theme.spacing.dp * 2),
            horizontalArrangement = Arrangement.spacedBy(theme.spacing.dp * 2)
        ) {
            // Vote button
            VoteButton(
                feedback = feedback,
                onVoteChange = onVoteChange,
                theme = theme
            )

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Badges row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(theme.spacing.dp)
                ) {
                    StatusBadge(status = feedback.status, theme = theme)
                    CategoryBadge(category = feedback.category, theme = theme)
                }

                Spacer(modifier = Modifier.height(theme.spacing.dp))

                // Title
                Text(
                    text = feedback.title,
                    color = theme.textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(theme.spacing.dp / 2))

                // Description
                Text(
                    text = feedback.description,
                    color = theme.secondaryTextColor,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(theme.spacing.dp))

                // Footer with comment count
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = theme.secondaryTextColor,
                        modifier = Modifier.height(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = feedback.commentCount.toString(),
                        color = theme.secondaryTextColor,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedbackCardPreview() {
    FeedbackCard(
        feedback = Feedback(
            id = "1",
            title = "Add dark mode support",
            description = "It would be great to have a dark mode option for the app to reduce eye strain at night.",
            status = FeedbackStatus.APPROVED,
            category = FeedbackCategory.FEATURE_REQUEST,
            voteCount = 42,
            hasVoted = false,
            commentCount = 5,
            createdAt = "2024-01-15T10:30:00Z"
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun FeedbackCardVotedPreview() {
    FeedbackCard(
        feedback = Feedback(
            id = "2",
            title = "Fix login bug on iOS",
            description = "Users are experiencing issues logging in after updating to the latest version.",
            status = FeedbackStatus.IN_PROGRESS,
            category = FeedbackCategory.BUG_REPORT,
            voteCount = 128,
            hasVoted = true,
            commentCount = 23,
            createdAt = "2024-01-10T08:00:00Z"
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun FeedbackCardCompletedPreview() {
    FeedbackCard(
        feedback = Feedback(
            id = "3",
            title = "Improve performance on older devices",
            description = "The app runs slowly on older Android phones. Please optimize for better performance.",
            status = FeedbackStatus.COMPLETED,
            category = FeedbackCategory.IMPROVEMENT,
            voteCount = 67,
            hasVoted = false,
            commentCount = 12,
            createdAt = "2024-01-01T12:00:00Z"
        )
    )
}
