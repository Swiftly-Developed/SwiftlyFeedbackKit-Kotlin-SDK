package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.models.VoteResponse
import com.swiftlydeveloped.feedbackkit.state.FeedbackListState
import com.swiftlydeveloped.feedbackkit.state.LocalFeedbackKitTheme
import com.swiftlydeveloped.feedbackkit.theme.FeedbackKitTheme
import kotlinx.coroutines.CompletableDeferred

internal fun feedback(
    id: String,
    title: String = "Title $id",
    description: String = "Description $id",
    status: FeedbackStatus = FeedbackStatus.PENDING,
    category: FeedbackCategory = FeedbackCategory.FEATURE_REQUEST,
    votes: Int = 0,
    voted: Boolean = false,
    comments: Int = 0,
    createdAt: String = "2026-10-01T10:00:00Z"
) = Feedback(
    id = id,
    title = title,
    description = description,
    status = status,
    category = category,
    voteCount = votes,
    hasVoted = voted,
    commentCount = comments,
    createdAt = createdAt
)

/** The list the screenshots render: every footer shape, a closed item and a four-digit count. */
internal val ShowcaseFixtures = listOf(
    feedback(
        "1",
        title = "Dark mode across the whole app",
        description = "The settings screen and the onboarding flow are still bright white at night.",
        status = FeedbackStatus.IN_PROGRESS,
        category = FeedbackCategory.FEATURE_REQUEST,
        votes = 1234,
        voted = true,
        comments = 18
    ),
    feedback(
        "2",
        title = "Crash when attaching a large photo",
        description = "Picking a photo over 20 MB closes the app.",
        status = FeedbackStatus.APPROVED,
        category = FeedbackCategory.BUG_REPORT,
        votes = 42,
        comments = 3
    ),
    feedback(
        "3",
        title = "Faster search",
        description = "Results take a second to appear on older phones.",
        status = FeedbackStatus.TESTFLIGHT,
        category = FeedbackCategory.IMPROVEMENT,
        votes = 7
    ),
    feedback(
        "4",
        title = "Export to CSV",
        description = "Shipped in 3.2.",
        status = FeedbackStatus.COMPLETED,
        category = FeedbackCategory.OTHER,
        votes = 96,
        comments = 1
    ),
    feedback(
        "5",
        title = "Offline mode",
        description = "Not planned for now; sync conflicts are too risky.",
        status = FeedbackStatus.REJECTED,
        category = FeedbackCategory.FEATURE_REQUEST,
        votes = 5
    ),
    feedback(
        "6",
        title = "Haptics on vote",
        description = "A light tap when the vote lands.",
        status = FeedbackStatus.PENDING,
        category = FeedbackCategory.IMPROVEMENT,
        votes = 1
    )
)

/** Records calls and answers like the server, or waits on [gate] when it is set. */
internal class FakeVoteActions(
    private val fail: Boolean = false,
    var gate: CompletableDeferred<Unit>? = null
) : VoteActions {
    val calls = mutableListOf<String>()
    private val counts = mutableMapOf<String, Int>()

    fun seed(id: String, count: Int) { counts[id] = count }

    override suspend fun vote(feedbackId: String): VoteResponse = answer("vote", feedbackId, +1, true)
    override suspend fun unvote(feedbackId: String): VoteResponse = answer("unvote", feedbackId, -1, false)

    private suspend fun answer(kind: String, id: String, delta: Int, voted: Boolean): VoteResponse {
        calls += "$kind:$id"
        gate?.await()
        if (fail) throw IllegalStateException("offline")
        val count = (counts[id] ?: 0) + delta
        counts[id] = count
        return VoteResponse(voteCount = count, hasVoted = voted, feedbackId = id)
    }
}

/** A colour scheme that is neither baseline, standing in for a wallpaper-derived one. */
internal val DynamicLikeLightScheme = lightColorScheme(
    primary = Color(0xFF3B6939),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBCF0B4),
    onPrimaryContainer = Color(0xFF002204),
    secondaryContainer = Color(0xFFD7E8CD),
    onSecondaryContainer = Color(0xFF111F0F),
    surface = Color(0xFFF7FBF1),
    surfaceContainerLow = Color(0xFFF1F5EC),
    surfaceContainer = Color(0xFFEBEFE6),
    surfaceContainerHigh = Color(0xFFE6E9E0),
    onSurface = Color(0xFF181D17),
    onSurfaceVariant = Color(0xFF424940),
    background = Color(0xFFF7FBF1)
)

/**
 * Theme, font scale and fake vote actions for one test. [fontScale] overrides the density
 * linearly, which is the worst case: Android 14's non-linear scaling grows large text less.
 */
@Composable
internal fun ShowcaseTestHost(
    dark: Boolean = false,
    fontScale: Float = 1f,
    colorScheme: ColorScheme = if (dark) darkColorScheme() else lightColorScheme(),
    voteActions: VoteActions = FakeVoteActions(),
    feedbackKitTheme: FeedbackKitTheme = if (dark) FeedbackKitTheme.Dark else FeedbackKitTheme.Light,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale),
        LocalVoteActions provides voteActions,
        LocalFeedbackKitTheme provides feedbackKitTheme
    ) {
        MaterialTheme(colorScheme = colorScheme) {
            Surface(color = MaterialTheme.colorScheme.surface) { content() }
        }
    }
}

/** A list state loaded from [items] without a server. */
@Composable
internal fun rememberFixtureState(
    items: List<Feedback> = ShowcaseFixtures,
    load: Boolean = true,
    configure: FeedbackListState.() -> Unit = {}
): FeedbackListState {
    val scope = rememberCoroutineScope()
    return remember {
        FeedbackListState(scope = scope, loadItems = { items }).also {
            it.configure()
            if (load) it.load()
        }
    }
}
