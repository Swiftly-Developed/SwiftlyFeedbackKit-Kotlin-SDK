package com.swiftlydeveloped.feedbackkit.ui

/**
 * How [FeedbackList] and [FeedbackListScreen] render each feedback item.
 *
 * The style selects a whole look. It is not a set of individual metrics.
 */
enum class FeedbackListStyle {
    /**
     * Default since 2.1.0. One Material 3 elevated card per request: a status eyebrow, the
     * full title and description, and a footer with the category, the comment count and a
     * vote button. Colours come from the host's `MaterialTheme`, so dynamic colour and dark
     * theme apply. Status colours come from [com.swiftlydeveloped.feedbackkit.theme.FeedbackKitTheme.statusColors].
     */
    SHOWCASE,

    /**
     * The list as it rendered before 2.1.0: [FeedbackCard] rows with the square vote control
     * on the leading side, coloured by [com.swiftlydeveloped.feedbackkit.theme.FeedbackKitTheme].
     * Supported for the whole 2.x line.
     */
    CLASSIC
}
