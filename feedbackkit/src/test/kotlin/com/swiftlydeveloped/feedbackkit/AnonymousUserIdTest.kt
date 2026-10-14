package com.swiftlydeveloped.feedbackkit

import android.content.Context
import android.content.SharedPreferences
import com.swiftlydeveloped.feedbackkit.models.CreateCommentRequest
import com.swiftlydeveloped.feedbackkit.models.CreateFeedbackRequest
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Anonymous ids match Flutter: `local_` plus a lowercase UUID v4, stored under
 * `feedbackkit_user_id`. An explicit id replaces that value and is what the
 * list, submit, vote, and comment calls send.
 */
class AnonymousUserIdTest {

    private val localUserId = Regex(
        "^local_[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
    )

    private lateinit var server: MockWebServer
    private lateinit var stored: MutableMap<String, String>
    private lateinit var context: Context
    private var prefsName: String? = null
    private var prefsMode: Int? = null

    @Before
    fun setUp() {
        FeedbackKit.reset()
        FeedbackKit.legacyUserIdReader = { null }
        server = MockWebServer()
        server.start()
        stored = mutableMapOf()
        prefsName = null
        prefsMode = null
        context = memoryContext(stored)
    }

    @After
    fun tearDown() {
        FeedbackKit.reset()
        FeedbackKit.legacyUserIdReader = FeedbackKit.defaultLegacyUserIdReader
        server.shutdown()
    }

    @Test
    fun `configure without a user id persists one local uuid and the next launch reuses it`() {
        val first = FeedbackKit.configure(context, config())
        val generated = first.userId

        assertTrue(generated?.matches(localUserId) == true)
        assertEquals("feedbackkit_user_id", prefsName)
        assertEquals(Context.MODE_PRIVATE, prefsMode)
        assertEquals(generated, stored["feedbackkit_user_id"])

        FeedbackKit.reset()
        val second = FeedbackKit.configure(context, config())

        assertEquals(generated, second.userId)
        assertEquals(generated, stored["feedbackkit_user_id"])
    }

    @Test
    fun `an explicit user id wins over a stored id and is persisted`() {
        stored["feedbackkit_user_id"] = "stored-user"

        val kit = FeedbackKit.configure(
            context,
            config(userId = "  explicit-user  ")
        )

        assertEquals("explicit-user", kit.userId)
        assertEquals("explicit-user", stored["feedbackkit_user_id"])
    }

    @Test
    fun `assigning userId persists the override and a blank value clears it`() {
        val kit = FeedbackKit.configure(context, config())
        val generated = kit.userId
        assertTrue(generated?.matches(localUserId) == true)

        kit.userId = "  host-user  "

        assertEquals("host-user", kit.userId)
        assertEquals("host-user", stored["feedbackkit_user_id"])

        kit.userId = "   "

        assertNull(kit.userId)
        assertNull(stored["feedbackkit_user_id"])
        assertTrue(generated != kit.userId)
    }

    @Test
    fun `configure migrates a DataStore user id into SharedPreferences and does not generate a new one`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        FeedbackKit.legacyUserIdReader = { "  legacy-voter  " }

        val kit = FeedbackKit.configure(context, config())

        assertEquals("legacy-voter", kit.userId)
        assertEquals("legacy-voter", stored["feedbackkit_user_id"])

        runBlocking { kit.feedback.list() }
        assertRequestCarriesUser("GET", "/api/v1/feedbacks", "legacy-voter", body = false)

        var laterReads = 0
        FeedbackKit.legacyUserIdReader = {
            laterReads += 1
            "other-user"
        }
        FeedbackKit.reset()
        val again = FeedbackKit.configure(context, config())

