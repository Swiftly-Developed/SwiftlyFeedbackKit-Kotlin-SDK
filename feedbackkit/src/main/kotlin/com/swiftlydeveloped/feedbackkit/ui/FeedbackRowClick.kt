package com.swiftlydeveloped.feedbackkit.ui

import com.swiftlydeveloped.feedbackkit.models.Feedback

/**
 * Click handler for a feedback row.
 *
 * A missing callback stays null so the card is not announced as a button.
 */
internal fun feedbackRowClick(
    onFeedbackClick: ((Feedback) -> Unit)?,
    feedback: Feedback
): (() -> Unit)? = onFeedbackClick?.let { callback -> { callback(feedback) } }
