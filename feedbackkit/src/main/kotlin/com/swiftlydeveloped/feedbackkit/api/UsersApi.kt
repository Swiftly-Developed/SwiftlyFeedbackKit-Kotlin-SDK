package com.swiftlydeveloped.feedbackkit.api

import com.swiftlydeveloped.feedbackkit.http.FeedbackKitHttpClient
import com.swiftlydeveloped.feedbackkit.models.RegisterUserRequest
import com.swiftlydeveloped.feedbackkit.models.SdkUser
import kotlinx.serialization.encodeToString

/**
 * API for user operations.
 */
class UsersApi internal constructor(
    private val http: FeedbackKitHttpClient
) {
    /**
     * Register a new SDK user.
     *
     * @param request The user registration request.
     * @return The registered user.
     */
    suspend fun register(request: RegisterUserRequest): SdkUser {
        val body = http.jsonSerializer.encodeToString(request)

        return http.post(
            endpoint = "users/register",
            body = body
        ) { responseBody ->
            http.jsonSerializer.decodeFromString<SdkUser>(responseBody)
        }
    }

    /**
     * Register a new SDK user with individual parameters.
     *
     * @param email Optional email address.
     * @param name Optional display name.
     * @param externalId Optional external ID for linking to your user system.
     * @param metadata Optional additional metadata.
     * @return The registered user.
     */
    suspend fun register(
        email: String? = null,
        name: String? = null,
        externalId: String? = null,
        metadata: Map<String, String>? = null
    ): SdkUser = register(
        RegisterUserRequest(
            email = email,
            name = name,
            externalId = externalId,
            metadata = metadata
        )
    )

    /**
     * Get the current user.
     *
     * @return The current user if a user ID is set.
     */
    suspend fun getCurrentUser(): SdkUser? {
        val userId = http.userId ?: return null

        return http.get(
            endpoint = "users/$userId"
        ) { body ->
            http.jsonSerializer.decodeFromString<SdkUser>(body)
        }
    }
}
