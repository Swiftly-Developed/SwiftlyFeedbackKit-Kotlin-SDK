package com.swiftlydeveloped.feedbackkit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a comment on a feedback item.
 */
@Serializable
data class Comment(
    val id: String,
    @SerialName("feedback_id")
    val feedbackId: String,
    val content: String,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("user_name")
    val userName: String? = null,
    @SerialName("is_official")
    val isOfficial: Boolean = false,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Request object for creating a comment.
 */
@Serializable
data class CreateCommentRequest(
    val content: String,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("user_name")
    val userName: String? = null
)
