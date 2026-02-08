package com.swiftlydeveloped.feedbackkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackKitConfigTest {

    @Test
    fun `default config uses production environment`() {
        val config = FeedbackKitConfig(apiKey = "test-key")

        assertEquals("test-key", config.apiKey)
        assertEquals(Environment.PRODUCTION.baseUrl, config.baseUrl)
        assertNull(config.userId)
        assertEquals(30_000L, config.timeout)
        assertEquals(false, config.debug)
    }

    @Test
    fun `apiUrl includes api path`() {
        val config = FeedbackKitConfig(
            apiKey = "test-key",
            baseUrl = "https://example.com"
        )

        assertEquals("https://example.com/api/v1", config.apiUrl)
    }

    @Test
    fun `apiUrl trims trailing slash`() {
        val config = FeedbackKitConfig(
            apiKey = "test-key",
            baseUrl = "https://example.com/"
        )

        assertEquals("https://example.com/api/v1", config.apiUrl)
    }

    @Test
    fun `withUserId creates new config with userId`() {
        val config = FeedbackKitConfig(apiKey = "test-key")
        val updated = config.withUserId("user-123")

        assertEquals("user-123", updated.userId)
        assertNull(config.userId) // Original unchanged
    }

    @Test
    fun `builder creates correct config`() {
        val config = FeedbackKitConfig.builder("test-key")
            .baseUrl("https://custom.com")
            .userId("user-456")
            .timeout(60_000L)
            .debug(true)
            .build()

        assertEquals("test-key", config.apiKey)
        assertEquals("https://custom.com", config.baseUrl)
        assertEquals("user-456", config.userId)
        assertEquals(60_000L, config.timeout)
        assertTrue(config.debug)
    }

    @Test
    fun `builder environment sets baseUrl`() {
        val config = FeedbackKitConfig.builder("test-key")
            .environment(Environment.LOCAL)
            .build()

        assertEquals(Environment.LOCAL.baseUrl, config.baseUrl)
    }

    @Test
    fun `environments have correct urls`() {
        assertEquals("https://api.swiftlyfeedback.com", Environment.PRODUCTION.baseUrl)
        assertEquals("https://staging.swiftlyfeedback.com", Environment.STAGING.baseUrl)
        assertEquals("http://10.0.2.2:8080", Environment.LOCAL.baseUrl)
        assertEquals("http://localhost:8080", Environment.LOCAL_DEVICE.baseUrl)
    }
}
