package com.swiftlydeveloped.feedbackkit

import com.swiftlydeveloped.feedbackkit.api.MetricBreakdownOptions
import com.swiftlydeveloped.feedbackkit.api.MetricEventsOptions
import com.swiftlydeveloped.feedbackkit.api.MetricSummaryOptions
import com.swiftlydeveloped.feedbackkit.api.MetricTimeseriesOptions
import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.errors.isRecoverable
import com.swiftlydeveloped.feedbackkit.http.FeedbackKitHttpClient
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.models.MetricBreakdown
import com.swiftlydeveloped.feedbackkit.models.MetricBreakdownItem
import com.swiftlydeveloped.feedbackkit.models.MetricDimension
import com.swiftlydeveloped.feedbackkit.models.MetricEventList
import com.swiftlydeveloped.feedbackkit.models.MetricEventRow
import com.swiftlydeveloped.feedbackkit.models.MetricGranularity
import com.swiftlydeveloped.feedbackkit.models.MetricMeasure
import com.swiftlydeveloped.feedbackkit.models.MetricEventScope
import com.swiftlydeveloped.feedbackkit.models.MetricSeries
import com.swiftlydeveloped.feedbackkit.models.MetricSeriesPoint
import com.swiftlydeveloped.feedbackkit.models.MetricSummary
import com.swiftlydeveloped.feedbackkit.models.MetricsMeta
import com.swiftlydeveloped.feedbackkit.models.MetricsPaginationMeta
import com.swiftlydeveloped.feedbackkit.models.PredefinedEventNames
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * `METRICS03` phase `09` — the Kotlin metrics surface. Gates `G1`, `G3`–`G8`, plus the
 * static halves of `G9` and `G10`.
 *
 * 🔴 **Read-derived, not measured.** This file was authored on a host with **no JDK and no
 * Android SDK** (`java -version` → *"Unable to locate a Java Runtime"*), so **not one
 * assertion below has ever been executed.** The phase plan's `G0` — the whole target,
 * unfiltered, with the test count asserted from the XML report — is **owed** and must be
 * run before anyone treats this suite as evidence. Anything in this repository that reports
 * this lane as green without an accompanying `BUILD` line and a test count is reporting a
 * run that did not happen.
 *
 * Conventions inherited from `SdkParityTest`:
 *
 *  * Every fixture is a committed corpus from phase `01`; **no JSON is hand-written here**.
 *    Negative cases perform runtime surgery on a **parsed** `JsonObject`.
 *  * Every parity assertion is a **set or ordered-list equality**, never a `contains` — a
 *    presence oracle cannot see an extra, and this surface has five "extra" axes (a tenth
 *    `meta` field, a fifth operation, a tenth measure, a fifth granularity, an eleventh
 *    error subclass).
 *  * Every zero-expecting leg first asserts a **non-zero denominator**. A count of zero over
 *    a path that does not exist is indistinguishable from a pass.
 *  * Source-scanning legs run **comment-stripped**, and assert the stripper did not return
 *    an empty corpus.
 */
@OptIn(ExperimentalSerializationApi::class)
class MetricsTest {

    /** Replicates `FeedbackKitHttpClient`'s `Json` configuration, like `SdkParityTest`. */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    // ---------------------------------------------------------------------------------
    // Corpus and source-tree helpers
    // ---------------------------------------------------------------------------------

    private fun corpusObject(name: String): JsonObject {
        val stream = javaClass.classLoader!!.getResourceAsStream(name)
        assertNotNull("$name is missing from test resources", stream)
        return json.parseToJsonElement(stream!!.bufferedReader().readText()).jsonObject
    }

    private fun wireCorpus(): JsonObject = corpusObject("metrics-wire-corpus.json")

    private fun errorCorpus(): JsonObject = corpusObject("metrics-error-wire-corpus.json")

