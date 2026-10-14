package com.swiftlydeveloped.feedbackkit

import android.content.Context
import com.swiftlydeveloped.feedbackkit.api.CommentsApi
import com.swiftlydeveloped.feedbackkit.api.EventsApi
import com.swiftlydeveloped.feedbackkit.api.FeedbackApi
import com.swiftlydeveloped.feedbackkit.api.MetricsApi
import com.swiftlydeveloped.feedbackkit.api.UsersApi
import com.swiftlydeveloped.feedbackkit.api.VotesApi
import com.swiftlydeveloped.feedbackkit.http.FeedbackKitHttpClient
import com.swiftlydeveloped.feedbackkit.storage.FeedbackKitStorage
import com.swiftlydeveloped.feedbackkit.storage.SharedPreferencesUserIdStore
import com.swiftlydeveloped.feedbackkit.storage.readDataStoreUserId
import com.swiftlydeveloped.feedbackkit.storage.resolveAnonymousUserId

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
    context: Context,
    private val userIdStore: SharedPreferencesUserIdStore
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
     * API for reading this project's metrics.
     *
     * A project below the Pro plan reads metrics too — the response is narrowed rather than
     * refused, and says so through `meta.eventScope` and `meta.retentionClamped`.
     */
    val metrics: MetricsApi = MetricsApi(http)

    /**
     * The current user ID. Assignment trims, persists in SharedPreferences,
     * and a blank value clears the stored id. It does not generate a new one;
     * the next [configure] after [reset] does that when nothing is stored.
     */
    var userId: String?
        get() = http.userId
        set(value) {
            val trimmed = value?.trim()?.takeIf { it.isNotEmpty() }
            http.setUserId(trimmed)
            userIdStore.set(trimmed)
        }

    /**
     * Set the user ID and persist it to SharedPreferences and DataStore.
     */
    suspend fun setUserIdAndPersist(userId: String?) {
        this.userId = userId
        storage.setUserId(this.userId)
    }

    /**
     * Load the user ID from persistent storage.
     *
     * SharedPreferences wins. DataStore is read only when that store has no id,
     * and a value found there is copied across.
     */
    suspend fun loadUserIdFromStorage() {
        val fromPreferences = userIdStore.get()
        if (fromPreferences != null) {
            http.setUserId(fromPreferences)
            return
        }
        val storedUserId = storage.getUserId()
        if (storedUserId != null) {
            http.setUserId(storedUserId)
            userIdStore.set(storedUserId)
        }
    }

    /**
     * Clear the current user and all persisted data.
     */
    suspend fun logout() {
        userId = null
        storage.clear()
    }

    companion object {
        @Volatile
        private var instance: FeedbackKit? = null

        /**
         * User id an older build stored in DataStore. Production reads that store.
         * Tests replace this so a mock [Context] does not open DataStore.
         */
        internal val defaultLegacyUserIdReader: (Context) -> String? = { context ->
            readDataStoreUserId(context)
        }

        internal var legacyUserIdReader: (Context) -> String? = defaultLegacyUserIdReader

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
            instance?.let { return it }
            val appContext = context.applicationContext
            val store = SharedPreferencesUserIdStore.from(appContext)
            val preferencesId = store.get()
            // DataStore is the pre-SharedPreferences home of this id. Read it only
            // when nothing else named the user, and copy it across so the next
            // launch does not mint a new one.
            val legacyId = if (config.userId.isNullOrBlank() && preferencesId == null) {
                legacyUserIdReader(appContext)
            } else {
                null
            }
            val resolved = resolveAnonymousUserId(
                configured = config.userId,
                stored = preferencesId ?: legacyId
            )
            if (store.get() != resolved) {
                store.set(resolved)
            }
            return FeedbackKit(
                config = config.withUserId(resolved),
                context = appContext,
                userIdStore = store
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
