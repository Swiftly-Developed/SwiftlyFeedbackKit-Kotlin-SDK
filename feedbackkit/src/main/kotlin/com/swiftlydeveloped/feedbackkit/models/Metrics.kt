package com.swiftlydeveloped.feedbackkit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// ---------------------------------------------------------------------------------------
// Contract of record: METRICS03 phase 01 §6, C1–C8. Nothing in this file was derived from
// reading the server; every wire spelling below is quoted from that table.
//
// Two decoder facts govern every declaration here, and they pull in opposite directions:
//
//   * `ignoreUnknownKeys = true` (http/FeedbackKitHttpClient.kt) makes the SDK tolerant of
//     additive server fields — and makes a MISSPELLED @SerialName completely silent. The
//     property simply falls back to its default on every payload. This package shipped
//     exactly that defect on `Feedback.email` for a whole release (see the comment in
//     Feedback.kt).
//   * `coerceInputValues = true` turns an explicit null for a non-nullable property WITH a
//     DEFAULT into that default, rather than throwing.
//
// The rule that follows, and it is the inverse of "be defensive": a REQUIRED wire field
// gets NO default, so its absence raises MissingFieldException — the only signal that
// exists. An OPTIONAL wire field gets `= null`. A required field given a default is the
// defect, not the safeguard.
// ---------------------------------------------------------------------------------------

/**
 * A measure token — the values the metrics API reads and writes.
 *
 * These are never case-converted: they go on the wire verbatim in the `measures` query
 * parameter, and they are the keys inside every [MetricValues] map.
 *
 * Five are readable with an `X-API-Key`, which is the only credential this SDK sends:
 * [FEEDBACK_COUNT], [VOTE_COUNT], [COMMENT_COUNT], [EVENT_COUNT] and [UNIQUE_EVENT_USERS].
 * The other four are reachable only on the authenticated project surface, which this SDK
 * does not reach; requesting one here is a **403** carrying `METRICS_MEASURE_REQUIRES_BEARER`.
 * They ship in this vocabulary anyway, because the vocabulary documents the server's
 * measure set and the server owns the boundary.
 *
 * [wireValue] duplicates the `@SerialName` deliberately: the annotation drives decoding,
 * [wireValue] drives query building, and a drift between the two is gated.
 */
@Serializable
enum class MetricMeasure(val wireValue: String) {
    /** Feedback items created in the window. */
    @SerialName("feedback_count")
    FEEDBACK_COUNT("feedback_count"),

    /** Votes cast in the window. */
    @SerialName("vote_count")
    VOTE_COUNT("vote_count"),

    /** Comments posted in the window. */
    @SerialName("comment_count")
    COMMENT_COUNT("comment_count"),

    /** View events recorded in the window. */
    @SerialName("event_count")
    EVENT_COUNT("event_count"),

    /** Distinct users who produced a view event. NOT additive — see [MetricValues]. */
    @SerialName("unique_event_users")
    UNIQUE_EVENT_USERS("unique_event_users"),

    /** Not available on the API-key surface: 403 `METRICS_MEASURE_REQUIRES_BEARER`. */
    @SerialName("sdk_user_count")
    SDK_USER_COUNT("sdk_user_count"),

    /** Not available on the API-key surface: 403 `METRICS_MEASURE_REQUIRES_BEARER`. */
    @SerialName("mrr_sum")
    MRR_SUM("mrr_sum"),

    /** Not available on the API-key surface: 403 `METRICS_MEASURE_REQUIRES_BEARER`. */
    @SerialName("mrr_average")
    MRR_AVERAGE("mrr_average"),

    /** Not available on the API-key surface: 403 `METRICS_MEASURE_REQUIRES_BEARER`. */
    @SerialName("users_with_mrr")
    USERS_WITH_MRR("users_with_mrr");
}

/**
 * A bucket width.
 *
 * These are the **wire** tokens. The server's own internal case names (`hourly`, `daily`,
 * …) are not the wire spelling and must never be sent — doing so is a 400
 * (`METRICS_UNKNOWN_GRANULARITY`), not a silent default.
 */
@Serializable
enum class MetricGranularity(val wireValue: String) {
    @SerialName("hour")
    HOUR("hour"),

