package com.swiftlydeveloped.feedbackkit.api

import com.swiftlydeveloped.feedbackkit.http.FeedbackKitHttpClient
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.models.MetricBreakdown
import com.swiftlydeveloped.feedbackkit.models.MetricDimension
import com.swiftlydeveloped.feedbackkit.models.MetricEventList
import com.swiftlydeveloped.feedbackkit.models.MetricGranularity
import com.swiftlydeveloped.feedbackkit.models.MetricMeasure
import com.swiftlydeveloped.feedbackkit.models.MetricSeries
import com.swiftlydeveloped.feedbackkit.models.MetricSummary

// ---------------------------------------------------------------------------------------
// Contract of record: METRICS03 phase 01 §6, C1–C8.
//
// ⚠️⚠️ Every key put into a query map below goes on the wire VERBATIM. `buildUrl` does not
// case-convert query keys. A camelCase spelling here is not rejected by anything: the
// server simply does not find the parameter, computes over its DEFAULT window, and returns
// 200. A wrong answer, with the right status, and no signal anywhere.
//
// That is why each key is written out one `put` at a time and not produced by a rename loop
// — a mechanical mapper is precisely the thing that would silently accept a mistyped option.
// ---------------------------------------------------------------------------------------

/** The server's accepted range for `per_page`. */
private const val PER_PAGE_MIN = 1
private const val PER_PAGE_MAX = 200

/**
 * The options every metrics operation shares.
 *
 * These property names are idiomatic camelCase; each is mapped BY HAND to its literal
 * snake_case wire key in [toQueryParams].
 */
data class MetricSummaryOptions(
    /** Required, at least one. An empty list throws before the request is made. */
    val measures: List<MetricMeasure>,

    /** Restrict feedback-derived measures to these statuses. Sent comma-joined. */
    val status: List<FeedbackStatus>? = null,

    /** Restrict feedback-derived measures to these categories. Sent comma-joined. */
    val category: List<FeedbackCategory>? = null,

    /**
     * A SINGLE event name — the wire takes one value, not a list. Free-form: any custom
     * name you track is valid, and `PredefinedEventNames` carries the three FeedbackKit
     * defines. Capped at 200 characters by the server.
     */
    val eventName: String? = null,

    /**
     * ISO-8601 instant, inclusive. Mutually exclusive with [days] — sending both is a 400
     * (`METRICS_CONFLICTING_WINDOW_PARAMS`) from the server, which owns that check.
     */
    val dateFrom: String? = null,

    /** ISO-8601 instant, inclusive. */
    val dateTo: String? = null,

    /** A trailing window in days. Mutually exclusive with [dateFrom]/[dateTo]. */
    val days: Int? = null,
) {
    /** Wire keys, in contract order. */
    fun toQueryParams(): Map<String, String> = baseQueryParams(
        measures, status, category, eventName, dateFrom, dateTo, days
    )
}

/** Options for [MetricsApi.timeseries]. */
data class MetricTimeseriesOptions(
    val measures: List<MetricMeasure>,
    val status: List<FeedbackStatus>? = null,
    val category: List<FeedbackCategory>? = null,
    val eventName: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val days: Int? = null,

    /** Bucket width. Defaults to a daily bucket server-side when omitted. */
    val granularity: MetricGranularity? = null,
) {
    /** Wire keys, in contract order. */
    fun toQueryParams(): Map<String, String> = buildMap {
        putAll(baseQueryParams(measures, status, category, eventName, dateFrom, dateTo, days))
        granularity?.let { put("granularity", it.wireValue) }
    }
}

/** Options for [MetricsApi.breakdown]. */
data class MetricBreakdownOptions(
    val measures: List<MetricMeasure>,

    /** Required. Which dimension to group by. */
    val dimension: MetricDimension,

    val status: List<FeedbackStatus>? = null,
    val category: List<FeedbackCategory>? = null,
    val eventName: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val days: Int? = null,

    /**
     * Accepted and sent, because the server reads it on this operation too — but it is
     * INERT here: it is echoed into `meta.granularity` and buckets nothing. An unparseable
     * value is still a 400 (`METRICS_UNKNOWN_GRANULARITY`).
     */
    val granularity: MetricGranularity? = null,
) {
    /** Wire keys, in contract order. */
    fun toQueryParams(): Map<String, String> = buildMap {
        putAll(baseQueryParams(measures, status, category, eventName, dateFrom, dateTo, days))
        granularity?.let { put("granularity", it.wireValue) }
        put("dimension", dimension.wireValue)
    }
}

/** Options for [MetricsApi.events]. */
data class MetricEventsOptions(
    val measures: List<MetricMeasure>,
    val status: List<FeedbackStatus>? = null,
    val category: List<FeedbackCategory>? = null,
    val eventName: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val days: Int? = null,

    /** 1-based page number. */
    val page: Int? = null,

    /**
     * Rows per page, server default 50. Clamped client-side to 1…200 as a fail-fast
     * convenience only. The server is the authority and returns a 400
     * (`METRICS_INVALID_PER_PAGE`) for an out-of-range value; it never clamps silently, so
     * this clamp does not mirror server behaviour, it forestalls a round trip.
     */
    val perPage: Int? = null,
) {
    /** Wire keys, in contract order. */
    fun toQueryParams(): Map<String, String> = buildMap {
        putAll(baseQueryParams(measures, status, category, eventName, dateFrom, dateTo, days))
        page?.let { put("page", it.toString()) }
        perPage?.let { put("per_page", it.coerceIn(PER_PAGE_MIN, PER_PAGE_MAX).toString()) }
    }
}

