package com.swiftlydeveloped.feedbackkit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfeedbackkit.feedbackkit.R
import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.errors.localizedUserMessage
import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.models.VoteResponse
import com.swiftlydeveloped.feedbackkit.state.FeedbackListState
import com.swiftlydeveloped.feedbackkit.state.LocalFeedbackKitTheme
import com.swiftlydeveloped.feedbackkit.state.rememberFeedbackListState
import com.swiftlydeveloped.feedbackkit.theme.FeedbackKitTheme
import com.swiftlydeveloped.feedbackkit.ui.showcase.FeedbackFilterSummaryRow
import com.swiftlydeveloped.feedbackkit.ui.showcase.ShowcaseCardSpacing
import com.swiftlydeveloped.feedbackkit.ui.showcase.ShowcaseEmptyState
import com.swiftlydeveloped.feedbackkit.ui.showcase.ShowcaseErrorState
import com.swiftlydeveloped.feedbackkit.ui.showcase.ShowcaseFeedbackCard
import com.swiftlydeveloped.feedbackkit.ui.showcase.ShowcaseFilteredEmptyState
import com.swiftlydeveloped.feedbackkit.ui.showcase.ShowcaseListPadding
import com.swiftlydeveloped.feedbackkit.ui.showcase.ShowcaseSearchEmptyState
import com.swiftlydeveloped.feedbackkit.ui.showcase.ShowcaseSkeletonList

internal const val FEEDBACK_LIST_TAG = "fbk-list"
internal const val REFRESH_INDICATOR_TAG = "fbk-list-refreshing"

/** Lets the default empty, error and loading slots render in the list's style. */
internal val LocalFeedbackListStyle = staticCompositionLocalOf { FeedbackListStyle.SHOWCASE }
private val LocalListContentPadding = staticCompositionLocalOf { PaddingValues(0.dp) }

/**
 * List composable that displays feedback items.
 *
 * Items are shown after the reader's search, status and category choices and the sort on
 * [state] ([FeedbackListState.displayedItems]). A "Filtered by … Clear" row heads the list
 * while a status or category is selected. Pair it with [FeedbackSortFilterButton] and
 * [FeedbackListSearchField], or use [FeedbackListScreen], which places both.
 *
 * @param modifier Modifier for the list.
 * @param state State holder for the feedback list.
 * @param onFeedbackClick Callback when a feedback item is clicked.
 * @param onVoteChange Callback when a vote changes.
 * @param theme Optional theme override. Showcase reads only its status colours and takes
 *   everything else from [MaterialTheme]; Classic uses all of it.
 * @param emptyContent Custom content to show when the list is empty.
 * @param errorContent Custom content to show when there's an error.
 * @param loadingContent Custom content to show while loading.
 * @param listStyle [FeedbackListStyle.SHOWCASE] (default) or [FeedbackListStyle.CLASSIC].
 * @param contentPadding Extra padding around the content, for example a Scaffold's inner
 *   padding in an edge-to-edge layout. The list still scrolls under it.
 */
@Composable
fun FeedbackList(
    modifier: Modifier = Modifier,
    state: FeedbackListState = rememberFeedbackListState(),
    onFeedbackClick: ((Feedback) -> Unit)? = null,
    onVoteChange: ((Feedback, VoteResponse) -> Unit)? = null,
    theme: FeedbackKitTheme = LocalFeedbackKitTheme.current,
    emptyContent: @Composable () -> Unit = { DefaultEmptyContent(theme) },
    errorContent: @Composable (FeedbackKitError) -> Unit = { error ->
        DefaultErrorContent(error = error, onRetry = { state.load() }, theme = theme)
    },
    loadingContent: @Composable () -> Unit = { DefaultLoadingContent(theme) },
    onVoteError: ((FeedbackKitError) -> Unit)? = null,
    listStyle: FeedbackListStyle = FeedbackListStyle.SHOWCASE,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    CompositionLocalProvider(
        LocalFeedbackListStyle provides listStyle,
        LocalListContentPadding provides contentPadding
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            when {
                state.isLoading && state.feedbackItems.isEmpty() -> loadingContent()
                state.error != null && state.feedbackItems.isEmpty() -> errorContent(state.error!!)
                state.feedbackItems.isEmpty() -> emptyContent()
                else -> FeedbackListItems(
                    state = state,
                    listStyle = listStyle,
                    theme = theme,
                    contentPadding = contentPadding,
                    onFeedbackClick = onFeedbackClick,
                    onVoteChange = onVoteChange,
                    onVoteError = onVoteError
                )
            }

            if (state.isRefreshing) {
                RefreshIndicator(
                    listStyle = listStyle,
                    theme = theme,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(contentPadding)
                        .padding(top = 16.dp)
                )
            }
        }
    }
}

