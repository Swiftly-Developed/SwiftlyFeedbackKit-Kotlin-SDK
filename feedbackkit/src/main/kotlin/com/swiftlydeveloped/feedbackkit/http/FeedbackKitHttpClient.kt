package com.swiftlydeveloped.feedbackkit.http

import com.swiftlydeveloped.feedbackkit.FeedbackKitConfig
import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * HTTP client for making API requests.
 */
class FeedbackKitHttpClient(
    private val config: FeedbackKitConfig
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .readTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .writeTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("X-API-Key", config.apiKey)

            config.userId?.let {
                requestBuilder.addHeader("X-User-Id", it)
            }

            chain.proceed(requestBuilder.build())
        }
        .build()

    /**
     * Current user ID (can be updated after initialization).
     */
    var userId: String? = config.userId
        private set

    /**
     * Update the current user ID.
     */
    fun setUserId(userId: String?) {
        this.userId = userId
    }

    /**
     * Perform a GET request.
     */
    suspend fun <T> get(
        endpoint: String,
        queryParams: Map<String, String> = emptyMap(),
        decoder: (String) -> T
    ): T = withContext(Dispatchers.IO) {
        val url = buildUrl(endpoint, queryParams)

        val request = Request.Builder()
            .url(url)
            .get()
            .apply {
                userId?.let { addHeader("X-User-Id", it) }
            }
            .build()

        executeRequest(request, decoder)
    }

    /**
     * Perform a POST request.
     */
    suspend fun <T> post(
        endpoint: String,
        body: String = "{}",
        decoder: (String) -> T
    ): T = withContext(Dispatchers.IO) {
        val url = buildUrl(endpoint)

        val requestBody = body.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .apply {
                userId?.let { addHeader("X-User-Id", it) }
            }
            .build()

        executeRequest(request, decoder)
    }

    /**
     * Perform a DELETE request.
     */
    suspend fun <T> delete(
        endpoint: String,
        queryParams: Map<String, String> = emptyMap(),
        decoder: (String) -> T
    ): T = withContext(Dispatchers.IO) {
        val url = buildUrl(endpoint, queryParams)

        val request = Request.Builder()
            .url(url)
            .delete()
            .apply {
                userId?.let { addHeader("X-User-Id", it) }
            }
            .build()

        executeRequest(request, decoder)
    }

    /**
     * Perform a PUT request.
     */
    suspend fun <T> put(
        endpoint: String,
        body: String = "{}",
        decoder: (String) -> T
    ): T = withContext(Dispatchers.IO) {
        val url = buildUrl(endpoint)

        val requestBody = body.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .apply {
                userId?.let { addHeader("X-User-Id", it) }
            }
            .build()

        executeRequest(request, decoder)
    }

    private fun buildUrl(endpoint: String, queryParams: Map<String, String> = emptyMap()): String {
        val baseUrl = "${config.apiUrl}/${endpoint.trimStart('/')}"

        if (queryParams.isEmpty()) return baseUrl

        val queryString = queryParams.entries
            .joinToString("&") { (key, value) ->
                "${java.net.URLEncoder.encode(key, "UTF-8")}=${java.net.URLEncoder.encode(value, "UTF-8")}"
            }

        return "$baseUrl?$queryString"
    }

    private fun <T> executeRequest(request: Request, decoder: (String) -> T): T {
        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""

                if (config.debug) {
                    println("FeedbackKit: ${request.method} ${request.url}")
                    println("FeedbackKit: Response ${response.code}: $bodyString")
                }

                if (!response.isSuccessful) {
                    val errorMessage = extractErrorMessage(bodyString) ?: response.message
                    throw FeedbackKitError.fromStatusCode(response.code, errorMessage)
                }

                return decoder(bodyString)
            }
        } catch (e: FeedbackKitError) {
            throw e
        } catch (e: Exception) {
            throw FeedbackKitError.fromException(e)
        }
    }

    private fun extractErrorMessage(body: String): String? {
        return try {
            val jsonElement = json.parseToJsonElement(body)
            val jsonObject = jsonElement.jsonObject

            // Try different common error message fields
            jsonObject["message"]?.jsonPrimitive?.content
                ?: jsonObject["error"]?.jsonPrimitive?.content
                ?: jsonObject["reason"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
    }

    /**
     * JSON serialization instance for use by API classes.
     */
    val jsonSerializer: Json get() = json
}
