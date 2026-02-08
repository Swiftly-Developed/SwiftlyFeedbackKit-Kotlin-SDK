package com.swiftlydeveloped.feedbackkit.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.state.LocalFeedbackKitTheme
import com.swiftlydeveloped.feedbackkit.theme.FeedbackKitTheme

/**
 * Badge composable that displays a feedback status with appropriate color.
 *
 * @param status The feedback status to display.
 * @param modifier Modifier for the badge.
 * @param theme Optional theme override.
 */
@Composable
fun StatusBadge(
    status: FeedbackStatus,
    modifier: Modifier = Modifier,
    theme: FeedbackKitTheme = LocalFeedbackKitTheme.current
) {
    val backgroundColor = theme.statusColors.forStatus(status)
    val textColor = Color.White

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor
    ) {
        Text(
            text = status.displayName,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusBadgePreview() {
    StatusBadge(status = FeedbackStatus.PENDING)
}

@Preview(showBackground = true)
@Composable
private fun StatusBadgeApprovedPreview() {
    StatusBadge(status = FeedbackStatus.APPROVED)
}

@Preview(showBackground = true)
@Composable
private fun StatusBadgeInProgressPreview() {
    StatusBadge(status = FeedbackStatus.IN_PROGRESS)
}

@Preview(showBackground = true)
@Composable
private fun StatusBadgeCompletedPreview() {
    StatusBadge(status = FeedbackStatus.COMPLETED)
}
