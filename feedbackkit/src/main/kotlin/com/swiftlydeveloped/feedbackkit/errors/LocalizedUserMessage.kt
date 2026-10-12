package com.swiftlydeveloped.feedbackkit.errors

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * User-facing message for Compose, resolved from the SDK string catalogs.
 *
 * [userMessage] stays the English wording for callers that are not composables.
 */
@Composable
fun FeedbackKitError.localizedUserMessage(): String {
    val resource = messageResourceId()
    return if (resource != null) stringResource(resource) else message
}