    @SerialName("day")
    DAY("day"),

    @SerialName("week")
    WEEK("week"),

    @SerialName("month")
    MONTH("month");
}

/** A breakdown dimension. */
@Serializable
enum class MetricDimension(val wireValue: String) {
    @SerialName("status")
    STATUS("status"),

    @SerialName("category")
    CATEGORY("category"),

    @SerialName("event_name")
    EVENT_NAME("event_name");
}

/**
 * Which events the response counted.
 *
 * [PREDEFINED_ONLY] is what a project below the Pro plan gets: a **success**, not an error.
 * The customer's own event names are still ingested and retained; they simply are not read
 * back until the plan allows it, and they become readable on upgrade with no re-tracking.
 * Read this off [MetricsMeta.eventScope] rather than inferring it from the plan.
 */
@Serializable
enum class MetricEventScope(val wireValue: String) {
    @SerialName("all")
    ALL("all"),

    @SerialName("predefined_only")
    PREDEFINED_ONLY("predefined_only");
}

/**
 * Measure token → value.
 *
 * Two things about this map surprise every first integrator, and both are contract:
 *
 * 1. **The keys are the server's snake_case tokens.** `values["event_count"]`, never
 *    `values["eventCount"]`. No naming strategy is configured in this package and
 *    `@SerialName` annotates declared properties only — it has no effect on map keys — so
 *    the tokens arrive verbatim. Nothing in this file may transform them.
 * 2. **The map is SPARSE.** A breakdown's overflow item omits `unique_event_users`
 *    entirely, because a distinct-user count is not additive across the names the
 *    cardinality ceiling elided and no honest number exists. That is why every typed
 *    accessor returns `Double?`. Forcing a value out of this map is reachable in
 *    production, not theoretical.
 */
typealias MetricValues = Map<String, Double>

/**
 * The `meta` block a metrics response carries. Exactly nine fields, of which one is optional.
 */
@Serializable
data class MetricsMeta(
    /** The resolved measure list, in first-seen order. Kept as raw tokens for tolerance. */
    val measures: List<String>,

    /**
     * The resolved bucket width.
     *
     * ABSENT — omitted from the JSON, never `null` — on `summary` and on the event feed's
     * `metrics` block. PRESENT on `timeseries` **and** on `breakdown`, defaulting to `day`.
     * Its presence therefore does not mean "this is a timeseries", and a caller that
     * branches on an explicit null never fires.
     *
     * Kept as a `String?` rather than a [MetricGranularity]: absence and null are
     * indistinguishable in kotlinx, and a token this SDK has not heard of must not make a
     * whole response undecodable.
     */
    val granularity: String? = null,

    /** The reporting zone actually applied. Bucket keys are in THIS zone, not the device's. */
    val timezone: String,

    /** ISO-8601 **instant** — not a plain calendar date. The INCLUSIVE first day. */
    @SerialName("date_from")
    val dateFrom: String,

    /** ISO-8601 **instant**. The INCLUSIVE last day — never an exclusive end. */
    @SerialName("date_to")
    val dateTo: String,

    /** True only when a breakdown's cardinality ceiling elided names. */
    val truncated: Boolean,

    /** Which events were counted. See [MetricEventScope]. */
    @SerialName("event_scope")
    val eventScope: MetricEventScope,

    /**
     * True when the plan narrowed the requested window.
     *
     * NOT idempotent: re-requesting an already-clamped window returns the same window with
     * this flag `false`, because the second request genuinely asks for something inside the
     * allowance. Never derive it by comparing what you asked for against what arrived.
     */
    @SerialName("retention_clamped")
    val retentionClamped: Boolean,

    /**
     * The plan's window allowance, in days (Free 30, Pro 90, Team 365).
     *
     * Deliberately has **no default**. Read this value; never assume a number for it. A
     * default here would be a hardcoded plan allowance wearing a defensive costume, and its
     * absence would then be undetectable.
     */
    @SerialName("retention_window_days")
    val retentionWindowDays: Int,
)

