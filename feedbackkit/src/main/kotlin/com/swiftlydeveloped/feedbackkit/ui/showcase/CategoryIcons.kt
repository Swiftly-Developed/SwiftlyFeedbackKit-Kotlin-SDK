package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory

/**
 * Category glyph for the Showcase footer and the category filter chips. Improvement uses
 * Material "tune" (FEEDBACK01 §12.5): an arrow reads as an upvote next to the vote capsule.
 */
internal val FeedbackCategory.icon: ImageVector
    get() = when (this) {
        FeedbackCategory.FEATURE_REQUEST -> Icons.Outlined.Lightbulb
        FeedbackCategory.BUG_REPORT -> Icons.Outlined.BugReport
        FeedbackCategory.IMPROVEMENT -> Icons.Outlined.Tune
        FeedbackCategory.OTHER -> Icons.Outlined.MoreHoriz
    }
