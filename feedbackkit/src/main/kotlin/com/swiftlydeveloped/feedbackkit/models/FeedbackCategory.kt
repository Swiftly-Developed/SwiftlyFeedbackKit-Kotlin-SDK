package com.swiftlydeveloped.feedbackkit.models

import androidx.annotation.StringRes
import com.swiftlydeveloped.feedbackkit.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the category of a feedback item.
 */
@Serializable
enum class FeedbackCategory {
    @SerialName("feature_request")
    FEATURE_REQUEST,

    @SerialName("bug_report")
    BUG_REPORT,

    @SerialName("improvement")
    IMPROVEMENT,

    @SerialName("other")
    OTHER;

    /**
     * String resource ID for the localized display name.
     */
    @get:StringRes
    val displayNameRes: Int
        get() = when (this) {
            FEATURE_REQUEST -> R.string.feedbackkit_category_feature_request
            BUG_REPORT -> R.string.feedbackkit_category_bug_report
            IMPROVEMENT -> R.string.feedbackkit_category_improvement
            OTHER -> R.string.feedbackkit_category_other
        }

    /**
     * Human-readable display name for the category (English fallback).
     */
    val displayName: String
        get() = when (this) {
            FEATURE_REQUEST -> "Feature Request"
            BUG_REPORT -> "Bug Report"
            IMPROVEMENT -> "Improvement"
            OTHER -> "Other"
        }

    /**
     * The JSON value for this category.
     */
    val jsonValue: String
        get() = when (this) {
            FEATURE_REQUEST -> "feature_request"
            BUG_REPORT -> "bug_report"
            IMPROVEMENT -> "improvement"
            OTHER -> "other"
        }

    companion object {
        /**
         * Parse a category from its JSON string value.
         */
        fun fromJsonValue(value: String): FeedbackCategory? = when (value.lowercase()) {
            "feature_request", "featurerequest" -> FEATURE_REQUEST
            "bug_report", "bugreport" -> BUG_REPORT
            "improvement" -> IMPROVEMENT
            "other" -> OTHER
            else -> null
        }
    }
}
