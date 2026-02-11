package com.swiftlydeveloped.feedbackkit

/**
 * Configuration for the FeedbackKit SDK.
 */
data class FeedbackKitConfig(
    /**
     * The API key for authentication.
     */
    val apiKey: String,

    /**
     * The base URL for the API.
     */
    val baseUrl: String = Environment.PRODUCTION.baseUrl,

    /**
     * The current user ID (optional, can be set later).
     */
    val userId: String? = null,

    /**
     * Request timeout in milliseconds.
     */
    val timeout: Long = 30_000L,

    /**
     * Whether to enable debug logging.
     */
    val debug: Boolean = false
) {
    /**
     * The full API URL (with /api/v1 suffix).
     */
    val apiUrl: String
        get() = "${baseUrl.trimEnd('/')}/api/v1"

    /**
     * Create a copy with a new user ID.
     */
    fun withUserId(userId: String?): FeedbackKitConfig = copy(userId = userId)

    /**
     * Builder for FeedbackKitConfig.
     */
    class Builder(private val apiKey: String) {
        private var baseUrl: String = Environment.PRODUCTION.baseUrl
        private var userId: String? = null
        private var timeout: Long = 30_000L
        private var debug: Boolean = false

        fun baseUrl(url: String) = apply { this.baseUrl = url }
        fun userId(id: String?) = apply { this.userId = id }
        fun timeout(ms: Long) = apply { this.timeout = ms }
        fun debug(enabled: Boolean) = apply { this.debug = enabled }
        fun environment(env: Environment) = apply { this.baseUrl = env.baseUrl }

        fun build() = FeedbackKitConfig(
            apiKey = apiKey,
            baseUrl = baseUrl,
            userId = userId,
            timeout = timeout,
            debug = debug
        )
    }

    companion object {
        /**
         * Create a builder for FeedbackKitConfig.
         */
        fun builder(apiKey: String) = Builder(apiKey)
    }
}

/**
 * Predefined environments.
 */
enum class Environment(val baseUrl: String) {
    /**
     * Production environment.
     */
    PRODUCTION("https://feedbackkit.swiftly-workspace.com"),

    /**
     * Staging environment.
     */
    STAGING("https://api.feedbackkit.testflight.swiftly-developed.com"),

    /**
     * Local development (Android emulator localhost).
     */
    LOCAL("http://10.0.2.2:8080"),

    /**
     * Local development (direct localhost for physical devices on same network).
     */
    LOCAL_DEVICE("http://localhost:8080")
}
