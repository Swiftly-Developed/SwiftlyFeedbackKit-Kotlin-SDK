package com.swiftlydeveloped.feedbackkit.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.getfeedbackkit.feedbackkit.R
import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.VoteResponse
import com.swiftlydeveloped.feedbackkit.state.FeedbackListState
import com.swiftlydeveloped.feedbackkit.state.rememberFeedbackListState

/**
 * A complete feedback screen: a top app bar with the title and [FeedbackSortFilterButton], a
 * pinned [FeedbackListSearchField], the [FeedbackList], and an optional add button.
 *
 * Built for edge-to-edge: the bars draw behind the system bars and the list scrolls under
 * the navigation bar. Colours come from your [MaterialTheme], so dynamic colour and dark
 * theme follow the app.
 *
 * @param state State holder for the feedback list.
 * @param onFeedbackClick Called when a card is tapped, typically to open the detail.
 * @param onAddFeedback Shows a floating action button that calls this, typically to open
 *   [SubmitFeedbackView]. No button when null.
 * @param onBack Shows a back button in the top app bar when not null.
 * @param onVoteChange Called after a vote is applied.
 * @param onVoteError Called when a vote fails.
 * @param listStyle [FeedbackListStyle.SHOWCASE] (default) or [FeedbackListStyle.CLASSIC].
 * @param showStatusFilter Whether the Sort & Filter sheet offers the Status filter.
 * @param showCategoryFilter Whether the Sort & Filter sheet offers the Category filter.
 * @param title Top app bar title. Defaults to the localized "Feedback".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackListScreen(
    modifier: Modifier = Modifier,
    state: FeedbackListState = rememberFeedbackListState(),
    onFeedbackClick: ((Feedback) -> Unit)? = null,
    onAddFeedback: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onVoteChange: ((Feedback, VoteResponse) -> Unit)? = null,
    onVoteError: ((FeedbackKitError) -> Unit)? = null,
    listStyle: FeedbackListStyle = FeedbackListStyle.SHOWCASE,
    showStatusFilter: Boolean = true,
    showCategoryFilter: Boolean = true,
    title: String = stringResource(R.string.feedbackkit_list_title)
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val colors = MaterialTheme.colorScheme
    val barColor by animateColorAsState(
        targetValue = if (scrollBehavior.state.overlappedFraction > 0.01f) colors.surfaceContainer else colors.surface,
        label = "feedbackkit-top-bar"
    )

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column(Modifier.background(barColor)) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.feedbackkit_back)
                                )
                            }
                        }
                    },
                    actions = {
                        FeedbackSortFilterButton(
                            state = state,
                            showStatusFilter = showStatusFilter,
                            showCategoryFilter = showCategoryFilter
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    scrollBehavior = scrollBehavior
                )
                FeedbackListSearchField(
                    state = state,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
            }
        },
        floatingActionButton = {
            if (onAddFeedback != null) {
                FloatingActionButton(onClick = onAddFeedback) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.feedbackkit_submit_title))
                }
            }
        }
    ) { innerPadding ->
        FeedbackList(
            state = state,
            onFeedbackClick = onFeedbackClick,
            onVoteChange = onVoteChange,
            onVoteError = onVoteError,
            listStyle = listStyle,
            contentPadding = withFabClearance(innerPadding, hasFab = onAddFeedback != null)
        )
    }
}

/** Room under the last card so the floating action button never covers its vote capsule. */
@Composable
private fun withFabClearance(padding: PaddingValues, hasFab: Boolean): PaddingValues {
    if (!hasFab) return padding
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = padding.calculateStartPadding(direction),
        top = padding.calculateTopPadding(),
        end = padding.calculateEndPadding(direction),
        bottom = padding.calculateBottomPadding() + FabClearance
    )
}

private val FabClearance = 72.dp
