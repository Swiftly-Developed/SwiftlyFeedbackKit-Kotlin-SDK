package com.swiftlydeveloped.feedbackkit

import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedbackCategoryTest {

    @Test
    fun `displayName returns correct values`() {
        assertEquals("Feature Request", FeedbackCategory.FEATURE_REQUEST.displayName)
        assertEquals("Bug Report", FeedbackCategory.BUG_REPORT.displayName)
        assertEquals("Improvement", FeedbackCategory.IMPROVEMENT.displayName)
        assertEquals("Other", FeedbackCategory.OTHER.displayName)
    }

    @Test
    fun `jsonValue returns correct snake_case values`() {
        assertEquals("feature_request", FeedbackCategory.FEATURE_REQUEST.jsonValue)
        assertEquals("bug_report", FeedbackCategory.BUG_REPORT.jsonValue)
        assertEquals("improvement", FeedbackCategory.IMPROVEMENT.jsonValue)
        assertEquals("other", FeedbackCategory.OTHER.jsonValue)
    }

    @Test
    fun `fromJsonValue parses valid values`() {
        assertEquals(FeedbackCategory.FEATURE_REQUEST, FeedbackCategory.fromJsonValue("feature_request"))
        assertEquals(FeedbackCategory.FEATURE_REQUEST, FeedbackCategory.fromJsonValue("featurerequest"))
        assertEquals(FeedbackCategory.BUG_REPORT, FeedbackCategory.fromJsonValue("bug_report"))
        assertEquals(FeedbackCategory.BUG_REPORT, FeedbackCategory.fromJsonValue("bugreport"))
        assertEquals(FeedbackCategory.IMPROVEMENT, FeedbackCategory.fromJsonValue("improvement"))
        assertEquals(FeedbackCategory.OTHER, FeedbackCategory.fromJsonValue("other"))
    }

    @Test
    fun `fromJsonValue returns null for invalid values`() {
        assertNull(FeedbackCategory.fromJsonValue("invalid"))
        assertNull(FeedbackCategory.fromJsonValue(""))
        assertNull(FeedbackCategory.fromJsonValue("unknown"))
    }

    @Test
    fun `fromJsonValue is case insensitive`() {
        assertEquals(FeedbackCategory.FEATURE_REQUEST, FeedbackCategory.fromJsonValue("FEATURE_REQUEST"))
        assertEquals(FeedbackCategory.BUG_REPORT, FeedbackCategory.fromJsonValue("Bug_Report"))
    }
}
