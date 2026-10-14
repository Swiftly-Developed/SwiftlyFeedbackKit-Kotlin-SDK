package com.swiftlydeveloped.feedbackkit.ui.showcase

/**
 * How the Showcase vote capsule renders (FEEDBACK01 §6.4). One resolver decides it so a unit
 * test can pin every cell, and so a closed request can never resolve to a button.
 *
 * The Kotlin SDK has no `showVoteCount` or `allowUndoVote` flag, so the table's "absent" and
 * "voted, undo not allowed" cells have no counterpart here.
 */
internal sealed interface VotePresentation {
    /** Skeleton: a grey capsule of the same size, disabled and hidden from accessibility. */
    data object Placeholder : VotePresentation

    /** Completed or rejected: plain arrow and count, not a control. */
    data object Closed : VotePresentation

    /** A button. Tonal while [voted] is false, filled while it is true. */
    data class Votable(val voted: Boolean) : VotePresentation
}

internal fun resolveVotePresentation(
    isPlaceholder: Boolean,
    canVote: Boolean,
    hasVoted: Boolean
): VotePresentation = when {
    isPlaceholder -> VotePresentation.Placeholder
    !canVote -> VotePresentation.Closed
    else -> VotePresentation.Votable(voted = hasVoted)
}