/**
 * Simplified FeedbackList that manages its own state.
 *
 * @param listStyle [FeedbackListStyle.SHOWCASE] (default) or [FeedbackListStyle.CLASSIC].
 * @param contentPadding Extra padding around the content.
 */
@Composable
fun FeedbackList(
    modifier: Modifier = Modifier,
    statusFilter: FeedbackStatus? = null,
    categoryFilter: FeedbackCategory? = null,
    onFeedbackClick: ((Feedback) -> Unit)? = null,
    theme: FeedbackKitTheme = LocalFeedbackKitTheme.current,
    onVoteError: ((FeedbackKitError) -> Unit)? = null,
    listStyle: FeedbackListStyle = FeedbackListStyle.SHOWCASE,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val state = rememberFeedbackListState(
        initialStatusFilter = statusFilter,
        initialCategoryFilter = categoryFilter
    )
    LaunchedEffect(statusFilter, categoryFilter) {
        state.syncExternalFilters(statusFilter, categoryFilter)
    }

    FeedbackList(
        modifier = modifier,
        state = state,
        onFeedbackClick = onFeedbackClick,
        theme = theme,
        onVoteError = onVoteError,
        listStyle = listStyle,
        contentPadding = contentPadding
    )
}

@Deprecated("Binary compatibility with 1.x. Source callers resolve to the overload with listStyle.", level = DeprecationLevel.HIDDEN)
@Composable
fun FeedbackList(
    modifier: Modifier = Modifier,
    state: FeedbackListState = rememberFeedbackListState(),
    onFeedbackClick: ((Feedback) -> Unit)? = null,
    onVoteChange: ((Feedback, VoteResponse) -> Unit)? = null,
    theme: FeedbackKitTheme = LocalFeedbackKitTheme.current,
    emptyContent: @Composable () -> Unit = { DefaultEmptyContent(theme) },
    errorContent: @Composable (FeedbackKitError) -> Unit = { error ->
        DefaultErrorContent(error = error, onRetry = { state.load() }, theme = theme)
    },
    loadingContent: @Composable () -> Unit = { DefaultLoadingContent(theme) },
    onVoteError: ((FeedbackKitError) -> Unit)? = null
) {
    FeedbackList(
        modifier = modifier,
        state = state,
        onFeedbackClick = onFeedbackClick,
        onVoteChange = onVoteChange,
        theme = theme,
        emptyContent = emptyContent,
        errorContent = errorContent,
        loadingContent = loadingContent,
        onVoteError = onVoteError,
        listStyle = FeedbackListStyle.SHOWCASE
    )
}

@Deprecated("Binary compatibility with 1.x. Source callers resolve to the overload with listStyle.", level = DeprecationLevel.HIDDEN)
@Composable
fun FeedbackList(
    modifier: Modifier = Modifier,
    statusFilter: FeedbackStatus? = null,
    categoryFilter: FeedbackCategory? = null,
    onFeedbackClick: ((Feedback) -> Unit)? = null,
    theme: FeedbackKitTheme = LocalFeedbackKitTheme.current,
    onVoteError: ((FeedbackKitError) -> Unit)? = null
) {
    FeedbackList(
        modifier = modifier,
        statusFilter = statusFilter,
        categoryFilter = categoryFilter,
        onFeedbackClick = onFeedbackClick,
        theme = theme,
        onVoteError = onVoteError,
        listStyle = FeedbackListStyle.SHOWCASE
    )
}

