package com.swiftlydeveloped.feedbackkit.models

/**
 * The three event names FeedbackKit itself defines.
 *
 * Every other name you track is a **custom** event. The distinction is not cosmetic: below
 * the Pro plan a project's custom events are ingested and retained but not read back, and
 * `MetricsMeta.eventScope` reports [MetricEventScope.PREDEFINED_ONLY] to say so. Only these
 * three names count on every plan.
 *
 * The match is **exact and case-sensitive** — `Feedback_List` is a custom event, not this
 * one. That is why they ship as constants rather than being spelled by hand at each call
 * site, and why this object is their single definition in the SDK.
 *
 * ```kotlin
 * FeedbackKit.shared.events.track(PredefinedEventNames.FEEDBACK_LIST)
 * ```
 */
object PredefinedEventNames {
    /** The customer's user opened the feedback list. */
    const val FEEDBACK_LIST = "feedback_list"

    /** The customer's user opened a feedback item's detail view. */
    const val FEEDBACK_DETAIL = "feedback_detail"

    /** The customer's user opened the submit-feedback form. */
    const val SUBMIT_FEEDBACK = "submit_feedback"

    /** All three, in the order the contract lists them. */
    val ALL: List<String> = listOf(FEEDBACK_LIST, FEEDBACK_DETAIL, SUBMIT_FEEDBACK)
}
