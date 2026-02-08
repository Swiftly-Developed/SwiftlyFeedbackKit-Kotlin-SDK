package com.swiftlydeveloped.feedbackkit

import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.errors.isRecoverable
import com.swiftlydeveloped.feedbackkit.errors.userMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class FeedbackKitErrorTest {

    @Test
    fun `fromStatusCode creates correct error types`() {
        assertTrue(FeedbackKitError.fromStatusCode(400, "Bad request") is FeedbackKitError.ValidationError)
        assertTrue(FeedbackKitError.fromStatusCode(401, "Unauthorized") is FeedbackKitError.AuthenticationError)
        assertTrue(FeedbackKitError.fromStatusCode(402, "Payment required") is FeedbackKitError.PaymentRequiredError)
        assertTrue(FeedbackKitError.fromStatusCode(403, "Forbidden") is FeedbackKitError.ForbiddenError)
        assertTrue(FeedbackKitError.fromStatusCode(404, "Not found") is FeedbackKitError.NotFoundError)
        assertTrue(FeedbackKitError.fromStatusCode(409, "Conflict") is FeedbackKitError.ConflictError)
        assertTrue(FeedbackKitError.fromStatusCode(500, "Server error") is FeedbackKitError.ServerError)
        assertTrue(FeedbackKitError.fromStatusCode(502, "Bad gateway") is FeedbackKitError.ServerError)
        assertTrue(FeedbackKitError.fromStatusCode(999, "Unknown") is FeedbackKitError.UnknownError)
    }

    @Test
    fun `fromException creates NetworkError for timeout`() {
        val error = FeedbackKitError.fromException(SocketTimeoutException())
        assertTrue(error is FeedbackKitError.NetworkError)
        assertTrue((error as FeedbackKitError.NetworkError).isTimeout)
    }

    @Test
    fun `fromException creates NetworkError for unknown host`() {
        val error = FeedbackKitError.fromException(UnknownHostException())
        assertTrue(error is FeedbackKitError.NetworkError)
        assertFalse((error as FeedbackKitError.NetworkError).isTimeout)
    }

    @Test
    fun `fromException returns same error if already FeedbackKitError`() {
        val original = FeedbackKitError.NotFoundError("Test not found")
        val result = FeedbackKitError.fromException(original)
        assertEquals(original, result)
    }

    @Test
    fun `fromException creates UnknownError for other exceptions`() {
        val error = FeedbackKitError.fromException(IllegalArgumentException("test"))
        assertTrue(error is FeedbackKitError.UnknownError)
    }

    @Test
    fun `error codes are correct`() {
        assertEquals("UNAUTHORIZED", FeedbackKitError.AuthenticationError().code)
        assertEquals("PAYMENT_REQUIRED", FeedbackKitError.PaymentRequiredError().code)
        assertEquals("FORBIDDEN", FeedbackKitError.ForbiddenError().code)
        assertEquals("NOT_FOUND", FeedbackKitError.NotFoundError().code)
        assertEquals("CONFLICT", FeedbackKitError.ConflictError().code)
        assertEquals("BAD_REQUEST", FeedbackKitError.ValidationError().code)
        assertEquals("NETWORK_ERROR", FeedbackKitError.NetworkError().code)
        assertEquals("TIMEOUT", FeedbackKitError.NetworkError(isTimeout = true).code)
        assertEquals("SERVER_ERROR", FeedbackKitError.ServerError().code)
        assertEquals("UNKNOWN", FeedbackKitError.UnknownError().code)
    }

    @Test
    fun `status codes are correct`() {
        assertEquals(401, FeedbackKitError.AuthenticationError().statusCode)
        assertEquals(402, FeedbackKitError.PaymentRequiredError().statusCode)
        assertEquals(403, FeedbackKitError.ForbiddenError().statusCode)
        assertEquals(404, FeedbackKitError.NotFoundError().statusCode)
        assertEquals(409, FeedbackKitError.ConflictError().statusCode)
        assertEquals(400, FeedbackKitError.ValidationError().statusCode)
        assertEquals(0, FeedbackKitError.NetworkError().statusCode)
        assertEquals(500, FeedbackKitError.ServerError().statusCode)
    }

    @Test
    fun `isRecoverable is correct`() {
        assertFalse(FeedbackKitError.AuthenticationError().isRecoverable)
        assertFalse(FeedbackKitError.PaymentRequiredError().isRecoverable)
        assertFalse(FeedbackKitError.ForbiddenError().isRecoverable)
        assertFalse(FeedbackKitError.NotFoundError().isRecoverable)
        assertFalse(FeedbackKitError.ConflictError().isRecoverable)
        assertFalse(FeedbackKitError.ValidationError().isRecoverable)
        assertTrue(FeedbackKitError.NetworkError().isRecoverable)
        assertTrue(FeedbackKitError.ServerError().isRecoverable)
    }

    @Test
    fun `userMessage returns user-friendly messages`() {
        assertEquals(
            "Authentication failed. Please check your API key.",
            FeedbackKitError.AuthenticationError().userMessage
        )
        assertEquals(
            "This feature requires a subscription upgrade.",
            FeedbackKitError.PaymentRequiredError().userMessage
        )
        assertEquals(
            "You don't have permission to perform this action.",
            FeedbackKitError.ForbiddenError().userMessage
        )
        assertEquals(
            "The requested item was not found.",
            FeedbackKitError.NotFoundError().userMessage
        )
        assertEquals(
            "Request timed out. Please try again.",
            FeedbackKitError.NetworkError(isTimeout = true).userMessage
        )
        assertEquals(
            "Network error. Please check your connection.",
            FeedbackKitError.NetworkError().userMessage
        )
        assertEquals(
            "Server error. Please try again later.",
            FeedbackKitError.ServerError().userMessage
        )
    }
}
