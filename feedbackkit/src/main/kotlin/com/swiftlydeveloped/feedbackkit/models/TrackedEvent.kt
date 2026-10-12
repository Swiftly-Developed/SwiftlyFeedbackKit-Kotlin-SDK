package com.swiftlydeveloped.feedbackkit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an event to track.
 */
@Serializable
data class TrackedEvent(
    /** Wire key is `event_name` (`TrackViewEventDTO.eventName`). The Kotlin name stays `name`. */
    @SerialName("event_name")
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
    /**
     * The server's event payload has never carried `success`. A successful HTTP
     * response is the success signal.
     */
    val success: Boolean = true,
    @SerialName("event_id")
    val eventId: String? = null,
    val id: String? = null,
    @SerialName("event_name")
    val eventName: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    val properties: Map<String, String>? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
