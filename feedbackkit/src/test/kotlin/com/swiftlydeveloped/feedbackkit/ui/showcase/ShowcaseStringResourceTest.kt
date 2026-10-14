package com.swiftlydeveloped.feedbackkit.ui.showcase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Every Showcase string and plural exists, is non-empty and keeps its placeholders in all
 * eleven locales. Gradle runs unit tests from the module directory.
 */
class ShowcaseStringResourceTest {

    private val showcaseStrings = listOf(
        "feedbackkit_sort_and_filter", "feedbackkit_sort_by",
        "feedbackkit_sort_votes", "feedbackkit_sort_newest", "feedbackkit_sort_oldest", "feedbackkit_sort_comments",
        "feedbackkit_filter_status", "feedbackkit_filter_category",
        "feedbackkit_filter_all_statuses", "feedbackkit_filter_all_categories", "feedbackkit_filter_clear_all",
        "feedbackkit_category_plural_feature_request", "feedbackkit_category_plural_bug_report",
        "feedbackkit_category_plural_improvement", "feedbackkit_category_plural_other",
        "feedbackkit_filtered_by", "feedbackkit_filter_names_join", "feedbackkit_filter_names_join_spoken",
        "feedbackkit_filter_clear", "feedbackkit_filtered_empty", "feedbackkit_filtered_empty_description",
        "feedbackkit_search_placeholder", "feedbackkit_search_clear",
        "feedbackkit_search_empty", "feedbackkit_search_empty_description",
        "feedbackkit_vote_state_voted", "feedbackkit_vote_state_not_voted", "feedbackkit_vote_closed",
        "feedbackkit_view_details", "feedbackkit_list_loading", "feedbackkit_sorted_by", "feedbackkit_list_refreshing"
    )
    private val showcasePlurals = listOf("feedbackkit_vote_count", "feedbackkit_comment_count")
    private val placeholder = Regex("%\\d+\\$[sd]")

    private class Locale(val folder: String, val strings: Map<String, String>, val plurals: Map<String, Map<String, String>>)

    private val locales: List<Locale> by lazy {
        File("src/main/res").listFiles { f -> f.isDirectory && f.name.matches(Regex("values(-.+)?")) }!!
            .mapNotNull { dir -> File(dir, "strings.xml").takeIf { it.exists() }?.let { parse(dir.name, it) } }
            .sortedBy { it.folder }
    }

    private fun parse(folder: String, file: File): Locale {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val strings = doc.getElementsByTagName("string").let { nodes ->
            (0 until nodes.length).map { nodes.item(it) as Element }.associate { it.getAttribute("name") to it.textContent }
        }
        val plurals = doc.getElementsByTagName("plurals").let { nodes ->
            (0 until nodes.length).map { nodes.item(it) as Element }.associate { plural ->
                val items = plural.getElementsByTagName("item")
                plural.getAttribute("name") to (0 until items.length).map { items.item(it) as Element }
                    .associate { it.getAttribute("quantity") to it.textContent }
            }
        }
        return Locale(folder, strings, plurals)
    }

    @Test
    fun `all eleven locales are present`() {
        assertEquals(11, locales.size)
    }

    @Test
    fun `every showcase string exists, is non-empty and keeps its placeholders`() {
        val base = locales.single { it.folder == "values" }
        for (locale in locales) {
            for (key in showcaseStrings) {
                val value = locale.strings[key]
                assertTrue("${locale.folder} is missing $key", !value.isNullOrBlank())
                assertEquals(
                    "${locale.folder}/$key placeholders",
                    placeholder.findAll(base.strings.getValue(key)).map { it.value }.toSet(),
                    placeholder.findAll(value!!).map { it.value }.toSet()
                )
            }
        }
    }

    @Test
    fun `every showcase plural has an other form carrying the count`() {
        for (locale in locales) {
            for (key in showcasePlurals) {
                val forms = locale.plurals[key]
                assertTrue("${locale.folder} is missing $key", forms != null)
                // Only "other" must show the numeral: Arabic "one" and "two" are grammatical
                // without it ("صوت واحد", "صوتان").
                val other = forms!!["other"]
                assertTrue("${locale.folder}/$key has no other form", other != null)
                assertTrue("${locale.folder}/$key/other drops the count", other!!.contains("%1\$s"))
                forms.values.forEach { assertTrue("${locale.folder}/$key has an empty form", it.isNotBlank()) }
            }
        }
    }

    @Test
    fun `TestFlight stays a product name in every locale`() {
        locales.forEach { assertEquals(it.folder, "TestFlight", it.strings["feedbackkit_status_testflight"]) }
    }
}
