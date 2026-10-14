package com.swiftlydeveloped.feedbackkit.errors

import androidx.annotation.StringRes
import com.getfeedbackkit.feedbackkit.R

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
        message: String = "Invalid or missing API key",
        wireCode: String? = null
    ) : FeedbackKitError(message, 401, wireCode ?: "UNAUTHORIZED")

    /**
     * Payment required error (402 Payment Required).
     * Thrown when the subscription tier doesn't allow the requested action.
     */
    class PaymentRequiredError(
        message: String = "Subscription upgrade required",
        wireCode: String? = null
    ) : FeedbackKitError(message, 402, wireCode ?: "PAYMENT_REQUIRED")

    /**
     * Forbidden error (403 Forbidden).
     * Thrown when the user doesn't have permission for the requested action.
     */
    class ForbiddenError(
        message: String = "Access denied",
        wireCode: String? = null
    ) : FeedbackKitError(message, 403, wireCode ?: "FORBIDDEN")

    /**
     * Not found error (404 Not Found).
     * Thrown when the requested resource doesn't exist.
     */
    class NotFoundError(
        message: String = "Resource not found",
        wireCode: String? = null
    ) : FeedbackKitError(message, 404, wireCode ?: "NOT_FOUND")

    /**
     * Conflict error (409 Conflict).
     * Thrown when there's a conflict with the current state (e.g., already voted).
     */
    class ConflictError(
        message: String = "Conflict with current state",
        wireCode: String? = null
    ) : FeedbackKitError(message, 409, wireCode ?: "CONFLICT")

    /**
     * Validation error (400 Bad Request).
     * Thrown when the request data is invalid.
     */
    class ValidationError(
        message: String = "Validation failed",
        val fieldErrors: Map<String, String> = emptyMap(),
        wireCode: String? = null
    ) : FeedbackKitError(message, 400, wireCode ?: "BAD_REQUEST")

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
        statusCode: Int = 500,
        wireCode: String? = null
    ) : FeedbackKitError(message, statusCode, wireCode ?: "SERVER_ERROR")

    /**
     * Rate limit error (429 Too Many Requests).
     *
     * The metrics family is capped at 60 requests per 60 seconds, shared across all four
     * operations. This SDK does not retry or back off: a retry loop would hide the cap.
     *
     * This arrives in the shared-middleware body shape, which carries no machine-readable
     * code, so [retryAfterSeconds] is populated only when a `Retry-After` header was present.
     */
    class RateLimitError(
        message: String = "Rate limit exceeded",
        val retryAfterSeconds: Int? = null,
        wireCode: String? = null
    ) : FeedbackKitError(message, 429, wireCode ?: "RATE_LIMITED")

    /**
     * Unknown error.
     * Thrown for unexpected errors.
     */
    class UnknownError(
        message: String = "An unknown error occurred",
        val underlyingCause: Throwable? = null,
        statusCode: Int? = null
    ) : FeedbackKitError(message, statusCode, "UNKNOWN")

    companion object {
        /**
         * Create an appropriate error from an HTTP status code and message.
         *
         * The status selects the class; [code] and [field] come from the response body when
         * it carried the typed envelope shape.
         *
         * @param statusCode The HTTP status.
         * @param message The message resolved from the body.
         * @param code The server's machine-readable token, when the body carried one. It
         *   OVERRIDES the class-level default, so a 402 can be distinguished from another
         *   402 — branch on this, not on the message text. The shared-middleware body shape
         *   has no code at all, and the model must not invent one: pass null there.
         * @param field The wire spelling of the parameter at fault, when the body named one.
         */
        fun fromStatusCode(
            statusCode: Int,
            message: String,
            code: String? = null,
            field: String? = null,
            retryAfterSeconds: Int? = null
        ): FeedbackKitError = when (statusCode) {
            400 -> ValidationError(
                message,
                fieldErrors = field?.let { mapOf(it to message) } ?: emptyMap(),
                wireCode = code
            )
            401 -> AuthenticationError(message, wireCode = code)
            402 -> PaymentRequiredError(message, wireCode = code)
            403 -> ForbiddenError(message, wireCode = code)
            404 -> NotFoundError(message, wireCode = code)
            409 -> ConflictError(message, wireCode = code)
            429 -> RateLimitError(
                message,
                retryAfterSeconds = retryAfterSeconds,
                wireCode = code
            )
            in 500..599 -> ServerError(message, statusCode, wireCode = code)
            else -> UnknownError(message, statusCode = statusCode)
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
        // Stated explicitly rather than left to the `else`: a rate limit is the most
        // recoverable failure in this vocabulary, and the `else` would silently call it
        // permanent.
        is FeedbackKitError.RateLimitError -> true
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
        is FeedbackKitError.RateLimitError -> "Too many requests. Please try again in a moment."
        is FeedbackKitError.UnknownError -> "An unexpected error occurred."
    }

/**
 * String resource for this error, when the SDK has its own wording.
 *
 * [FeedbackKitError.ValidationError] and [FeedbackKitError.ConflictError] return null:
 * those messages come from the server.
 */
@StringRes
fun FeedbackKitError.messageResourceId(): Int? = when (this) {
    is FeedbackKitError.AuthenticationError -> R.string.feedbackkit_error_auth
    is FeedbackKitError.PaymentRequiredError -> R.string.feedbackkit_error_payment
    is FeedbackKitError.ForbiddenError -> R.string.feedbackkit_error_forbidden
    is FeedbackKitError.NotFoundError -> R.string.feedbackkit_error_not_found
    is FeedbackKitError.NetworkError -> if (isTimeout) {
        R.string.feedbackkit_error_timeout
    } else {
        R.string.feedbackkit_error_network
    }
    is FeedbackKitError.ServerError -> R.string.feedbackkit_error_server
    is FeedbackKitError.RateLimitError -> R.string.feedbackkit_error_rate_limit
    is FeedbackKitError.UnknownError -> R.string.feedbackkit_error_unexpected
    is FeedbackKitError.ValidationError -> null
    is FeedbackKitError.ConflictError -> null
}
