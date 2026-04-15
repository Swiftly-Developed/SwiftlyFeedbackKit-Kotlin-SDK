package com.swiftlydeveloped.feedbackkit.theme

import androidx.compose.ui.graphics.Color
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus

/**
 * Color scheme for feedback statuses.
 */
data class StatusColors(
    val pending: Color = Color(0xFF9CA3AF),      // Gray
    val approved: Color = Color(0xFF3B82F6),     // Blue
    val inProgress: Color = Color(0xFFF97316),   // Orange
    val testflight: Color = Color(0xFF06B6D4),   // Cyan
    val completed: Color = Color(0xFF22C55E),    // Green
    val rejected: Color = Color(0xFFEF4444)      // Red
) {
    /**
     * Get the color for a specific status.
     */
    fun forStatus(status: FeedbackStatus): Color = when (status) {
        FeedbackStatus.PENDING -> pending
        FeedbackStatus.APPROVED -> approved
        FeedbackStatus.IN_PROGRESS -> inProgress
        FeedbackStatus.TESTFLIGHT -> testflight
        FeedbackStatus.COMPLETED -> completed
        FeedbackStatus.REJECTED -> rejected
    }
}

/**
 * Color scheme for feedback categories.
 */
data class CategoryColors(
    val featureRequest: Color = Color(0xFF8B5CF6), // Purple
    val bugReport: Color = Color(0xFFEF4444),      // Red
    val improvement: Color = Color(0xFF3B82F6),    // Blue
    val other: Color = Color(0xFF9CA3AF)           // Gray
) {
    /**
     * Get the color for a specific category.
     */
    fun forCategory(category: FeedbackCategory): Color = when (category) {
        FeedbackCategory.FEATURE_REQUEST -> featureRequest
        FeedbackCategory.BUG_REPORT -> bugReport
        FeedbackCategory.IMPROVEMENT -> improvement
        FeedbackCategory.OTHER -> other
    }
}

/**
 * Complete theme configuration for FeedbackKit UI components.
 */
data class FeedbackKitTheme(
    // Primary colors
    val primaryColor: Color = Color(0xFF3B82F6),
    val backgroundColor: Color = Color(0xFFFFFFFF),
    val cardBackgroundColor: Color = Color(0xFFF9FAFB),

    // Text colors
    val textColor: Color = Color(0xFF1F2937),
    val secondaryTextColor: Color = Color(0xFF6B7280),

    // State colors
    val borderColor: Color = Color(0xFFE5E7EB),
    val errorColor: Color = Color(0xFFEF4444),
    val successColor: Color = Color(0xFF22C55E),

    // Vote button colors
    val voteActiveColor: Color = Color(0xFF3B82F6),
    val voteInactiveColor: Color = Color(0xFF9CA3AF),

    // Status and category colors
    val statusColors: StatusColors = StatusColors(),
    val categoryColors: CategoryColors = CategoryColors(),

    // Dimensions
    val borderRadius: Float = 12f,
    val spacing: Float = 8f,
    val cardElevation: Float = 2f
) {
    companion object {
        /**
         * Light theme preset.
         */
        val Light = FeedbackKitTheme()

        /**
         * Dark theme preset.
         */
        val Dark = FeedbackKitTheme(
            primaryColor = Color(0xFF60A5FA),
            backgroundColor = Color(0xFF111827),
            cardBackgroundColor = Color(0xFF1F2937),
            textColor = Color(0xFFF9FAFB),
            secondaryTextColor = Color(0xFF9CA3AF),
            borderColor = Color(0xFF374151),
            voteActiveColor = Color(0xFF60A5FA),
            voteInactiveColor = Color(0xFF6B7280)
        )
    }

    /**
     * Merge this theme with another, using the other's values.
     */
    fun merge(other: FeedbackKitTheme): FeedbackKitTheme = other
}