@Composable
private fun FeedbackListItems(
    state: FeedbackListState,
    listStyle: FeedbackListStyle,
    theme: FeedbackKitTheme,
    contentPadding: PaddingValues,
    onFeedbackClick: ((Feedback) -> Unit)?,
    onVoteChange: ((Feedback, VoteResponse) -> Unit)?,
    onVoteError: ((FeedbackKitError) -> Unit)?
) {
    val showcase = listStyle == FeedbackListStyle.SHOWCASE
    val stylePadding = if (showcase) ShowcaseListPadding else PaddingValues(theme.spacing.dp * 2)
    val spacing = if (showcase) ShowcaseCardSpacing else theme.spacing.dp * 2
    val displayed = state.displayedItems

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(FEEDBACK_LIST_TAG),
        contentPadding = sumPadding(contentPadding, stylePadding),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        if (state.isFiltered) {
            item(key = "filter-summary", contentType = "filter-summary") {
                FeedbackFilterSummaryRow(
                    status = state.selectedStatus,
                    category = state.selectedCategory,
                    onClear = state::clearSelectedFilters
                )
            }
        }
        if (displayed.isEmpty()) {
            item(key = "no-match", contentType = "no-match") {
                if (state.isFiltered) {
                    ShowcaseFilteredEmptyState(onClearFilters = state::clearSelectedFilters)
                } else {
                    ShowcaseSearchEmptyState(onClearSearch = { state.updateSearchQuery("") })
                }
            }
        }
        items(items = displayed, key = { it.id }, contentType = { "feedback" }) { feedback ->
            val onCardVoteChange: (VoteResponse) -> Unit = { response ->
                state.updateFeedback(feedback.withVote(response.hasVoted, response.voteCount))
                onVoteChange?.invoke(feedback, response)
            }
            if (showcase) {
                ShowcaseFeedbackCard(
                    feedback = feedback,
                    onClick = feedbackRowClick(onFeedbackClick, feedback),
                    onVoteChange = onCardVoteChange,
                    onVoteError = onVoteError
                )
            } else {
                FeedbackCard(
                    feedback = feedback,
                    onClick = feedbackRowClick(onFeedbackClick, feedback),
                    onVoteChange = onCardVoteChange,
                    theme = theme,
                    onVoteError = onVoteError
                )
            }
        }
    }
}

@Composable
private fun RefreshIndicator(listStyle: FeedbackListStyle, theme: FeedbackKitTheme, modifier: Modifier) {
    val label = stringResource(R.string.feedbackkit_list_refreshing)
    val semanticsModifier = modifier
        .testTag(REFRESH_INDICATOR_TAG)
        .semantics { contentDescription = label }
    if (listStyle == FeedbackListStyle.SHOWCASE) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 3.dp,
            modifier = semanticsModifier
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp),
                strokeWidth = 2.5.dp
            )
        }
    } else {
        CircularProgressIndicator(modifier = semanticsModifier, color = theme.primaryColor)
    }
}

@Composable
private fun sumPadding(a: PaddingValues, b: PaddingValues): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = a.calculateStartPadding(direction) + b.calculateStartPadding(direction),
        top = a.calculateTopPadding() + b.calculateTopPadding(),
        end = a.calculateEndPadding(direction) + b.calculateEndPadding(direction),
        bottom = a.calculateBottomPadding() + b.calculateBottomPadding()
    )
}

@Composable
private fun DefaultEmptyContent(theme: FeedbackKitTheme) {
    if (LocalFeedbackListStyle.current == FeedbackListStyle.SHOWCASE) {
        ShowcaseEmptyState(contentPadding = LocalListContentPadding.current)
        return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(LocalListContentPadding.current),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(theme.spacing.dp * 4)
        ) {
            Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = null,
                tint = theme.secondaryTextColor,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(theme.spacing.dp * 2))
            Text(
                text = stringResource(R.string.feedbackkit_list_empty),
                color = theme.textColor,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(theme.spacing.dp))
            Text(
                text = stringResource(R.string.feedbackkit_list_empty_description),
                color = theme.secondaryTextColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DefaultErrorContent(
    error: FeedbackKitError,
    onRetry: () -> Unit,
    theme: FeedbackKitTheme
) {
    if (LocalFeedbackListStyle.current == FeedbackListStyle.SHOWCASE) {
        ShowcaseErrorState(error = error, onRetry = onRetry, contentPadding = LocalListContentPadding.current)
        return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(LocalListContentPadding.current),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(theme.spacing.dp * 4)
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = theme.errorColor,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(theme.spacing.dp * 2))
            Text(
                text = stringResource(R.string.feedbackkit_list_error),
                color = theme.textColor,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(theme.spacing.dp))
            Text(
                text = error.localizedUserMessage(),
                color = theme.secondaryTextColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(theme.spacing.dp * 2))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.feedbackkit_list_retry))
            }
        }
    }
}

@Composable
private fun DefaultLoadingContent(theme: FeedbackKitTheme) {
    if (LocalFeedbackListStyle.current == FeedbackListStyle.SHOWCASE) {
        ShowcaseSkeletonList(contentPadding = LocalListContentPadding.current)
        return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(LocalListContentPadding.current),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = theme.primaryColor
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedbackListEmptyPreview() {
    Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
        DefaultEmptyContent(FeedbackKitTheme.Light)
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedbackListErrorPreview() {
    Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
        DefaultErrorContent(
            error = FeedbackKitError.NetworkError("Unable to connect"),
            onRetry = {},
            theme = FeedbackKitTheme.Light
        )
    }
}
