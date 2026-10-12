package com.swiftlydeveloped.feedbackkit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from a vote or unvote operation.
 */
@Serializable
data class VoteResponse(
    /**
     * The server's vote payload has never carried `success`. A successful HTTP
     * response is the success signal; the default keeps older call sites that
     * read this property compiling.
     */
    val success: Boolean = true,
    @SerialName("vote_count")
    val voteCount: Int,
    @SerialName("has_voted")
    val hasVoted: Boolean,
    @SerialName("feedback_id")
    val feedbackId: String? = null
)
