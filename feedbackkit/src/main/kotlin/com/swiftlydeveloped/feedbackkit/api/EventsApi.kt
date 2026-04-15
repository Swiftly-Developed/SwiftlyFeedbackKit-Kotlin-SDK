package com.swiftlydeveloped.feedbackkit.api

import com.swiftlydeveloped.feedbackkit.http.FeedbackKitHttpClient
import com.swiftlydeveloped.feedbackkit.models.TrackEventResponse
import com.swiftlydeveloped.feedbackkit.models.TrackedEvent
import kotlinx.serialization.encodeToString

/**
 * API for event tracking operations.
 */
class EventsApi internal constructor(
    private val http: FeedbackKitHttpClient
) {
    /**
     * Track an event.
     *
     * @param event The event to track.
     * @return The tracking response.
     */
    suspend fun track(event: TrackedEvent): TrackEventResponse {
        // Include user ID in the event if not already set
        val eventWithUserId = if (event.userId == null && http.userId != null) {
            event.copy(userId = http.userId)
        } else {
            event
        }

        val body = http.jsonSerializer.encodeToString(eventWithUserId)

        return http.post(
            endpoint = "events/track",
            body = body
        ) { responseBody ->
            http.jsonSerializer.decodeFromString<TrackEventResponse>(responseBody)
        }
    }

    /**
     * Track an event with individual parameters.
     *
     * @param name The event name.
     * @param properties Optional event properties.
     * @return The tracking response.
     */
    suspend fun track(
        name: String,
        properties: Map<String, String>? = null
    ): TrackEventResponse = track(
        TrackedEvent(
            name = name,
            properties = properties
        )
    )
}
