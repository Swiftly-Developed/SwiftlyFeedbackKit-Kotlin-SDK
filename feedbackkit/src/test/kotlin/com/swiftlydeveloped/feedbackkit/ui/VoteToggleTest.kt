package com.swiftlydeveloped.feedbackkit.ui

import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.models.VoteResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VoteToggleTest {

    @Test
    fun `a failed vote is reported and a cancellation is not turned into an error`() = runTest {
        val failed = toggleVote(
            currentlyVoted = false,
            feedbackId = "feedback-1",
            vote = { throw FeedbackKitError.ValidationError("User ID is required for voting") },
            unvote = { fail("unvote was called"); error("unreachable") }
        )
        assertTrue(failed is VoteToggleOutcome.Failed)
        assertTrue((failed as VoteToggleOutcome.Failed).error is FeedbackKitError.ValidationError)

        val applied = toggleVote(
            currentlyVoted = true,
            feedbackId = "feedback-1",
            vote = { fail("vote was called"); error("unreachable") },
            unvote = {
                VoteResponse(voteCount = 3, hasVoted = false, feedbackId = "feedback-1")
            }
        )
        val response = (applied as VoteToggleOutcome.Applied).response
        assertEquals(3, response.voteCount)
        assertEquals(false, response.hasVoted)

        try {
            toggleVote(
                currentlyVoted = false,
                feedbackId = "feedback-1",
                vote = { throw CancellationException("left the screen") },
                unvote = { error("unreachable") }
            )
            fail("Cancellation was swallowed")
        } catch (error: CancellationException) {
            assertEquals("left the screen", error.message)
        }
    }
}