/**
 * The seven keys every metrics operation shares, in contract order.
 *
 * `measures`, `status` and `category` are ONE comma-joined string each, never repeated
 * parameters; `event_name` is a single value.
 */
private fun baseQueryParams(
    measures: List<MetricMeasure>,
    status: List<FeedbackStatus>?,
    category: List<FeedbackCategory>?,
    eventName: String?,
    dateFrom: String?,
    dateTo: String?,
    days: Int?,
): Map<String, String> = buildMap {
    put("measures", measures.joinToString(",") { it.wireValue })
    status?.takeIf { it.isNotEmpty() }?.let { put("status", it.joinToString(",") { s -> s.jsonValue }) }
    category?.takeIf { it.isNotEmpty() }?.let { put("category", it.joinToString(",") { c -> c.jsonValue }) }
    eventName?.let { put("event_name", it) }
    dateFrom?.let { put("date_from", it) }
    dateTo?.let { put("date_to", it) }
    days?.let { put("days", it.toString()) }
}

/**
 * The two mistakes worth catching before a round trip.
 *
 * Everything else — a conflicting `days` plus `date_from`, an out-of-range `per_page`, an
 * unimplemented measure-by-endpoint combination — is a typed 400 from the server, and
 * duplicating those checks here would build a second allow-list that goes stale the day the
 * server's changes.
 */
private fun validate(measures: List<MetricMeasure>, dateFrom: String?, dateTo: String?) {
    require(measures.isNotEmpty()) { "measures must contain at least one measure" }
    if (dateFrom != null && dateTo != null) {
        require(dateFrom <= dateTo) { "dateFrom must not be later than dateTo" }
    }
}

/**
 * API for reading this project's metrics.
 *
 * All four operations are rate limited to 60 requests per 60 seconds, shared across the
 * family. The SDK does **not** retry or back off — hiding the cap behind a retry loop makes
 * it invisible. A 429 surfaces as `FeedbackKitError.RateLimitError`.
 *
 * A project below the Pro plan gets a **success**, not an error: the response is narrowed
 * and says so, via `meta.eventScope` and `meta.retentionClamped`. The single 402
 * (`METRICS_CUSTOM_EVENTS_REQUIRE_PRO`) is reserved for the three requests that have no
 * honest narrower answer — a breakdown by event name, a custom `eventName` filter, and the
 * raw event feed.
 *
 * Every operation here uses the API key, which identifies the project; there is no project
 * parameter to pass.
 */
class MetricsApi internal constructor(
    private val http: FeedbackKitHttpClient
) {
    /**
     * Aggregate totals over the window — one number per requested measure.
     *
     * `meta.granularity` is ABSENT on this operation — omitted, never null.
     *
     * @param options Measures, filters and window.
     * @return `meta` plus a sparse `values` map keyed by the server's measure tokens.
     * @throws IllegalArgumentException if `measures` is empty or the date range is inverted.
     */
    suspend fun summary(options: MetricSummaryOptions): MetricSummary {
        validate(options.measures, options.dateFrom, options.dateTo)
        return http.get(
            endpoint = "metrics/summary",
            queryParams = options.toQueryParams()
        ) { body ->
            http.jsonSerializer.decodeFromString<MetricSummary>(body)
        }
    }

    /**
     * Bucketed totals over the window.
     *
     * Empty buckets are PRESENT and zero-valued, so an `isEmpty()` branch never fires for a
     * valid window. Bucket keys are expressed in `meta.timezone`.
     *
     * @param options Measures, filters, window and bucket width.
     * @return `meta` (with `granularity` populated) plus the ordered bucket list.
     * @throws IllegalArgumentException if `measures` is empty or the date range is inverted.
     */
    suspend fun timeseries(options: MetricTimeseriesOptions): MetricSeries {
        validate(options.measures, options.dateFrom, options.dateTo)
        return http.get(
            endpoint = "metrics/timeseries",
            queryParams = options.toQueryParams()
        ) { body ->
            http.jsonSerializer.decodeFromString<MetricSeries>(body)
        }
    }

    /**
     * Totals grouped by a dimension, ordered by the first requested measure descending.
     *
     * Past the server's cardinality ceiling the tail collapses into a single item with
     * `isOverflow` true, `meta.truncated` goes true, and that item's `values` OMITS
     * `unique_event_users`. Use `item.measure(...)`; never assert non-null.
     *
     * @param options Measures, filters, window and the required dimension.
     * @throws IllegalArgumentException if `measures` is empty or the date range is inverted.
     */
    suspend fun breakdown(options: MetricBreakdownOptions): MetricBreakdown {
        validate(options.measures, options.dateFrom, options.dateTo)
        return http.get(
            endpoint = "metrics/breakdown",
            queryParams = options.toQueryParams()
        ) { body ->
            http.jsonSerializer.decodeFromString<MetricBreakdown>(body)
        }
    }

    /**
     * The raw event feed, paginated.
     *
     * The response carries TWO meta blocks: `meta` is pagination, `metrics` is the window
     * and scope block every other operation puts under `meta`.
     *
     * There is no automatic page following. Pass an explicit `dateTo` so the window stays
     * stable while you page, then advance it on the next sync.
     *
     * @param options Measures, filters, window and pagination.
     * @throws IllegalArgumentException if `measures` is empty or the date range is inverted.
     */
    suspend fun events(options: MetricEventsOptions): MetricEventList {
        validate(options.measures, options.dateFrom, options.dateTo)
        return http.get(
            endpoint = "metrics/events",
            queryParams = options.toQueryParams()
        ) { body ->
            http.jsonSerializer.decodeFromString<MetricEventList>(body)
        }
    }
}