    /** The `feedbackkit` module directory, wherever the test runner rooted itself. */
    private fun moduleDir(): File {
        var dir: File? = File(".").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "src/main/kotlin/com/swiftlydeveloped/feedbackkit")
            if (candidate.isDirectory) return dir
            val nested = File(dir, "feedbackkit/src/main/kotlin/com/swiftlydeveloped/feedbackkit")
            if (nested.isDirectory) return File(dir, "feedbackkit")
            dir = dir.parentFile
        }
        fail("could not locate the feedbackkit module directory from ${File(".").absolutePath}")
        error("unreachable")
    }

    private fun mainSourceRoot(): File =
        File(moduleDir(), "src/main/kotlin/com/swiftlydeveloped/feedbackkit")

    private fun mainSource(relative: String): String {
        val file = File(mainSourceRoot(), relative)
        assertTrue("$relative not found at ${file.absolutePath}", file.isFile)
        val text = file.readText()
        assertTrue("$relative is empty", text.isNotEmpty())
        return text
    }

    /**
     * Strip block comments, KDoc and line comments, so an absence gate cannot be reddened —
     * or greened — by prose.
     *
     * Two known limits, both fail-OPEN (they can only hide a needle, never invent one), and
     * both harmless for the files this suite scans: a line comment opener inside a string
     * literal truncates the rest of that line, and the block matcher is non-greedy so a
     * doc comment containing a block terminator ends early. Every caller asserts the result
     * is non-blank, so a stripper that ate everything cannot turn an absence leg vacuously
     * green.
     */
    private fun stripComments(source: String): String {
        val withoutBlocks = source.replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), " ")
        // The lookbehind spares `https://…`, the only shape in this package that would
        // otherwise be read as a line comment.
        val lineComment = Regex("""(?<!:)//.*$""", RegexOption.MULTILINE)
        return withoutBlocks.replace(lineComment, "")
    }

    private fun strippedMainSource(relative: String): String {
        val stripped = stripComments(mainSource(relative))
        assertTrue("comment-stripping $relative produced nothing", stripped.isNotBlank())
        return stripped
    }

    private fun allMainKotlinFiles(): List<File> =
        mainSourceRoot().walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    // ---------------------------------------------------------------------------------
    // G1 — the query builder, all five breaking classes, with controls that prove it looked
    // ---------------------------------------------------------------------------------

    /**
     * The five values that discriminate a real percent-encoder from a naive string join.
     * A suite whose only hostile value is a space, an accent or an emoji is green against a
     * BROKEN builder; the sibling Swift phase measured a naive join at 7/12 and every one of
     * the five failures was in this list.
     */
    private val breakingValues = listOf(
        "a&b=c",
        "a#b",
        "a+b",
        "100%20done",
        "my event & more · ünïcode"
    )

    /** The seven controls, so the gate can tell "encoding is fine" from "encoding changed". */
    private val controlValues = listOf(
        "my event",
        "ünïcode · dot",
        "checkout 🛒",
        "say \"hi\"",
        "a=b",
        "a?b",
        "a;b"
    )

    private val hostileValues = breakingValues + controlValues

    /** `FeedbackKitHttpClient.buildUrl`'s exact query expression, replicated verbatim. */
    private fun encodeQuery(params: Map<String, String>): String =
        params.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }

    /**
     * Vapor's `URLEncodedFormParser`, replicated: every literal `+` becomes `%20` BEFORE
     * percent-decoding, which is what makes form encoding round-trip on this server. A `+`
     * inside a value arrives as `%2B` and is therefore untouched by that replacement.
     */
    private fun vaporDecodeQuery(query: String): Map<String, String> {
        val pairs = query.split("&").filter { it.isNotEmpty() }
        return pairs.associate { pair ->
            val split = pair.indexOf('=')
            assertTrue("query pair carries no '=': $pair", split >= 0)
            val rawKey = pair.substring(0, split).replace("+", "%20")
            val rawValue = pair.substring(split + 1).replace("+", "%20")
            URLDecoder.decode(rawKey, "UTF-8") to URLDecoder.decode(rawValue, "UTF-8")
        }
    }

    private fun summaryOptions(eventName: String) = MetricSummaryOptions(
        measures = listOf(MetricMeasure.FEEDBACK_COUNT, MetricMeasure.EVENT_COUNT),
        status = listOf(FeedbackStatus.PENDING, FeedbackStatus.IN_PROGRESS),
        category = listOf(FeedbackCategory.BUG_REPORT),
        eventName = eventName,
        dateFrom = "2023-11-12T05:00:00Z",
        dateTo = "2023-11-14T05:00:00Z",
        days = 7
    )

    private fun timeseriesOptions(eventName: String) = MetricTimeseriesOptions(
        measures = listOf(MetricMeasure.FEEDBACK_COUNT, MetricMeasure.EVENT_COUNT),
        status = listOf(FeedbackStatus.PENDING, FeedbackStatus.IN_PROGRESS),
        category = listOf(FeedbackCategory.BUG_REPORT),
        eventName = eventName,
        dateFrom = "2023-11-12T05:00:00Z",
        dateTo = "2023-11-14T05:00:00Z",
        days = 7,
        granularity = MetricGranularity.DAY
    )

    private fun breakdownOptions(eventName: String) = MetricBreakdownOptions(
        measures = listOf(MetricMeasure.FEEDBACK_COUNT, MetricMeasure.EVENT_COUNT),
        dimension = MetricDimension.EVENT_NAME,
        status = listOf(FeedbackStatus.PENDING, FeedbackStatus.IN_PROGRESS),
        category = listOf(FeedbackCategory.BUG_REPORT),
        eventName = eventName,
        dateFrom = "2023-11-12T05:00:00Z",
        dateTo = "2023-11-14T05:00:00Z",
        days = 7,
        granularity = MetricGranularity.DAY
    )

    private fun eventsOptions(eventName: String) = MetricEventsOptions(
        measures = listOf(MetricMeasure.FEEDBACK_COUNT, MetricMeasure.EVENT_COUNT),
        status = listOf(FeedbackStatus.PENDING, FeedbackStatus.IN_PROGRESS),
        category = listOf(FeedbackCategory.BUG_REPORT),
        eventName = eventName,
        dateFrom = "2023-11-12T05:00:00Z",
        dateTo = "2023-11-14T05:00:00Z",
        days = 7,
        page = 2,
        perPage = 50
    )

    private fun allOperationParams(eventName: String): List<Map<String, String>> = listOf(
        summaryOptions(eventName).toQueryParams(),
        timeseriesOptions(eventName).toQueryParams(),
        breakdownOptions(eventName).toQueryParams(),
        eventsOptions(eventName).toQueryParams()
    )

    @Test
    fun `G1 every hostile event name round-trips through the shipped query encoder`() {
        assertEquals("the hostile-value list must stay at twelve", 12, hostileValues.size)
        assertEquals("five of them must be the breaking classes", 5, breakingValues.size)

        var checked = 0
        val failures = mutableListOf<String>()

        for (value in hostileValues) {
            for (params in allOperationParams(value)) {
                val decoded = vaporDecodeQuery(encodeQuery(params))
                checked += 1
                if (decoded["event_name"] != value) {
                    failures += "event_name $value -> ${decoded["event_name"]}"
                }
                if (decoded.size != params.size) {
                    failures += "key count ${params.size} -> ${decoded.size} for $value"
                }
            }
        }

        assertEquals("the round-trip must have been exercised 12 values x 4 operations", 48, checked)
        assertEquals("values that did not survive the round trip: $failures", emptyList<String>(), failures)
    }

    @Test
    fun `G1 every other option value also round-trips, so the gate is not event_name-only`() {
        val params = breakdownOptions("checkout 🛒 & more").toQueryParams()
        val decoded = vaporDecodeQuery(encodeQuery(params))

        assertTrue("denominator: the breakdown must emit keys", params.isNotEmpty())
        assertEquals(params, decoded)
    }

    @Test
    fun `G1 the wire keys of all four operations are exactly the contract's, in order`() {
        val base = listOf(
            "measures", "status", "category", "event_name", "date_from", "date_to", "days"
        )

        assertEquals(base, summaryOptions("x").toQueryParams().keys.toList())
        assertEquals(base + "granularity", timeseriesOptions("x").toQueryParams().keys.toList())
        assertEquals(
            base + "granularity" + "dimension",
            breakdownOptions("x").toQueryParams().keys.toList()
        )
        assertEquals(
            base + "page" + "per_page",
            eventsOptions("x").toQueryParams().keys.toList()
        )
    }

    @Test
    fun `G1 no emitted query key carries an uppercase letter`() {
        val keys = allOperationParams("x").flatMap { it.keys }
        assertTrue("denominator: no keys were examined", keys.isNotEmpty())
        assertEquals(4 * 7 + 1 + 2 + 2, keys.size)

        val camel = keys.filter { it.contains(Regex("[A-Z]")) }
        assertEquals("camelCase query keys are a silent 200 over the default window", emptyList<String>(), camel)
    }

    @Test
    fun `G1 measures status and category are one comma-joined value each`() {
        val params = summaryOptions("x").toQueryParams()

        assertEquals("feedback_count,event_count", params["measures"])
        assertEquals("pending,in_progress", params["status"])
        assertEquals("bug_report", params["category"])
        assertEquals("event_name is a single value, never a list", "x", params["event_name"])
    }

    @Test
    fun `G1 per_page is clamped client-side to the server's accepted range`() {
        val low = eventsOptions("x").copy(perPage = 0).toQueryParams()
        val high = eventsOptions("x").copy(perPage = 5000).toQueryParams()
        val inside = eventsOptions("x").copy(perPage = 75).toQueryParams()

        assertEquals("1", low["per_page"])
        assertEquals("200", high["per_page"])
        assertEquals("75", inside["per_page"])
    }

    // ---------------------------------------------------------------------------------
    // G3 — every declared property binds a non-default value, and required means required
    // ---------------------------------------------------------------------------------

    private fun elementNamesOf(descriptor: SerialDescriptor): Set<String> =
        descriptor.elementNames.toSet()

    @Test
    fun `G3 every metrics model declares exactly the contract's fields, both directions`() {
        assertEquals(
            setOf(
                "measures", "granularity", "timezone", "date_from", "date_to",
                "truncated", "event_scope", "retention_clamped", "retention_window_days"
            ),
            elementNamesOf(MetricsMeta.serializer().descriptor)
        )
        assertEquals(
            setOf("total", "page", "per_page", "total_pages"),
            elementNamesOf(MetricsPaginationMeta.serializer().descriptor)
        )
        assertEquals(setOf("meta", "values"), elementNamesOf(MetricSummary.serializer().descriptor))
        assertEquals(setOf("bucket", "values"), elementNamesOf(MetricSeriesPoint.serializer().descriptor))
        assertEquals(setOf("meta", "series"), elementNamesOf(MetricSeries.serializer().descriptor))
        assertEquals(
            setOf("key", "values", "is_overflow"),
            elementNamesOf(MetricBreakdownItem.serializer().descriptor)
        )
        assertEquals(
            setOf("meta", "dimension", "items"),
            elementNamesOf(MetricBreakdown.serializer().descriptor)
        )
        assertEquals(
            setOf("id", "event_name", "user_id", "properties", "created_at"),
            elementNamesOf(MetricEventRow.serializer().descriptor)
        )
        assertEquals(
            setOf("items", "meta", "metrics"),
            elementNamesOf(MetricEventList.serializer().descriptor)
        )
    }

    @Test
    fun `G3 every declared meta property binds a real wire value, none falls back to a default`() {
        // The breakdown meta is the only corpus row carrying all nine — granularity present
        // and truncated true — so it is the row that can prove a full bind.
        val raw = wireCorpus()["breakdown_event_name_with_overflow"]!!.jsonObject["meta"]!!.jsonObject
        val meta = json.decodeFromJsonElement<MetricsMeta>(raw)

        var checked = 0

        assertEquals(raw["measures"]!!.jsonArray.map { it.jsonPrimitive.content }, meta.measures)
        assertTrue(meta.measures.isNotEmpty()); checked += 1

        assertEquals(raw["granularity"]!!.jsonPrimitive.content, meta.granularity)
        assertNotNull(meta.granularity); checked += 1

        assertEquals(raw["timezone"]!!.jsonPrimitive.content, meta.timezone)
        assertTrue(meta.timezone.isNotBlank()); checked += 1

        assertEquals(raw["date_from"]!!.jsonPrimitive.content, meta.dateFrom); checked += 1
        assertEquals(raw["date_to"]!!.jsonPrimitive.content, meta.dateTo); checked += 1

        // Non-default in the literal sense: `truncated` is true only on this row.
        assertTrue("this row must be the truncated one", meta.truncated); checked += 1

        assertEquals(MetricEventScope.ALL, meta.eventScope); checked += 1
        assertFalse(meta.retentionClamped); checked += 1

        assertEquals(raw["retention_window_days"]!!.jsonPrimitive.int, meta.retentionWindowDays)
        assertTrue("the allowance must be read from the wire", meta.retentionWindowDays > 0)
        checked += 1

        assertEquals(
            "the census must have visited every declared property",
            MetricsMeta.serializer().descriptor.elementsCount,
            checked
        )
    }

    @Test
    fun `G3 the free-tier summary meta binds the scoped and clamped values, not defaults`() {
        val raw = wireCorpus()["summary_free_scoped_and_clamped"]!!.jsonObject["meta"]!!.jsonObject
        val meta = json.decodeFromJsonElement<MetricsMeta>(raw)

        assertEquals(MetricEventScope.PREDEFINED_ONLY, meta.eventScope)
        assertTrue(meta.retentionClamped)
        assertEquals(raw["retention_window_days"]!!.jsonPrimitive.int, meta.retentionWindowDays)
    }

    @Test
    fun `G3 granularity is ABSENT on summary, not null, and decoding still succeeds`() {
        val raw = wireCorpus()["summary_free_scoped_and_clamped"]!!.jsonObject["meta"]!!.jsonObject

        assertFalse(
            "D1: the key is omitted. A null-expecting assertion never fires",
            raw.containsKey("granularity")
        )
        assertNull(json.decodeFromJsonElement<MetricsMeta>(raw).granularity)
    }

    @Test
    fun `G3 granularity is present on breakdown, so its absence is per-surface not per-family`() {
        val breakdownMeta =
            wireCorpus()["breakdown_event_name_with_overflow"]!!.jsonObject["meta"]!!.jsonObject
        val eventsMetrics = wireCorpus()["events_page"]!!.jsonObject["metrics"]!!.jsonObject

        assertTrue(breakdownMeta.containsKey("granularity"))
        assertFalse(eventsMetrics.containsKey("granularity"))
        assertEquals(9, breakdownMeta.size)
        assertEquals(8, eventsMetrics.size)
    }

    @Test
    fun `G3 every required meta key throws when removed — none has a hardcoded fallback`() {
        val raw = wireCorpus()["timeseries_daily"]!!.jsonObject["meta"]!!.jsonObject
        val required = listOf(
            "measures", "timezone", "date_from", "date_to",
            "truncated", "event_scope", "retention_clamped", "retention_window_days"
        )

        assertEquals(
            "the required set must be the nine minus the one optional",
            MetricsMeta.serializer().descriptor.elementsCount - 1,
            required.size
        )

        for (key in required) {
            assertTrue("denominator: $key must be present before removal", raw.containsKey(key))
            val mutated = JsonObject(raw.filterKeys { it != key })
            try {
                json.decodeFromJsonElement<MetricsMeta>(mutated)
                fail("removing $key must throw — a default there is a hardcoded plan value")
            } catch (e: MissingFieldException) {
                // expected
            }
        }
    }

    @Test
    fun `G3 date_from is an ISO-8601 instant, not a plain calendar date`() {
        val metas = listOf(
            wireCorpus()["summary_free_scoped_and_clamped"]!!.jsonObject["meta"]!!.jsonObject,
            wireCorpus()["timeseries_daily"]!!.jsonObject["meta"]!!.jsonObject,
            wireCorpus()["breakdown_event_name_with_overflow"]!!.jsonObject["meta"]!!.jsonObject,
            wireCorpus()["events_page"]!!.jsonObject["metrics"]!!.jsonObject
        )
        assertEquals("denominator: four meta blocks must be examined", 4, metas.size)

        for (raw in metas) {
            val meta = json.decodeFromJsonElement<MetricsMeta>(raw)
            for (value in listOf(meta.dateFrom, meta.dateTo)) {
                assertTrue("not an instant: $value", value.contains("T"))
                assertTrue("no time component: $value", value.contains(Regex("""\d{2}:\d{2}""")))
            }
        }
    }

    @Test
    fun `G3 an unknown additive key still decodes, and the key was really there`() {
        val raw = wireCorpus()["timeseries_daily"]!!.jsonObject["meta"]!!.jsonObject
        val addedKey = "a_field_this_sdk_has_never_heard_of"
        val mutated = JsonObject(raw + (addedKey to JsonPrimitive(1)))

        assertTrue("denominator: the added key must be in the mutated JSON", mutated.containsKey(addedKey))
        assertEquals(raw.size + 1, mutated.size)

        val meta = json.decodeFromJsonElement<MetricsMeta>(mutated)
        assertEquals(raw["timezone"]!!.jsonPrimitive.content, meta.timezone)
    }

    // ---------------------------------------------------------------------------------
    // G4 — `values` is sparse, snake_case-keyed, and the accessor is optional-returning
    // ---------------------------------------------------------------------------------

    @Test
    fun `G4 the values map keys are the server's tokens, verbatim and unrewritten`() {
        val row = wireCorpus()["summary_team_all_scope"]!!.jsonObject
        val rawKeys = row["values"]!!.jsonObject.keys

        assertTrue("denominator: the corpus row must carry values", rawKeys.isNotEmpty())

        val summary = json.decodeFromJsonElement<MetricSummary>(row)
        assertEquals(rawKeys, summary.values.keys)
    }

    @Test
    fun `G4 the typed accessor resolves the snake_case token and the camelCase twin is absent`() {
        val row = wireCorpus()["summary_team_all_scope"]!!.jsonObject
        val summary = json.decodeFromJsonElement<MetricSummary>(row)

        assertNotNull(summary.measure(MetricMeasure.EVENT_COUNT))
        // BOTH directions. A one-directional gate passes a decoder that emitted both.
        assertNull(summary.values["eventCount"])
        assertNull(summary.values["uniqueEventUsers"])
    }

    @Test
    fun `G4 the breakdown overflow row omits unique_event_users, and the row was found`() {
        val breakdown = json.decodeFromJsonElement<MetricBreakdown>(
            wireCorpus()["breakdown_event_name_with_overflow"]!!
        )

        val overflow = breakdown.items.filter { it.isOverflow }
        assertEquals("denominator: exactly one overflow row must exist", 1, overflow.size)

        val item = overflow.single()
        assertNull(
            "a distinct-user count is not additive across elided names",
            item.measure(MetricMeasure.UNIQUE_EVENT_USERS)
        )
        assertNotNull(item.measure(MetricMeasure.EVENT_COUNT))

        val normal = breakdown.items.single { !it.isOverflow }
        assertNotNull(normal.measure(MetricMeasure.UNIQUE_EVENT_USERS))
    }

    @Test
    fun `G4 a series point's values survive verbatim and the zero bucket is present`() {
        val series = json.decodeFromJsonElement<MetricSeries>(wireCorpus()["timeseries_daily"]!!)

        assertEquals(3, series.series.size)
        assertEquals(
            "empty buckets are present and zero-valued, never omitted",
            0.0,
            series.series[1].measure(MetricMeasure.EVENT_COUNT)!!,
            0.0
        )
        assertNull(series.series[0].values["eventCount"])
    }

    @Test
    fun `G4 an event row's properties keep customer keys verbatim, including a non-string value`() {
        val feed = wireCorpus()["events_page"]!!.jsonObject
        val rawRow = feed["items"]!!.jsonArray[0].jsonObject
        val rawProps = rawRow["properties"]!!.jsonObject

        assertTrue("denominator: the corpus row must carry properties", rawProps.isNotEmpty())

        // Runtime surgery on the PARSED row: a numeric property is the leg that fails a
        // string-valued map declaration.
        val numericKey = "items_in_cart"
        val mutatedRow = JsonObject(
            rawRow + ("properties" to JsonObject(rawProps + (numericKey to JsonPrimitive(3))))
        )

        val row = json.decodeFromJsonElement<MetricEventRow>(mutatedRow)
        val properties = row.properties
        assertNotNull(properties)
        assertEquals(rawProps.keys + numericKey, properties!!.keys)
        assertEquals(3, properties[numericKey]!!.jsonPrimitive.int)
        assertEquals("payment", properties["checkout_step"]!!.jsonPrimitive.content)
    }

    @Test
    fun `G4 the models file has no forced unwraps and every accessor is optional-returning`() {
        val source = strippedMainSource("models/Metrics.kt")
        assertTrue("denominator: the stripped source must be substantial", source.length > 1000)

        val forced = Regex(Regex.escape("!") + Regex.escape("!")).findAll(source).count()
        assertEquals("a forced unwrap of a sparse map is reachable in production", 0, forced)

        val accessors = Regex("""fun measure\(m: MetricMeasure\): Double\?""").findAll(source).count()
        assertEquals("summary, series point and breakdown item each carry one", 3, accessors)
    }

    // ---------------------------------------------------------------------------------
    // G5 — both error bodies, cardinality 2, and the 403 that arrives in both
    // ---------------------------------------------------------------------------------

    /** Wire token → HTTP status, derived from the corpus rather than retyped. */
    private fun statusForToken(): Map<String, Int> =
        errorCorpus()["vocabulary"]!!.jsonArray.associate { row ->
            row.jsonObject["wire_token"]!!.jsonPrimitive.content to
                row.jsonObject["http_status"]!!.jsonPrimitive.int
        }

    private fun envelopedRows(): List<JsonObject> =
        errorCorpus()["enveloped"]!!.jsonArray.map { it.jsonObject }

    private fun middlewareRows(): List<JsonObject> =
        errorCorpus()["middleware"]!!.jsonArray.map { it.jsonObject }

    /** The verbatim wire body a `middleware` row wraps. */
    private fun JsonObject.wireBody(): JsonObject = this["body"]!!.jsonObject

    /**
     * The status that row is served at, read off the row.
     *
     * Rows used to be bare bodies with no status, so a consumer had to pair them to
     * `C6`'s prose by index. Each row now carries its own `http_status`, derived by the
     * generator from the server source line the `reason` occurs on.
     */
    private fun JsonObject.wireStatus(): Int = this["http_status"]!!.jsonPrimitive.int

    /** One shared-layer row, selected by content. `single` reds on an ambiguous fragment. */
    private fun middlewareRow(reasonFragment: String): JsonObject =
        middlewareRows().single {
            it.wireBody()["reason"]!!.jsonPrimitive.content.contains(reasonFragment)
        }

    private fun errorFor(body: JsonObject, statusCode: Int): FeedbackKitError {
        val details = FeedbackKitHttpClient.parseErrorBody(json, body.toString())
        return FeedbackKitError.fromStatusCode(
            statusCode = statusCode,
            message = details.message ?: "TRANSPORT STATUS TEXT",
            code = details.code,
            field = details.field
        )
    }

    private fun jsonKind(element: JsonElement): String = when (element) {
        is JsonObject -> "object"
        is JsonArray -> "array"
        is JsonNull -> "null"
        is JsonPrimitive -> "primitive"
    }

    @Test
    fun `G5 the set of JSON types of the top-level error has cardinality exactly two`() {
        val rows = envelopedRows() + middlewareRows().map { it.wireBody() }
        assertTrue("denominator: no error rows were examined", rows.isNotEmpty())
        // Derived from the corpus, not typed: `enveloped` is a total cover of the 17
        // wire tokens (plus the second, field-less METRICS_CUSTOM_EVENTS_REQUIRE_PRO
        // row), so a literal here would need editing every time the vocabulary grows.
        assertEquals(envelopedRows().size + middlewareRows().size, rows.size)

        val kinds = rows.map { jsonKind(it["error"]!!) }.toSet()

        // A presence oracle passes a one-shape decoder, which IS the bug. Only cardinality
        // catches it.
        assertEquals(2, kinds.size)
        assertEquals(setOf("object", "primitive"), kinds)
    }

    @Test
    fun `G5 shape A carries the wire code onto the error, overriding the class default`() {
        val statuses = statusForToken()

        val proRow = envelopedRows().single {
            it["error"]!!.jsonObject["code"]!!.jsonPrimitive.content == "METRICS_CUSTOM_EVENTS_REQUIRE_PRO" &&
                it["error"]!!.jsonObject.containsKey("field")
        }
        val proToken = proRow["error"]!!.jsonObject["code"]!!.jsonPrimitive.content
        val error = errorFor(proRow, statuses.getValue(proToken))

        assertEquals(402, statuses.getValue(proToken))
        assertTrue(error is FeedbackKitError.PaymentRequiredError)
        assertEquals("the wire token, not the class default", proToken, error.code)
        assertEquals(
            proRow["error"]!!.jsonObject["message"]!!.jsonPrimitive.content,
            error.message
        )
    }

    @Test
    fun `G5 the measure 403 is shape A with a code, and stays a ForbiddenError`() {
        val statuses = statusForToken()
        val row = envelopedRows().single {
            it["error"]!!.jsonObject["code"]!!.jsonPrimitive.content == "METRICS_MEASURE_REQUIRES_BEARER"
        }
        val token = row["error"]!!.jsonObject["code"]!!.jsonPrimitive.content

        assertEquals(403, statuses.getValue(token))

        val error = errorFor(row, statuses.getValue(token))
        assertTrue(error is FeedbackKitError.ForbiddenError)
        assertEquals(token, error.code)
        assertEquals(403, error.statusCode)
    }

    @Test
    fun `G5 403 arrives in BOTH shapes and the two stay distinguishable`() {
        val statuses = statusForToken()

        val shapeA = envelopedRows().single {
            it["error"]!!.jsonObject["code"]!!.jsonPrimitive.content == "METRICS_MEASURE_REQUIRES_BEARER"
        }
        val shapeBRow = middlewareRow("suspended")
        val shapeB = shapeBRow.wireBody()

        assertEquals("object", jsonKind(shapeA["error"]!!))
        assertEquals("primitive", jsonKind(shapeB["error"]!!))

        val fromEnvelope = errorFor(shapeA, statuses.getValue("METRICS_MEASURE_REQUIRES_BEARER"))
        assertEquals(403, shapeBRow.wireStatus())
        val fromMiddleware = errorFor(shapeB, shapeBRow.wireStatus())

        assertTrue(fromEnvelope is FeedbackKitError.ForbiddenError)
        assertTrue(fromMiddleware is FeedbackKitError.ForbiddenError)

        // Same class, same status — only the body separates them. A status-code branch
        // cannot produce this pair.
        assertEquals("METRICS_MEASURE_REQUIRES_BEARER", fromEnvelope.code)
        assertEquals("FORBIDDEN", fromMiddleware.code)
        assertFalse(fromEnvelope.message == fromMiddleware.message)
    }

    @Test
    fun `G5 shape B surfaces the server's reason, never the boolean's rendering`() {
        val row = middlewareRow("Invalid API key")
        assertEquals(401, row.wireStatus())
        val error = errorFor(row.wireBody(), row.wireStatus())

        assertTrue(error is FeedbackKitError.AuthenticationError)
        assertEquals("Invalid API key", error.message)
        assertEquals("shape B has no wire code and one must not be invented", "UNAUTHORIZED", error.code)

        // The shipped elvis chain read `error` as a primitive first and yielded the
        // boolean's own rendering, never reaching `reason`. Pin that it cannot come back.
        assertFalse(error.message == JsonPrimitive(true).content)
    }

    @Test
    fun `G5 shape B 429 is a RateLimitError carrying the real status`() {
        val row = middlewareRow("Too many requests")
        assertEquals(429, row.wireStatus())
        val error = errorFor(row.wireBody(), row.wireStatus())

        assertTrue(error is FeedbackKitError.RateLimitError)
        assertEquals(429, error.statusCode)
        assertEquals("RATE_LIMITED", error.code)
        assertEquals(row.wireBody()["reason"]!!.jsonPrimitive.content, error.message)
        assertTrue("a rate limit is the most recoverable failure in the vocabulary", error.isRecoverable)
    }

    @Test
    fun `G5 shape B 404 is a NotFoundError — the version-skew case`() {
        val row = middlewareRow("Project not found")
        assertEquals(404, row.wireStatus())
        val error = errorFor(row.wireBody(), row.wireStatus())

        assertTrue(error is FeedbackKitError.NotFoundError)
        assertEquals("Project not found", error.message)
    }

    @Test
    fun `G5 an unparseable body degrades to the transport's status text, not to a crash`() {
        val details = FeedbackKitHttpClient.parseErrorBody(json, "<html>502 Bad Gateway</html>")
        assertNull(details.message)
        assertNull(details.code)
        assertNull(details.field)
    }

    @Test
    fun `G5 the error vocabulary is exactly the shipped nine plus RateLimitError`() {
        val source = stripComments(mainSource("errors/FeedbackKitError.kt"))
        assertTrue("denominator: the stripped source must be substantial", source.length > 1000)

        val declared = Regex("""class\s+(\w+)\s*\((?:[^()]|\([^()]*\))*\)\s*:\s*FeedbackKitError\(""")
            .findAll(source)
            .map { it.groupValues[1] }
            .toSet()

        // Set equality, not `contains`: a wrong ELEVENTH subclass must red too.
        assertEquals(
            setOf(
                "AuthenticationError", "PaymentRequiredError", "ForbiddenError", "NotFoundError",
                "ConflictError", "ValidationError", "NetworkError", "ServerError", "UnknownError",
                "RateLimitError"
            ),
            declared
        )
        assertEquals("nine shipped plus exactly one new", 10, declared.size)
    }

    // ---------------------------------------------------------------------------------
    // G6 — the deleted bucket-start field does not exist anywhere in this package
    // ---------------------------------------------------------------------------------

    @Test
    fun `G6 the deleted start-instant field appears nowhere under src`() {
        // The needles are ASSEMBLED so this test file does not itself satisfy the search it
        // performs — the scan covers src/test as well as src/main.
        val snake = "bucket" + "_start"
        val camel = "bucket" + "Start"

        val srcRoot = File(moduleDir(), "src")
        assertTrue("src root not found at ${srcRoot.absolutePath}", srcRoot.isDirectory)

        val files = srcRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("denominator: no Kotlin files were scanned", files.isNotEmpty())

        var bytes = 0L
        val offenders = mutableListOf<String>()
        for (file in files) {
            val stripped = stripComments(file.readText())
            bytes += stripped.length.toLong()
            if (stripped.contains(snake) || stripped.contains(camel)) offenders += file.name
        }

        assertTrue("denominator: the scan read nothing", bytes > 0L)
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `G6 a series point declares exactly the bucket key and its values`() {
        assertEquals(
            setOf("bucket", "values"),
            elementNamesOf(MetricSeriesPoint.serializer().descriptor)
        )
    }

    // ---------------------------------------------------------------------------------
    // G7 — the four operations, the paths, the constants, and the four vocabularies
    // ---------------------------------------------------------------------------------

    @Test
    fun `G7 the metrics API declares exactly four suspend operations`() {
        val source = strippedMainSource("api/MetricsApi.kt")

        val operations = Regex("""^\s*suspend fun (\w+)\(""", RegexOption.MULTILINE)
            .findAll(source)
            .map { it.groupValues[1] }
            .toSet()

        assertEquals(setOf("summary", "timeseries", "breakdown", "events"), operations)
        assertEquals(4, operations.size)
    }

    @Test
    fun `G7 the four endpoints are the key-family paths, un-slashed, one occurrence each`() {
        val source = strippedMainSource("api/MetricsApi.kt")

        for (endpoint in listOf(
            "metrics/summary", "metrics/timeseries", "metrics/breakdown", "metrics/events"
        )) {
            val quoted = "\"" + endpoint + "\""
            assertEquals(
                "$endpoint must appear exactly once, quoted and un-slashed",
                1,
                Regex(Regex.escape(quoted)).findAll(source).count()
            )
            assertFalse(
                "endpoints are relative; a leading slash is not the shipped convention",
                source.contains("\"/" + endpoint)
            )
        }

        // The authenticated project family needs a Bearer token this SDK never sends.
        // The needle is assembled so this assertion's own source cannot satisfy it.
        val bearerSegment = "projects" + "/"
        assertFalse("this SDK reaches only the key family", source.contains(bearerSegment))
    }

    @Test
    fun `G7 the three predefined event names are defined exactly once, in contract order`() {
        assertEquals(
            listOf("feedback_list", "feedback_detail", "submit_feedback"),
            PredefinedEventNames.ALL
        )

        val files = allMainKotlinFiles()
        assertTrue("denominator: no main sources were scanned", files.isNotEmpty())

        for (name in PredefinedEventNames.ALL) {
            val defining = files.filter { stripComments(it.readText()).contains("\"" + name + "\"") }
            assertEquals("$name must have a single definition site", 1, defining.size)
            assertEquals("PredefinedEventNames.kt", defining.single().name)
        }
    }

    @Test
    fun `G7 each vocabulary's wire tokens equal the contract's, and match its SerialNames`() {
        assertVocabulary(
            enumValues<MetricMeasure>().map { it.wireValue },
            MetricMeasure.serializer().descriptor,
            listOf(
                "feedback_count", "vote_count", "comment_count", "event_count",
                "unique_event_users", "sdk_user_count", "mrr_sum", "mrr_average", "users_with_mrr"
            )
        )
        assertVocabulary(
            enumValues<MetricDimension>().map { it.wireValue },
            MetricDimension.serializer().descriptor,
            listOf("status", "category", "event_name")
        )
        // Never `hourly`/`daily`/…: those are the server's internal case names, and sending
        // one is a 400.
        assertVocabulary(
            enumValues<MetricGranularity>().map { it.wireValue },
            MetricGranularity.serializer().descriptor,
            listOf("hour", "day", "week", "month")
        )
        assertVocabulary(
            enumValues<MetricEventScope>().map { it.wireValue },
            MetricEventScope.serializer().descriptor,
            listOf("all", "predefined_only")
        )
    }

    /**
     * `wireValue` and `@SerialName` are two separate declarations on every enum entry: a
     * drift between them is a silent 400 on the request side and a decode failure on the
     * response side. Pinned as an ORDERED list equality across all three.
     */
    private fun assertVocabulary(
        wireValues: List<String>,
        descriptor: SerialDescriptor,
        expected: List<String>
    ) {
        val serialNames = (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }
        assertEquals(expected, wireValues)
        assertEquals(expected, serialNames)
        assertEquals(expected.size, descriptor.elementsCount)
    }

    @Test
    fun `G7 the vocabularies carry exactly nine three four and two members`() {
        assertEquals(9, enumValues<MetricMeasure>().size)
        assertEquals(3, enumValues<MetricDimension>().size)
        assertEquals(4, enumValues<MetricGranularity>().size)
        assertEquals(2, enumValues<MetricEventScope>().size)
    }

    // ---------------------------------------------------------------------------------
    // G8 — the event feed's two meta blocks
    // ---------------------------------------------------------------------------------

    @Test
    fun `G8 the event feed populates both meta blocks, and they are different types`() {
        val feed = json.decodeFromJsonElement<MetricEventList>(wireCorpus()["events_page"]!!)

        assertEquals(2, feed.items.size)
        assertEquals("checkout_completed", feed.items[0].eventName)

        // Readable off `meta` proves the pagination block landed there…
        assertEquals(3, feed.meta.totalPages)
        assertEquals(137, feed.meta.total)
        assertEquals(50, feed.meta.perPage)
        assertEquals(2, feed.meta.page)

        // …and readable off `metrics` proves the window block did not.
        assertTrue(feed.metrics.retentionWindowDays > 0)
        assertEquals(MetricEventScope.ALL, feed.metrics.eventScope)
        assertNull("the feed's metrics block omits granularity", feed.metrics.granularity)
    }

    @Test
    fun `G8 putting the metrics block under meta must NOT decode`() {
        val feed = wireCorpus()["events_page"]!!.jsonObject
        val metricsBlock = feed["metrics"]!!

        // Runtime surgery on the PARSED page: swap the window block into `meta`.
        val mutated = JsonObject(feed.toMutableMap().apply { put("meta", metricsBlock) })
        assertEquals("denominator: the mutation must have replaced meta", metricsBlock, mutated["meta"])

        try {
            json.decodeFromJsonElement<MetricEventList>(mutated)
            fail("ignoreUnknownKeys must not make the wrong shape silently acceptable")
        } catch (e: MissingFieldException) {
            // expected: MetricsPaginationMeta's four required properties are all absent
        }
    }

    @Test
    fun `G8 the pagination block declares exactly the contract's four fields`() {
        assertEquals(
            setOf("total", "page", "per_page", "total_pages"),
            elementNamesOf(MetricsPaginationMeta.serializer().descriptor)
        )
    }

    // ---------------------------------------------------------------------------------
    // G9 / G10 — the client surface and the static hygiene legs
    // ---------------------------------------------------------------------------------

    @Test
    fun `G9 the SDK exposes exactly six API groups`() {
        val source = strippedMainSource("FeedbackKit.kt")
        val groups = Regex("""val (\w+): (\w+Api) = \2\(http\)""")
            .findAll(source)
            .map { it.groupValues[1] }
            .toSet()

        assertEquals(
            setOf("feedback", "votes", "comments", "users", "events", "metrics"),
            groups
        )
    }

    @Test
    fun `G10 this phase adds no logging and no hardcoded retention allowance`() {
        val added = listOf("models/Metrics.kt", "models/PredefinedEventNames.kt", "api/MetricsApi.kt")

        var bytes = 0
        for (relative in added) {
            val source = strippedMainSource(relative)
            bytes += source.length

            assertEquals(
                "$relative must not log: a metrics event_name filter is customer data",
                0,
                Regex("""\bprintln\s*\(""").findAll(source).count()
            )
            // The plan's allowance is read off `retention_window_days`; a literal here is
            // the hardcode the contract forbids.
            for (literal in listOf("30", "365")) {
                assertEquals(
                    "$relative must not carry $literal as a retention constant",
                    0,
                    Regex("""=\s*""" + literal + """\b""").findAll(source).count()
                )
            }
        }

        assertTrue("denominator: the added sources must be substantial", bytes > 3000)
    }

    @Test
    fun `G10 no metrics UI component was added`() {
        val uiDir = File(mainSourceRoot(), "ui")
        assertTrue("denominator: the ui package must exist", uiDir.isDirectory)

        val metricsUi = uiDir.listFiles()!!.filter { it.name.contains("Metric", ignoreCase = true) }
        assertEquals(emptyList<File>(), metricsUi)
    }

}