/** The pagination block on the raw event feed. */
@Serializable
data class MetricsPaginationMeta(
    /** Total matching rows across all pages. */
    val total: Int,

    /** The 1-based page returned. */
    val page: Int,

    /** Rows per page. */
    @SerialName("per_page")
    val perPage: Int,

    /** Total number of pages. */
    @SerialName("total_pages")
    val totalPages: Int,
)

/**
 * `GET /metrics/summary` — one aggregate per requested measure.
 *
 * `meta.granularity` is absent on this operation.
 */
@Serializable
data class MetricSummary(
    val meta: MetricsMeta,
    val values: MetricValues,
) {
    /**
     * The value recorded for [m], or `null` when the server omitted it.
     *
     * Optional-returning by contract — see [MetricValues].
     */
    fun measure(m: MetricMeasure): Double? = values[m.wireValue]
}

/**
 * One bucket of a timeseries.
 *
 * The bucket is identified by its [bucket] key alone; there is no separate start-instant
 * field, and this SDK does not parse the key. Its format follows the resolved granularity
 * and it is expressed in `meta.timezone` — re-interpreting it in the device's zone is how
 * a chart silently shifts by an offset.
 */
@Serializable
data class MetricSeriesPoint(
    /** The bucket identifier, formatted for the resolved granularity. */
    val bucket: String,

    /** Sparse measure map — see [MetricValues]. */
    val values: MetricValues,
) {
    /** The value recorded for [m], or `null` when the server omitted it. */
    fun measure(m: MetricMeasure): Double? = values[m.wireValue]
}

/**
 * `GET /metrics/timeseries`.
 *
 * Empty buckets are PRESENT and zero-valued, not omitted, so `series.isEmpty()` is a branch
 * that never fires for a valid window. Handle the all-zero case instead.
 */
@Serializable
data class MetricSeries(
    val meta: MetricsMeta,
    val series: List<MetricSeriesPoint>,
)

/** One row of a breakdown. */
@Serializable
data class MetricBreakdownItem(
    /** The dimension value — a status, a category, or an event name. */
    val key: String,

    /**
     * Sparse measure map. THIS is where the sparsity bites: when [isOverflow] is true the
     * map omits `unique_event_users`. See [MetricValues].
     */
    val values: MetricValues,

    /** True on the single synthetic row aggregating the names the ceiling elided. */
    @SerialName("is_overflow")
    val isOverflow: Boolean,
) {
    /** The value recorded for [m], or `null` when the server omitted it. */
    fun measure(m: MetricMeasure): Double? = values[m.wireValue]
}

/**
 * `GET /metrics/breakdown`.
 *
 * [items] arrives ordered by the first requested measure, descending. Do not re-sort unless
 * you mean to. `meta.granularity` is populated here (`day` by default) even though it does
 * not bucket anything on this surface.
 */
@Serializable
data class MetricBreakdown(
    val meta: MetricsMeta,
    val dimension: MetricDimension,
    val items: List<MetricBreakdownItem>,
)

/**
 * One row of the raw event feed.
 *
 * A distinct type from [TrackedEvent], and not an accident: [TrackedEvent.properties] is a
 * `Map<String, String>?`, which cannot decode a numeric or nested customer property.
 */
@Serializable
data class MetricEventRow(
    val id: String,

    @SerialName("event_name")
    val eventName: String,

    @SerialName("user_id")
    val userId: String? = null,

    /**
     * Arbitrary customer-supplied JSON, keys verbatim. Optional on the wire.
     *
     * A `JsonObject` rather than a typed map: property values may be numbers, booleans or
     * nested objects, and a string-valued map would fail to decode them.
     */
    val properties: JsonObject? = null,

    /** ISO-8601 instant. Optional on the wire. */
    @SerialName("created_at")
    val createdAt: String? = null,
)

/**
 * `GET /metrics/events` — the raw event feed.
 *
 * TWO meta blocks, and they are not interchangeable: [meta] is the pagination block and
 * [metrics] is the [MetricsMeta] every other operation puts under `meta`. A model that puts
 * the metrics block under `meta` here decodes nothing.
 */
@Serializable
data class MetricEventList(
    val items: List<MetricEventRow>,

    /** Pagination. */
    val meta: MetricsPaginationMeta,

    /** The window/scope block — NOT under `meta` on this operation. */
    val metrics: MetricsMeta,
)
