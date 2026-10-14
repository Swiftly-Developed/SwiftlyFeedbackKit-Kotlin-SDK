package com.swiftlydeveloped.feedbackkit.ui.showcase

import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.models.VoteResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoteControllerTest {

    @Test
    fun `a vote flips at once and settles on the server's answer`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val actions = FakeVoteActions(gate = gate).apply { seed("f", 10) }
        val controller = VoteController("f", initialHasVoted = false, initialVoteCount = 3, actions = actions)
        var applied: VoteResponse? = null

        launch { controller.toggle(onApplied = { applied = it }) }
        runCurrent()

        assertTrue(controller.hasVoted)
        assertEquals(4, controller.voteCount)
        assertTrue(controller.isBusy)

        gate.complete(Unit)
        advanceUntilIdle()

        assertTrue(controller.hasVoted)
        assertEquals(11, controller.voteCount)
        assertFalse(controller.isBusy)
        assertEquals(listOf("vote:f"), actions.calls)
        assertEquals(11, applied?.voteCount)
    }

    @Test
    fun `removing a vote calls unvote`() = runTest {
        val actions = FakeVoteActions().apply { seed("f", 5) }
        val controller = VoteController("f", initialHasVoted = true, initialVoteCount = 5, actions = actions)

        controller.toggle()

        assertEquals(listOf("unvote:f"), actions.calls)
        assertFalse(controller.hasVoted)
        assertEquals(4, controller.voteCount)
    }

    @Test
    fun `a failed vote rolls back and reports the error`() = runTest {
        val controller = VoteController("f", initialHasVoted = false, initialVoteCount = 7, actions = FakeVoteActions(fail = true))
        var failure: FeedbackKitError? = null

        controller.toggle(onFailed = { failure = it })

        assertFalse(controller.hasVoted)
        assertEquals(7, controller.voteCount)
        assertFalse(controller.isBusy)
        assertNotNull(controller.error)
        assertNotNull(failure)
    }

    @Test
    fun `a second tap while a vote is in flight is ignored`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val actions = FakeVoteActions(gate = gate)
        val controller = VoteController("f", initialHasVoted = false, initialVoteCount = 0, actions = actions)

        launch { controller.toggle() }
        runCurrent()
        controller.toggle()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("vote:f"), actions.calls)
        assertTrue(controller.hasVoted)
        assertNull(controller.error)
    }
}
