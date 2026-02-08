package com.swiftlydeveloped.feedbackkit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val createdAt: String? = null
)

/**
 * Request object for registering a user.
 */
@Serializable
data class RegisterUserRequest(
    val email: String? = null,
    val name: String? = null,
    @SerialName("external_id")
    val externalId: String? = null,
    val metadata: Map<String, String>? = null
)
