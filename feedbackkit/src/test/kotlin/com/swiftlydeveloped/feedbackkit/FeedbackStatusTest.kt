package com.swiftlydeveloped.feedbackkit

import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackStatusTest {

    @Test
    fun `canVote returns true for pending status`() {
        assertTrue(FeedbackStatus.PENDING.canVote)
    }

    @Test
    fun `canVote returns true for approved status`() {
        assertTrue(FeedbackStatus.APPROVED.canVote)
    }

    @Test
    fun `canVote returns true for in_progress status`() {
        assertTrue(FeedbackStatus.IN_PROGRESS.canVote)
    }

    @Test
    fun `canVote returns true for testflight status`() {
        assertTrue(FeedbackStatus.TESTFLIGHT.canVote)
    }

    @Test
    fun `canVote returns false for completed status`() {
        assertFalse(FeedbackStatus.COMPLETED.canVote)
    }

    @Test
    fun `canVote returns false for rejected status`() {
        assertFalse(FeedbackStatus.REJECTED.canVote)
    }

    @Test
    fun `displayName returns correct values`() {
        assertEquals("Pending", FeedbackStatus.PENDING.displayName)
        assertEquals("Approved", FeedbackStatus.APPROVED.displayName)
        assertEquals("In Progress", FeedbackStatus.IN_PROGRESS.displayName)
        assertEquals("TestFlight", FeedbackStatus.TESTFLIGHT.displayName)
        assertEquals("Completed", FeedbackStatus.COMPLETED.displayName)
        assertEquals("Rejected", FeedbackStatus.REJECTED.displayName)
    }

    @Test
    fun `jsonValue returns correct snake_case values`() {
        assertEquals("pending", FeedbackStatus.PENDING.jsonValue)
        assertEquals("approved", FeedbackStatus.APPROVED.jsonValue)
        assertEquals("in_progress", FeedbackStatus.IN_PROGRESS.jsonValue)
        assertEquals("testflight", FeedbackStatus.TESTFLIGHT.jsonValue)
        assertEquals("completed", FeedbackStatus.COMPLETED.jsonValue)
        assertEquals("rejected", FeedbackStatus.REJECTED.jsonValue)
    }

    @Test
    fun `fromJsonValue parses valid values`() {
        assertEquals(FeedbackStatus.PENDING, FeedbackStatus.fromJsonValue("pending"))
        assertEquals(FeedbackStatus.APPROVED, FeedbackStatus.fromJsonValue("approved"))
        assertEquals(FeedbackStatus.IN_PROGRESS, FeedbackStatus.fromJsonValue("in_progress"))
        assertEquals(FeedbackStatus.IN_PROGRESS, FeedbackStatus.fromJsonValue("inprogress"))
        assertEquals(FeedbackStatus.TESTFLIGHT, FeedbackStatus.fromJsonValue("testflight"))
        assertEquals(FeedbackStatus.COMPLETED, FeedbackStatus.fromJsonValue("completed"))
        assertEquals(FeedbackStatus.REJECTED, FeedbackStatus.fromJsonValue("rejected"))
    }

    @Test
    fun `fromJsonValue returns null for invalid values`() {
        assertNull(FeedbackStatus.fromJsonValue("invalid"))
        assertNull(FeedbackStatus.fromJsonValue(""))
        assertNull(FeedbackStatus.fromJsonValue("unknown"))
    }

    @Test
    fun `fromJsonValue is case insensitive`() {
        assertEquals(FeedbackStatus.PENDING, FeedbackStatus.fromJsonValue("PENDING"))
        assertEquals(FeedbackStatus.APPROVED, FeedbackStatus.fromJsonValue("Approved"))
        assertEquals(FeedbackStatus.IN_PROGRESS, FeedbackStatus.fromJsonValue("IN_PROGRESS"))
    }
}
