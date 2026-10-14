package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.compose.ui.graphics.Color
import com.getfeedbackkit.feedbackkit.R
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.theme.FeedbackKitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShowcasePresentationTest {

    @Test
    fun `vote presentation covers every cell`() {
        for (canVote in listOf(true, false)) {
            for (hasVoted in listOf(true, false)) {
                assertEquals(VotePresentation.Placeholder, resolveVotePresentation(true, canVote, hasVoted))
            }
        }
        assertEquals(VotePresentation.Closed, resolveVotePresentation(false, canVote = false, hasVoted = true))
        assertEquals(VotePresentation.Closed, resolveVotePresentation(false, canVote = false, hasVoted = false))
        assertEquals(VotePresentation.Votable(voted = true), resolveVotePresentation(false, canVote = true, hasVoted = true))
        assertEquals(VotePresentation.Votable(voted = false), resolveVotePresentation(false, canVote = true, hasVoted = false))
    }

    @Test
    fun `a closed request never resolves to a button`() {
        FeedbackStatus.entries.filterNot { it.canVote }.forEach { status ->
            val presentation = resolveVotePresentation(false, status.canVote, hasVoted = true)
            assertFalse("$status resolved to $presentation", presentation is VotePresentation.Votable)
        }
    }

    @Test
    fun `every status has its own glyph shape`() {
        assertEquals(FeedbackStatus.entries.size, FeedbackStatus.entries.map { it.glyphShape }.toSet().size)
    }

    @Test
    fun `category icons are distinct and improvement uses tune, not an arrow`() {
        val names = FeedbackCategory.entries.map { it.icon.name }
        assertEquals(names.size, names.toSet().size)
        assertTrue(FeedbackCategory.IMPROVEMENT.icon.name, FeedbackCategory.IMPROVEMENT.icon.name.contains("Tune"))
        names.forEach { assertFalse(it, it.contains("Arrow")) }
    }

    @Test
    fun `readableOn lifts every default status colour to AA on light and dark cards`() {
        val cards = listOf(Color(0xFFF7F2FA), Color(0xFFFFFFFF), Color(0xFF1D1B20), Color(0xFF211F26))
        for (theme in listOf(FeedbackKitTheme.Light, FeedbackKitTheme.Dark)) {
            for (status in FeedbackStatus.entries) {
                for (card in cards) {
                    val ratio = contrastRatio(readableOn(theme.statusColors.forStatus(status), card), card)
                    assertTrue("$status on $card is $ratio", ratio >= 4.5f)
                }
            }
        }
    }

    @Test
    fun `readableOn keeps a colour that already passes`() {
        val navy = Color(0xFF0B2A6F)
        assertEquals(navy, readableOn(navy, Color.White))
    }

    @Test
    fun `contrast ratio spans one to twenty one`() {
        assertEquals(21f, contrastRatio(Color.Black, Color.White), 0.01f)
        assertEquals(1f, contrastRatio(Color.Red, Color.Red), 0.0001f)
    }

    @Test
    fun `filter summary names category first, then status`() {
        assertEquals(
            listOf(R.string.feedbackkit_category_plural_bug_report, R.string.feedbackkit_status_in_progress),
            filterSummaryNameRes(FeedbackStatus.IN_PROGRESS, FeedbackCategory.BUG_REPORT)
        )
        assertEquals(listOf(R.string.feedbackkit_status_rejected), filterSummaryNameRes(FeedbackStatus.REJECTED, null))
        assertTrue(filterSummaryNameRes(null, null).isEmpty())
    }
}
