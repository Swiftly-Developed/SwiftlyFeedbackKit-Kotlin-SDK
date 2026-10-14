package com.swiftlydeveloped.feedbackkit

import com.swiftlydeveloped.feedbackkit.models.FeedbackSortOption
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedbackSortOptionTest {

    @Test
    fun `the four sorts match the other SDKs and default to votes`() {
        assertEquals(listOf("VOTES", "NEWEST", "OLDEST", "COMMENTS"), FeedbackSortOption.entries.map { it.name })
        assertEquals(FeedbackSortOption.VOTES, FeedbackSortOption.DEFAULT)
    }

    @Test
    fun `stored names round trip and unknown values fall back to the default`() {
        FeedbackSortOption.entries.forEach {
            assertEquals(it, FeedbackSortOption.fromStoredName(it.name))
        }
        assertEquals(FeedbackSortOption.DEFAULT, FeedbackSortOption.fromStoredName(null))
        assertEquals(FeedbackSortOption.DEFAULT, FeedbackSortOption.fromStoredName("votes"))
        assertEquals(FeedbackSortOption.DEFAULT, FeedbackSortOption.fromStoredName("TRENDING"))
    }

    @Test
    fun `every sort has its own label`() {
        assertEquals(4, FeedbackSortOption.entries.map { it.displayNameRes }.toSet().size)
    }
}