        assertEquals("legacy-voter", again.userId)
        assertEquals("legacy-voter", stored["feedbackkit_user_id"])
        assertEquals(0, laterReads)
    }

    @Test
    fun `SharedPreferences wins over a DataStore user id`() {
        stored["feedbackkit_user_id"] = "prefs-user"
        var reads = 0
        FeedbackKit.legacyUserIdReader = {
            reads += 1
            "legacy-voter"
        }

        val kit = FeedbackKit.configure(context, config())

        assertEquals("prefs-user", kit.userId)
        assertEquals("prefs-user", stored["feedbackkit_user_id"])
        assertEquals(0, reads)
    }

    @Test
    fun `an explicit user id wins over a DataStore user id`() {
        var reads = 0
        FeedbackKit.legacyUserIdReader = {
            reads += 1
            "legacy-voter"
        }

        val kit = FeedbackKit.configure(context, config(userId = "explicit-user"))

        assertEquals("explicit-user", kit.userId)
        assertEquals("explicit-user", stored["feedbackkit_user_id"])
        assertEquals(0, reads)
    }

    @Test
    fun `a blank DataStore user id still generates a local id`() {
        FeedbackKit.legacyUserIdReader = { "   " }

        val kit = FeedbackKit.configure(context, config())

        assertTrue(kit.userId?.matches(localUserId) == true)
        assertEquals(kit.userId, stored["feedbackkit_user_id"])
    }

    @Test
    fun `a blank configured id is treated as absent`() {
        val kit = FeedbackKit.configure(context, config(userId = "   "))

        assertTrue(kit.userId?.matches(localUserId) == true)
        assertEquals(kit.userId, stored["feedbackkit_user_id"])
    }

    @Test
    fun `list submit vote and comment send the persisted id`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        server.enqueue(MockResponse().setResponseCode(200).setBody(FEEDBACK_JSON))
        server.enqueue(MockResponse().setResponseCode(200).setBody(VOTE_JSON))
        server.enqueue(MockResponse().setResponseCode(200).setBody(UNVOTE_JSON))
        server.enqueue(MockResponse().setResponseCode(200).setBody(COMMENT_JSON))

        val kit = FeedbackKit.configure(context, config())
        val userId = kit.userId
        assertTrue(userId?.matches(localUserId) == true)

        runBlocking {
            kit.feedback.list()
            kit.feedback.create(
                CreateFeedbackRequest(
                    title = "Dark mode",
                    description = "Please",
                    category = FeedbackCategory.FEATURE_REQUEST
                )
            )
            kit.votes.vote("fb-1")
            kit.votes.unvote("fb-1")
            kit.comments.create("fb-1", CreateCommentRequest(content = "hi"))
        }

        assertRequestCarriesUser("GET", "/api/v1/feedbacks", userId!!, body = false)
        assertRequestCarriesUser("POST", "/api/v1/feedbacks", userId, body = true)
        assertRequestCarriesUser("POST", "/api/v1/feedbacks/fb-1/votes", userId, body = true)
        assertRequestCarriesUser("DELETE", "/api/v1/feedbacks/fb-1/votes", userId, body = true)
        assertRequestCarriesUser("POST", "/api/v1/feedbacks/fb-1/comments", userId, body = true)
    }

    private fun assertRequestCarriesUser(
        method: String,
        path: String,
        userId: String,
        body: Boolean
    ) {
        val recorded = server.takeRequest()
        assertEquals(method, recorded.method)
        assertEquals(path, recorded.requestUrl?.encodedPath)
        assertEquals(userId, recorded.getHeader("X-User-Id"))
        if (body) {
            val json = Json.parseToJsonElement(recorded.body.readUtf8()).jsonObject
            assertEquals(userId, json["user_id"]?.jsonPrimitive?.content)
        } else {
            assertEquals(userId, recorded.requestUrl?.queryParameter("user_id"))
        }
    }

    private fun config(userId: String? = null) = FeedbackKitConfig(
        apiKey = "test-key",
        baseUrl = server.url("/").toString().trimEnd('/'),
        userId = userId
    )

    private fun memoryContext(backing: MutableMap<String, String>): Context {
        val editor = mockk<SharedPreferences.Editor>()
        every { editor.putString(any(), any()) } answers {
            backing[firstArg()] = secondArg()
            editor
        }
        every { editor.remove(any()) } answers {
            backing.remove(firstArg<String>())
            editor
        }
        every { editor.commit() } returns true

        val prefs = mockk<SharedPreferences>()
        every { prefs.getString(any(), any()) } answers {
            backing[firstArg()] ?: secondArg()
        }
        every { prefs.edit() } returns editor

        val context = mockk<Context>()
        every { context.applicationContext } returns context
        every { context.getSharedPreferences(any(), any()) } answers {
            prefsName = firstArg()
            prefsMode = secondArg()
            prefs
        }
        return context
    }

    private companion object {
        const val FEEDBACK_JSON =
            """{"id":"fb-1","title":"Dark mode","description":"Please","status":"pending","category":"feature_request","vote_count":1,"has_voted":false,"comment_count":0,"created_at":"2026-10-11T00:00:00Z"}"""
        const val VOTE_JSON = """{"vote_count":2,"has_voted":true,"feedback_id":"fb-1"}"""
        const val UNVOTE_JSON = """{"vote_count":1,"has_voted":false,"feedback_id":"fb-1"}"""
        const val COMMENT_JSON =
            """{"id":"c-1","content":"hi","created_at":"2026-10-11T00:00:00Z"}"""
    }
}
