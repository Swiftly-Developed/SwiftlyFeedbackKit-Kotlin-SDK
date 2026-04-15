package com.swiftlydeveloped.feedbackkit

import android.content.Context
import com.swiftlydeveloped.feedbackkit.api.CommentsApi
import com.swiftlydeveloped.feedbackkit.api.EventsApi
import com.swiftlydeveloped.feedbackkit.api.FeedbackApi
import com.swiftlydeveloped.feedbackkit.api.UsersApi
import com.swiftlydeveloped.feedbackkit.api.VotesApi
import com.swiftlydeveloped.feedbackkit.http.FeedbackKitHttpClient
import com.swiftlydeveloped.feedbackkit.storage.FeedbackKitStorage

/**
 * Main entry point for the FeedbackKit SDK.
 *
 * Usage:
 * ```kotlin
 * // Configure once at app startup
 * FeedbackKit.configure(context) {
 *     apiKey = "your-api-key"
 *     userId = "optional-user-id"
 * }
 *
 * // Use the SDK
 * val feedbackList = FeedbackKit.shared.feedback.list()
 * ```
 */
class FeedbackKit private constructor(
    config: FeedbackKitConfig,
    context: Context
) {
    private val http = FeedbackKitHttpClient(config)

    /**
     * The current configuration.
     */
    val config: FeedbackKitConfig = config

    /**
     * Persistent storage for user data.
     */
    val storage: FeedbackKitStorage = FeedbackKitStorage(context)

    /**
     * API for feedback operations.
     */
    val feedback: FeedbackApi = FeedbackApi(http)

    /**
     * API for vote operations.
     */
    val votes: VotesApi = VotesApi(http)

    /**
     * API for comment operations.
     */
    val comments: CommentsApi = CommentsApi(http)

    /**
     * API for user operations.
     */
    val users: UsersApi = UsersApi(http)

    /**
     * API for event tracking operations.
     */
    val events: EventsApi = EventsApi(http)

    /**
     * The current user ID.
     */
    var userId: String?
        get() = http.userId
        set(value) {
            http.setUserId(value)
        }

    /**
     * Set the user ID and persist it to storage.
     */
    suspend fun setUserIdAndPersist(userId: String?) {
        http.setUserId(userId)
        storage.setUserId(userId)
    }

    /**
     * Load the user ID from persistent storage.
     */
    suspend fun loadUserIdFromStorage() {
        val storedUserId = storage.getUserId()
        if (storedUserId != null) {
            http.setUserId(storedUserId)
        }
    }

    /**
     * Clear the current user and all persisted data.
     */
    suspend fun logout() {
        http.setUserId(null)
        storage.clear()
    }

    companion object {
        @Volatile
        private var instance: FeedbackKit? = null

        /**
         * Get the shared FeedbackKit instance.
         *
         * @throws IllegalStateException if configure() has not been called.
         */
        val shared: FeedbackKit
            get() = instance ?: throw IllegalStateException(
                "FeedbackKit has not been configured. Call FeedbackKit.configure() first."
            )

        /**
         * Check if FeedbackKit has been configured.
         */
        val isConfigured: Boolean
            get() = instance != null

        /**
         * Configure the FeedbackKit SDK.
         *
         * @param context Application context.
         * @param config The configuration to use.
         * @return The configured FeedbackKit instance.
         */
        @Synchronized
        fun configure(context: Context, config: FeedbackKitConfig): FeedbackKit {
            return instance ?: FeedbackKit(
                config = config,
                context = context.applicationContext
            ).also { instance = it }
        }

        /**
         * Configure the FeedbackKit SDK using a builder DSL.
         *
         * @param context Application context.
         * @param apiKey The API key for authentication.
         * @param builder Configuration builder lambda.
         * @return The configured FeedbackKit instance.
         */
        @Synchronized
        fun configure(
            context: Context,
            apiKey: String,
            builder: FeedbackKitConfig.Builder.() -> Unit = {}
        ): FeedbackKit {
            val config = FeedbackKitConfig.builder(apiKey).apply(builder).build()
            return configure(context, config)
        }

        /**
         * Reset the SDK instance (primarily for testing).
         */
        @Synchronized
        internal fun reset() {
            instance = null
        }
    }
}

/**
 * DSL builder for configuring FeedbackKit.
 *
 * Usage:
 * ```kotlin
 * FeedbackKit.configure(context) {
 *     apiKey = "your-api-key"
 *     environment = Environment.PRODUCTION
 *     userId = "optional-user-id"
 *     debug = true
 * }
 * ```
 */
class FeedbackKitBuilder {
    var apiKey: String = ""
    var baseUrl: String = Environment.PRODUCTION.baseUrl
    var userId: String? = null
    var timeout: Long = 30_000L
    var debug: Boolean = false
    var environment: Environment = Environment.PRODUCTION
        set(value) {
            field = value
            baseUrl = value.baseUrl
        }

    internal fun build(): FeedbackKitConfig {
        require(apiKey.isNotBlank()) { "API key is required" }
        return FeedbackKitConfig(
            apiKey = apiKey,
            baseUrl = baseUrl,
            userId = userId,
            timeout = timeout,
            debug = debug
        )
    }
}

/**
 * Configure FeedbackKit using a DSL builder.
 */
fun FeedbackKit.Companion.configure(
    context: Context,
    builder: FeedbackKitBuilder.() -> Unit
): FeedbackKit {
    val config = FeedbackKitBuilder().apply(builder).build()
    return configure(context, config)
}
