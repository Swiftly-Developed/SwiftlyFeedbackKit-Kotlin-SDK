package com.swiftlydeveloped.feedbackkit.api

import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
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
        val resolvedUserId = request.userId?.takeIf { it.isNotBlank() }
            ?: http.userId?.takeIf { it.isNotBlank() }
            ?: throw FeedbackKitError.ValidationError("User ID is required to register a user")
        val body = http.jsonSerializer.encodeToString(request.copy(userId = resolvedUserId))

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
     * [userId] defaults to the id set on the client. [email], [name], [externalId]
     * and [metadata] are ignored by `POST /users/register`.
     *
     * @param email Ignored by the server. Kept so existing call sites compile.
     * @param name Ignored by the server. Kept so existing call sites compile.
     * @param externalId Ignored by the server. Kept so existing call sites compile.
     * @param metadata Ignored by the server. Kept so existing call sites compile.
     * @param userId SDK user identifier. Sent as `user_id`.
     * @param mrr Optional monthly recurring revenue.
     * @return The registered user.
     */
    @Suppress("DEPRECATION")
    suspend fun register(
        email: String? = null,
        name: String? = null,
        externalId: String? = null,
        metadata: Map<String, String>? = null,
        userId: String? = null,
        mrr: Double? = null
    ): SdkUser = register(
        RegisterUserRequest(
            email = email,
            name = name,
            externalId = externalId,
            metadata = metadata,
            userId = userId,
            mrr = mrr
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
