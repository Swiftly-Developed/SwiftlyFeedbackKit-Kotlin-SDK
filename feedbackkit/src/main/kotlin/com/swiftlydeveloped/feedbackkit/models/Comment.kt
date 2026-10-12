package com.swiftlydeveloped.feedbackkit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a comment on a feedback item.
 */
@Serializable
data class Comment(
    val id: String,
    // Nullable with a null default: the server's comment payload carries no feedback_id at
    // all, and a required field the server never sends threw MissingFieldException on every
    // comment list until 2026-08-15 (QA-UNIT04-COMMENTS -12, fixed by QA-UNIT10-SDK-PARITY).
    @SerialName("feedback_id")
    val feedbackId: String? = null,
    val content: String,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("user_name")
    val userName: String? = null,
    // The server's wire key is is_admin (CommentResponseDTO.isAdmin under the global
    // .convertToSnakeCase strategy). This read is_official until 2026-08-15, so the admin
    // badge never rendered on Android (QA-UNIT04-COMMENTS -12).
    @SerialName("is_admin")
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
