package com.swiftlydeveloped.feedbackkit

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `QA-INTEGRATION17-22`. Kotlin `UsersApi.getCurrentUser` requests
 * `GET users/{id}`. That path is not registered on the server.
 *
 * This pin fails if `getCurrentUser` stops calling `users/$userId` or starts
 * calling `users/register`. Both the client call and the server 404 are intentional.
 */
class GetCurrentUserRoutePinTest {

    @Test
    fun getCurrentUserCallsUsersByIdAndNotRegister() {
        val source = usersApiSource()
        val body = functionBody(source, "getCurrentUser")
        assertTrue(
            "getCurrentUser must keep calling users/\$userId",
            body.contains("users/\$userId")
        )
        assertFalse(
            "getCurrentUser must not call users/register",
            body.contains("users/register")
        )
    }

    private fun usersApiSource(): String {
        val relative = "src/main/kotlin/com/swiftlydeveloped/feedbackkit/api/UsersApi.kt"
        val candidates = listOf(
            File(relative),
            File("feedbackkit/$relative"),
            File("SwiftlyFeedbackKit-Kotlin/feedbackkit/$relative")
        )
        val found = candidates.firstOrNull { it.isFile }
            ?: error("UsersApi.kt not found from ${File(".").absolutePath}")
        return found.readText()
    }

    private fun functionBody(source: String, name: String): String {
        val start = source.indexOf("fun $name")
        check(start >= 0) { "$name is missing from UsersApi.kt" }
        val open = source.indexOf("{", start)
        check(open >= 0) { "$name has no body" }
        var depth = 0
        for (index in open until source.length) {
            when (source[index]) {
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) {
                        return source.substring(start, index + 1)
                    }
                }
            }
        }
        error("$name body did not close")
    }
}
