package com.swiftlydeveloped.feedbackkit.errors

import com.getfeedbackkit.feedbackkit.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ErrorMessageResourceTest {

    @Test
    fun `typed errors map to string resources and server messages stay on the error`() {
        assertEquals(
            R.string.feedbackkit_error_auth,
            FeedbackKitError.AuthenticationError().messageResourceId()
        )
        assertEquals(
            R.string.feedbackkit_error_payment,
            FeedbackKitError.PaymentRequiredError().messageResourceId()
        )
        assertEquals(
            R.string.feedbackkit_error_forbidden,
            FeedbackKitError.ForbiddenError().messageResourceId()
        )
        assertEquals(
            R.string.feedbackkit_error_not_found,
            FeedbackKitError.NotFoundError().messageResourceId()
        )
        assertEquals(
            R.string.feedbackkit_error_timeout,
            FeedbackKitError.NetworkError(isTimeout = true).messageResourceId()
        )
        assertEquals(
            R.string.feedbackkit_error_network,
            FeedbackKitError.NetworkError().messageResourceId()
        )
        assertEquals(
            R.string.feedbackkit_error_server,
            FeedbackKitError.ServerError().messageResourceId()
        )
        assertEquals(
            R.string.feedbackkit_error_rate_limit,
            FeedbackKitError.RateLimitError().messageResourceId()
        )
        assertEquals(
            R.string.feedbackkit_error_unexpected,
            FeedbackKitError.UnknownError().messageResourceId()
        )
        assertNull(FeedbackKitError.ValidationError("Title is required").messageResourceId())
        assertNull(FeedbackKitError.ConflictError("Already voted").messageResourceId())
    }
}
