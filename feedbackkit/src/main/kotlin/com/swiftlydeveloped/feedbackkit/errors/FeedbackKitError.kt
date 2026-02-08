package com.swiftlydeveloped.feedbackkit.errors

/**
 * Base sealed class for all FeedbackKit errors.
 * Using a sealed class allows for exhaustive when expressions and type-safe error handling.
 */
sealed class FeedbackKitError(
    override val message: String,
    val statusCode: Int? = null,
    val code: String? = null
) : Exception(message) {

    /**
     * Authentication error (401 Unauthorized).
     * Thrown when the API key is invalid or missing.
     */
    class AuthenticationError(
        message: String = "Invalid or missing API key"
    ) : FeedbackKitError(message, 401, "UNAUTHORIZED")

    /**
     * Payment required error (402 Payment Required).
     * Thrown when the subscription tier doesn't allow the requested action.
     */
    class PaymentRequiredError(
        message: String = "Subscription upgrade required"
    ) : FeedbackKitError(message, 402, "PAYMENT_REQUIRED")

    /**
     * Forbidden error (403 Forbidden).
     * Thrown when the user doesn't have permission for the requested action.
     */
    class ForbiddenError(
        message: String = "Access denied"
    ) : FeedbackKitError(message, 403, "FORBIDDEN")

    /**
     * Not found error (404 Not Found).
     * Thrown when the requested resource doesn't exist.
     */
    class NotFoundError(
        message: String = "Resource not found"
    ) : FeedbackKitError(message, 404, "NOT_FOUND")

    /**
     * Conflict error (409 Conflict).
     * Thrown when there's a conflict with the current state (e.g., already voted).
     */
    class ConflictError(
        message: String = "Conflict with current state"
    ) : FeedbackKitError(message, 409, "CONFLICT")

    /**
     * Validation error (400 Bad Request).
     * Thrown when the request data is invalid.
     */
    class ValidationError(
        message: String = "Validation failed",
        val fieldErrors: Map<String, String> = emptyMap()
    ) : FeedbackKitError(message, 400, "BAD_REQUEST")

    /**
     * Network error.
     * Thrown when there's a network connectivity issue.
     */
    class NetworkError(
        message: String = "Network error",
        val isTimeout: Boolean = false
    ) : FeedbackKitError(message, 0, if (isTimeout) "TIMEOUT" else "NETWORK_ERROR")

    /**
     * Server error (5xx).
     * Thrown when the server returns an internal error.
     */
    class ServerError(
        message: String = "Internal server error",
        statusCode: Int = 500
    ) : FeedbackKitError(message, statusCode, "SERVER_ERROR")

    /**
     * Unknown error.
     * Thrown for unexpected errors.
     */
    class UnknownError(
        message: String = "An unknown error occurred",
        val underlyingCause: Throwable? = null
    ) : FeedbackKitError(message, null, "UNKNOWN")

    companion object {
        /**
         * Create an appropriate error from an HTTP status code and message.
         */
        fun fromStatusCode(statusCode: Int, message: String): FeedbackKitError = when (statusCode) {
            400 -> ValidationError(message)
            401 -> AuthenticationError(message)
            402 -> PaymentRequiredError(message)
            403 -> ForbiddenError(message)
            404 -> NotFoundError(message)
            409 -> ConflictError(message)
            in 500..599 -> ServerError(message, statusCode)
            else -> UnknownError(message)
        }

        /**
         * Create an error from an exception.
         */
        fun fromException(e: Throwable): FeedbackKitError = when (e) {
            is FeedbackKitError -> e
            is java.net.SocketTimeoutException -> NetworkError("Request timed out", isTimeout = true)
            is java.net.UnknownHostException -> NetworkError("Unable to reach server")
            is java.net.ConnectException -> NetworkError("Connection failed")
            is java.io.IOException -> NetworkError(e.message ?: "Network error")
            else -> UnknownError(e.message ?: "An unknown error occurred", e)
        }
    }
}

/**
 * Extension function to check if an error is recoverable.
 */
val FeedbackKitError.isRecoverable: Boolean
    get() = when (this) {
        is FeedbackKitError.NetworkError -> true
        is FeedbackKitError.ServerError -> true
        else -> false
    }

/**
 * Extension function to get a user-friendly error message.
 */
val FeedbackKitError.userMessage: String
    get() = when (this) {
        is FeedbackKitError.AuthenticationError -> "Authentication failed. Please check your API key."
        is FeedbackKitError.PaymentRequiredError -> "This feature requires a subscription upgrade."
        is FeedbackKitError.ForbiddenError -> "You don't have permission to perform this action."
        is FeedbackKitError.NotFoundError -> "The requested item was not found."
        is FeedbackKitError.ConflictError -> message
        is FeedbackKitError.ValidationError -> message
        is FeedbackKitError.NetworkError -> if (isTimeout) "Request timed out. Please try again." else "Network error. Please check your connection."
        is FeedbackKitError.ServerError -> "Server error. Please try again later."
        is FeedbackKitError.UnknownError -> "An unexpected error occurred."
    }
