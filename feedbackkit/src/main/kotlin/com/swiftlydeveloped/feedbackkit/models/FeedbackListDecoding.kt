package com.swiftlydeveloped.feedbackkit.models

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
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

private fun <T> isUnknownEnumToken(
    row: JsonObject,
    key: String,
    parse: (String) -> T?
): Boolean {
    val token = row[key]?.jsonPrimitive?.contentOrNull ?: return false
    return parse(token) == null
}
