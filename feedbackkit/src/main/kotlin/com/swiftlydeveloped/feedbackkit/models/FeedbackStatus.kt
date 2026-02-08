package com.swiftlydeveloped.feedbackkit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the status of a feedback item.
 */
@Serializable
enum class FeedbackStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("approved")
    APPROVED,

    @SerialName("in_progress")
    IN_PROGRESS,

    @SerialName("testflight")
    TESTFLIGHT,

    @SerialName("completed")
    COMPLETED,

    @SerialName("rejected")
    REJECTED;

    /**
     * Whether voting is allowed for feedback with this status.
     * Voting is blocked for completed and rejected feedback.
     */
    val canVote: Boolean
        get() = this != COMPLETED && this != REJECTED

    /**
     * Human-readable display name for the status.
     */
    val displayName: String
        get() = when (this) {
            PENDING -> "Pending"
            APPROVED -> "Approved"
            IN_PROGRESS -> "In Progress"
            TESTFLIGHT -> "TestFlight"
            COMPLETED -> "Completed"
            REJECTED -> "Rejected"
        }

    /**
     * The JSON value for this status.
     */
    val jsonValue: String
        get() = when (this) {
            PENDING -> "pending"
            APPROVED -> "approved"
            IN_PROGRESS -> "in_progress"
            TESTFLIGHT -> "testflight"
            COMPLETED -> "completed"
            REJECTED -> "rejected"
        }

    companion object {
        /**
         * Parse a status from its JSON string value.
         */
        fun fromJsonValue(value: String): FeedbackStatus? = when (value.lowercase()) {
            "pending" -> PENDING
            "approved" -> APPROVED
            "in_progress", "inprogress" -> IN_PROGRESS
            "testflight" -> TESTFLIGHT
            "completed" -> COMPLETED
            "rejected" -> REJECTED
            else -> null
        }
    }
}
