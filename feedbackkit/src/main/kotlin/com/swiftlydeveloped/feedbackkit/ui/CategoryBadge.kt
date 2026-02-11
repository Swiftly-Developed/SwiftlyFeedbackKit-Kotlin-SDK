package com.swiftlydeveloped.feedbackkit.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.state.LocalFeedbackKitTheme
import com.swiftlydeveloped.feedbackkit.theme.FeedbackKitTheme

/**
 * Badge composable that displays a feedback category with appropriate color.
 *
 * @param category The feedback category to display.
 * @param modifier Modifier for the badge.
 * @param theme Optional theme override.
 */
@Composable
fun CategoryBadge(
    category: FeedbackCategory,
    modifier: Modifier = Modifier,
    theme: FeedbackKitTheme = LocalFeedbackKitTheme.current
) {
    val backgroundColor = theme.categoryColors.forCategory(category)
    val textColor = Color.White

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor
    ) {
        Text(
            text = stringResource(category.displayNameRes),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryBadgeFeatureRequestPreview() {
    CategoryBadge(category = FeedbackCategory.FEATURE_REQUEST)
}

@Preview(showBackground = true)
@Composable
private fun CategoryBadgeBugReportPreview() {
    CategoryBadge(category = FeedbackCategory.BUG_REPORT)
}

@Preview(showBackground = true)
@Composable
private fun CategoryBadgeImprovementPreview() {
    CategoryBadge(category = FeedbackCategory.IMPROVEMENT)
}

@Preview(showBackground = true)
@Composable
private fun CategoryBadgeOtherPreview() {
    CategoryBadge(category = FeedbackCategory.OTHER)
}
