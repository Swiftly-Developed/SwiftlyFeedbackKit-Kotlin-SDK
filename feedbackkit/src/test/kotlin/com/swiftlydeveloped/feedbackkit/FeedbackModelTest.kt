package com.swiftlydeveloped.feedbackkit

import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.models.ListFeedbackOptions
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackModelTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val sampleFeedback = Feedback(
        id = "123",
        title = "Test Feedback",
        description = "Test description",
        status = FeedbackStatus.PENDING,
        category = FeedbackCategory.FEATURE_REQUEST,
        voteCount = 5,
        hasVoted = false,
        commentCount = 2,
        createdAt = "2024-01-15T10:30:00Z"
    )

    @Test
    fun `canVote reflects status canVote`() {
        val pendingFeedback = sampleFeedback.copy(status = FeedbackStatus.PENDING)
        assertTrue(pendingFeedback.canVote)

        val completedFeedback = sampleFeedback.copy(status = FeedbackStatus.COMPLETED)
        assertFalse(completedFeedback.canVote)

        val rejectedFeedback = sampleFeedback.copy(status = FeedbackStatus.REJECTED)
        assertFalse(rejectedFeedback.canVote)
    }

    @Test
    fun `withVote creates copy with updated vote state`() {
        val updated = sampleFeedback.withVote(hasVoted = true, voteCount = 6)

        assertEquals(true, updated.hasVoted)
        assertEquals(6, updated.voteCount)
        // Other fields unchanged
        assertEquals(sampleFeedback.id, updated.id)
        assertEquals(sampleFeedback.title, updated.title)
        assertEquals(sampleFeedback.status, updated.status)
    }

    @Test
    fun `withCommentCount creates copy with updated count`() {
        val updated = sampleFeedback.withCommentCount(10)

        assertEquals(10, updated.commentCount)
        assertEquals(sampleFeedback.id, updated.id)
    }

    @Test
    fun `withStatus creates copy with updated status`() {
        val updated = sampleFeedback.withStatus(FeedbackStatus.APPROVED)

        assertEquals(FeedbackStatus.APPROVED, updated.status)
        assertEquals(sampleFeedback.id, updated.id)
    }

    @Test
    fun `feedback deserializes from JSON`() {
        val jsonString = """
            {
                "id": "456",
                "title": "JSON Feedback",
                "description": "From JSON",
                "status": "in_progress",
                "category": "bug_report",
                "vote_count": 10,
                "has_voted": true,
                "comment_count": 3,
                "created_at": "2024-01-20T12:00:00Z"
            }
        """.trimIndent()

        val feedback = json.decodeFromString<Feedback>(jsonString)

        assertEquals("456", feedback.id)
        assertEquals("JSON Feedback", feedback.title)
        assertEquals("From JSON", feedback.description)
        assertEquals(FeedbackStatus.IN_PROGRESS, feedback.status)
        assertEquals(FeedbackCategory.BUG_REPORT, feedback.category)
        assertEquals(10, feedback.voteCount)
        assertTrue(feedback.hasVoted)
        assertEquals(3, feedback.commentCount)
    }

    @Test
    fun `ListFeedbackOptions toQueryParams includes only non-null values`() {
        val emptyOptions = ListFeedbackOptions()
        assertTrue(emptyOptions.toQueryParams().isEmpty())

        val statusOnly = ListFeedbackOptions(status = FeedbackStatus.PENDING)
        assertEquals(mapOf("status" to "pending"), statusOnly.toQueryParams())

        val fullOptions = ListFeedbackOptions(
            status = FeedbackStatus.APPROVED,
            category = FeedbackCategory.BUG_REPORT,
            page = 2,
            perPage = 20
        )
        val params = fullOptions.toQueryParams()
        assertEquals("approved", params["status"])
        assertEquals("bug_report", params["category"])
        assertEquals("2", params["page"])
        assertEquals("20", params["per_page"])
    }
}
