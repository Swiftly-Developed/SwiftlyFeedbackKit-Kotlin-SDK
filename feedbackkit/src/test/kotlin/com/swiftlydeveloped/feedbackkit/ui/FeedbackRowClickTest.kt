package com.swiftlydeveloped.feedbackkit.ui

import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedbackRowClickTest {

    @Test
    fun `a row with no click callback is not clickable`() {
        val feedback = sample()

        assertNull(feedbackRowClick(null, feedback))

        var clickedId: String? = null
        val click = feedbackRowClick({ clickedId = it.id }, feedback)
        click!!.invoke()
        assertEquals("row-1", clickedId)
    }

    private fun sample() = Feedback(
        id = "row-1",
        title = "title",
        description = "description",
        status = FeedbackStatus.PENDING,
        category = FeedbackCategory.OTHER,
        voteCount = 0,
        hasVoted = false,
        commentCount = 0,
        createdAt = "2023-11-15T00:13:20Z"
    )
}
