package com.swiftlydeveloped.feedbackkit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Represents an SDK user.
 */
@Serializable
data class SdkUser(
    val id: String,
    val email: String? = null,
    val name: String? = null,
    @SerialName("external_id")
    val externalId: String? = null,
    val metadata: Map<String, String>? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    /** The SDK user id the server stores (`SDKUserResponseDTO.userId`), distinct from row [id]. */
    @SerialName("user_id")
    val userId: String? = null,
    val mrr: Double? = null,
    @SerialName("first_seen_at")
    val firstSeenAt: String? = null,
    @SerialName("last_seen_at")
    val lastSeenAt: String? = null
)

/**
 * Request object for registering a user.
 *
 * `POST /users/register` decodes [userId] (`user_id` on the wire) and an optional
 * [mrr]. [email], [name], [externalId] and [metadata] are kept so existing call
 * sites still compile; the server ignores them, and they are not serialized.
 */
@Serializable
data class RegisterUserRequest(
    @Deprecated("The server ignores email. POST /users/register accepts userId and an optional mrr only.")
    @Transient
    val email: String? = null,
    @Deprecated("The server ignores name. POST /users/register accepts userId and an optional mrr only.")
    @Transient
    val name: String? = null,
    @Deprecated("The server ignores externalId. POST /users/register accepts userId and an optional mrr only.")
    @Transient
    @SerialName("external_id")
    val externalId: String? = null,
    @Deprecated("The server ignores metadata. POST /users/register accepts userId and an optional mrr only.")
    @Transient
    val metadata: Map<String, String>? = null,
    @SerialName("user_id")
    val userId: String? = null,
    val mrr: Double? = null
)
