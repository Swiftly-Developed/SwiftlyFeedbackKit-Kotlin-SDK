package com.swiftlydeveloped.feedbackkit.http

import com.swiftlydeveloped.feedbackkit.FeedbackKitConfig
import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
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

    /**
     * Current user ID (can be updated after initialization).
     * The request interceptor reads this property, not the construction-time config.
     */
    var userId: String? = config.userId
        private set

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

            userId?.let {
                requestBuilder.addHeader("X-User-Id", it)
            }

            chain.proceed(requestBuilder.build())
        }
        .build()

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
        body: String? = null,
        decoder: (String) -> T
    ): T = withContext(Dispatchers.IO) {
        val url = buildUrl(endpoint, queryParams)

        val request = Request.Builder()
            .url(url)
            .apply {
                if (body != null) {
                    delete(body.toRequestBody("application/json".toMediaType()))
                } else {
                    delete()
                }
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
                    // Path and query *names* only. Query values carry customer event
                    // names, and the body can carry them too. Neither is logged.
                    println("FeedbackKit: ${request.method} ${redactedDebugTarget(request.url)}")
                    println("FeedbackKit: Response ${response.code} (${bodyString.length} bytes)")
                }

                if (!response.isSuccessful) {
                    val details = parseErrorBody(json, bodyString)
                    throw FeedbackKitError.fromStatusCode(
                        statusCode = response.code,
                        message = details.message ?: response.message,
                        code = details.code,
                        field = details.field,
                        retryAfterSeconds = retryAfterSeconds(response.header("Retry-After"))
                    )
                }

                return decoder(bodyString)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FeedbackKitError) {
            throw e
        } catch (e: Exception) {
            throw FeedbackKitError.fromException(e)
        }
    }

    companion object {
        /**
         * `Retry-After` as delta-seconds. An HTTP-date value is left unset rather
         * than parsed into a guessed delay.
         */
        internal fun retryAfterSeconds(headerValue: String?): Int? {
            val value = headerValue?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return value.toIntOrNull()?.takeIf { it >= 0 }
        }

        /**
         * Debug-log target: encoded path plus query names, never query values and never
         * the host. Metrics reads put the customer's event name in `event_name`.
         */
        internal fun redactedDebugTarget(url: HttpUrl): String {
            val path = url.encodedPath
            if (url.querySize == 0) return path
            val query = buildString {
                for (index in 0 until url.querySize) {
                    if (index > 0) append('&')
                    append(url.queryParameterName(index))
                    append("=…")
                }
            }
            return "$path?$query"
        }

        /**
         * What an error body yielded. Any field may be null when the body did not carry it.
         */
        internal data class ErrorBodyDetails(
            val message: String?,
            val code: String?,
            val field: String?
        )

        /**
         * Read either of the API's two error body shapes.
         *
         * Shape A — the typed API envelope — is
         * `{"error": {"code", "message", "field"?}, "timestamp"}`.
         * Shape B — the shared middleware body — is `{"error": true, "reason": "..."}`, and it
         * carries no machine-readable code at all.
         *
         * ⚠️ Branch on the JSON **type** of `error`, never on the status code: a 403 arrives in
         * BOTH shapes. A role or suspension denial is shape B with no code; a measure rejection
         * is shape A carrying a code. Only the body distinguishes them.
         *
         * This replaced an elvis chain that read `error` as a primitive first. On shape B that
         * chain yielded the literal four-letter rendering of the boolean and never reached
         * `reason`; on shape A it raised into a swallowing catch and returned null, discarding
         * the code, the message and the field, so the caller reported the transport's status
         * text instead. Between them, the server's own explanation had never reached a host app
         * on any status.
         */
        internal fun parseErrorBody(json: Json, body: String): ErrorBodyDetails {
            return try {
                val jsonObject = json.parseToJsonElement(body).jsonObject

                when (val error = jsonObject["error"]) {
                    is JsonObject -> ErrorBodyDetails(
                        message = error["message"]?.jsonPrimitive?.contentOrNull,
                        code = error["code"]?.jsonPrimitive?.contentOrNull,
                        field = error["field"]?.jsonPrimitive?.contentOrNull
                    )
                    // Shape B, and anything else parseable: there is no code here, and one
                    // must not be invented.
                    else -> ErrorBodyDetails(
                        message = jsonObject["reason"]?.jsonPrimitive?.contentOrNull
                            ?: jsonObject["message"]?.jsonPrimitive?.contentOrNull,
                        code = null,
                        field = null
                    )
                }
            } catch (e: Exception) {
                ErrorBodyDetails(null, null, null)
            }
        }
    }

    /**
     * JSON serialization instance for use by API classes.
     */
    val jsonSerializer: Json get() = json
}
