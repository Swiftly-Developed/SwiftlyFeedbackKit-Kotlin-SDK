package com.swiftlydeveloped.feedbackkit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from a vote or unvote operation.
 */
@Serializable
data class VoteResponse(
    val success: Boolean,
    @SerialName("vote_count")
    val voteCount: Int,
    @SerialName("has_voted")
    val hasVoted: Boolean
)
