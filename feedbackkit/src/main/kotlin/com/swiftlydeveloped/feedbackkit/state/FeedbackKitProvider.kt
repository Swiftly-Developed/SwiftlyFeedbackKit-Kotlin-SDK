package com.swiftlydeveloped.feedbackkit.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.swiftlydeveloped.feedbackkit.FeedbackKit
import com.swiftlydeveloped.feedbackkit.theme.FeedbackKitTheme

/**
 * CompositionLocal for accessing the FeedbackKit instance.
 */
val LocalFeedbackKit = staticCompositionLocalOf<FeedbackKit> {
    error("FeedbackKit not provided. Wrap your UI with FeedbackKitProvider.")
}

/**
 * CompositionLocal for accessing the FeedbackKit theme.
 */
val LocalFeedbackKitTheme = staticCompositionLocalOf<FeedbackKitTheme> {
    FeedbackKitTheme.Light
}

/**
 * Provider composable that provides FeedbackKit and theme to the composition tree.
 *
 * Usage:
 * ```kotlin
 * FeedbackKitProvider(
 *     theme = FeedbackKitTheme.Dark
 * ) {
 *     // Your UI that uses FeedbackKit
 *     FeedbackList()
 * }
 * ```
 */
@Composable
fun FeedbackKitProvider(
    feedbackKit: FeedbackKit = FeedbackKit.shared,
    theme: FeedbackKitTheme = FeedbackKitTheme.Light,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalFeedbackKit provides feedbackKit,
        LocalFeedbackKitTheme provides theme,
        content = content
    )
}

/**
 * Provider that automatically uses the FeedbackKit.shared instance.
 *
 * Usage:
 * ```kotlin
 * FeedbackKitProvider {
 *     FeedbackList()
 * }
 * ```
 */
@Composable
fun FeedbackKitProvider(
    theme: FeedbackKitTheme = FeedbackKitTheme.Light,
    content: @Composable () -> Unit
) {
    FeedbackKitProvider(
        feedbackKit = FeedbackKit.shared,
        theme = theme,
        content = content
    )
}
