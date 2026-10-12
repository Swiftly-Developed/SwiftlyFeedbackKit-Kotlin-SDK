package com.swiftlydeveloped.feedbackkit.http

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugLogRedactionTest {

    @Test
    fun queryValuesIncludingEventNamesAreNotInTheDebugTarget() {
        val url = "https://api.example.com/api/v1/metrics/events?event_name=checkout_completed&days=30".toHttpUrl()

        val target = FeedbackKitHttpClient.redactedDebugTarget(url)

        assertEquals("/api/v1/metrics/events?event_name=…&days=…", target)
        assertFalse(target.contains("checkout_completed"))
        assertFalse(target.contains("api.example.com"))
        assertFalse(target.contains("30"))
        assertTrue(target.contains("event_name=…"))
    }

    @Test
    fun aUrlWithoutAQueryIsJustThePath() {
        val url = "https://api.example.com/api/v1/feedbacks".toHttpUrl()

        assertEquals("/api/v1/feedbacks", FeedbackKitHttpClient.redactedDebugTarget(url))
    }
}
