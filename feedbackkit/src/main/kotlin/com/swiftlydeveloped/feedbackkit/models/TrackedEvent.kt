package com.swiftlydeveloped.feedbackkit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an event to track.
 */
@Serializable
data class TrackedEvent(
    val name: String,
    val properties: Map<String, String>? = null,
    @SerialName("user_id")
    val userId: String? = null,
    val timestamp: String? = null
)

/**
 * Response from tracking an event.
 */
@Serializable
data class TrackEventResponse(
    val success: Boolean,
    @SerialName("event_id")
    val eventId: String? = null
)
