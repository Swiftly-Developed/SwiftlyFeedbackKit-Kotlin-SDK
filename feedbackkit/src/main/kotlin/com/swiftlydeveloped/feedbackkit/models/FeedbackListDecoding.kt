package com.swiftlydeveloped.feedbackkit.models

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Decode a feedback list, dropping only rows whose `status` or `category` token
 * this SDK does not know.
 *
 * A raw `List<Feedback>` decode throws on the first unknown enum and the screen
 * loses every row. A missing required field on a known row still throws: that
 * is a corrupt payload, not a status the server added later.
 */
internal fun decodeFeedbackList(json: Json, payload: JsonElement): List<Feedback> {
    val rows = payload as? JsonArray
        ?: throw SerializationException("Feedback list payload is not a JSON array")
    val decoded = ArrayList<Feedback>(rows.size)
    for (row in rows) {
        val objectRow = row as? JsonObject
            ?: throw SerializationException("Feedback row is not a JSON object")
        if (isUnknownEnumToken(objectRow, "status", FeedbackStatus::fromJsonValue)) continue
        if (isUnknownEnumToken(objectRow, "category", FeedbackCategory::fromJsonValue)) continue
        decoded += json.decodeFromJsonElement(Feedback.serializer(), objectRow)
    }
    return decoded
}

internal data class DecodedFeedbackPage(
    val items: List<Feedback>,
    val page: Int,
    val perPage: Int,
    val totalPages: Int,
    val legacyArray: Boolean
)

internal fun decodeFeedbackPage(json: Json, payload: JsonElement): DecodedFeedbackPage {
    if (payload is JsonArray) {
        val items = decodeFeedbackList(json, payload)
        return DecodedFeedbackPage(items, 1, items.size, 1, true)
    }
    val objectPayload = payload as? JsonObject
        ?: throw SerializationException("Feedback list payload is not a JSON array or page")
    val rows = objectPayload["items"]
        ?: throw SerializationException("Feedback list page is missing items")
    val items = decodeFeedbackList(json, rows)
    val meta = objectPayload["meta"] as? JsonObject
    return DecodedFeedbackPage(
        items = items,
        page = meta?.get("page")?.jsonPrimitive?.intOrNull ?: 1,
        perPage = meta?.get("per_page")?.jsonPrimitive?.intOrNull ?: items.size,
        totalPages = meta?.get("total_pages")?.jsonPrimitive?.intOrNull ?: 1,
        legacyArray = false
    )
}

private fun <T> isUnknownEnumToken(
    row: JsonObject,
    key: String,
    parse: (String) -> T?
): Boolean {
    val token = row[key]?.jsonPrimitive?.contentOrNull ?: return false
    return parse(token) == null
}
