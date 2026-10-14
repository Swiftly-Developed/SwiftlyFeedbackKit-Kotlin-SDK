package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FilterListOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.getfeedbackkit.feedbackkit.R
import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.errors.localizedUserMessage
import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import kotlinx.coroutines.delay

internal const val SKELETON_TAG = "fbk-list-skeleton"
internal const val SKELETON_REVEAL_DELAY_MILLIS = 300L

/**
 * Loading state: the real Showcase card fed placeholder values. It appears only after a short
 * delay, so a fast load does not flash a skeleton. TalkBack reads one "Loading feedback".
 */
@Composable
internal fun ShowcaseSkeletonList(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    revealDelayMillis: Long = SKELETON_REVEAL_DELAY_MILLIS
) {
    var visible by remember { mutableStateOf(revealDelayMillis <= 0L) }
    LaunchedEffect(revealDelayMillis) {
        if (!visible) {
            delay(revealDelayMillis)
            visible = true
        }
    }
    val loading = stringResource(R.string.feedbackkit_list_loading)
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(SKELETON_TAG)
            .clearAndSetSemantics { contentDescription = loading }
            .padding(contentPadding)
            .padding(ShowcaseListPadding),
        verticalArrangement = Arrangement.spacedBy(ShowcaseCardSpacing)
    ) {
        if (visible) {
            SkeletonItems.forEach { item ->
                ShowcaseFeedbackCard(feedback = item, onClick = null, placeholder = true)
            }
        }
    }
}

/** Project-empty: nothing has been submitted yet. */
@Composable
internal fun ShowcaseEmptyState(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    ShowcaseMessageState(
        icon = Icons.Outlined.Inbox,
        title = stringResource(R.string.feedbackkit_list_empty),
        message = stringResource(R.string.feedbackkit_list_empty_description),
        contentPadding = contentPadding,
        modifier = modifier
    )
}

/** Load failed with nothing to show. */
@Composable
internal fun ShowcaseErrorState(
    error: FeedbackKitError,
    onRetry: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    ShowcaseMessageState(
        icon = Icons.Outlined.ErrorOutline,
        title = stringResource(R.string.feedbackkit_list_error),
        message = error.localizedUserMessage(),
        contentPadding = contentPadding,
        modifier = modifier,
        iconTint = MaterialTheme.colorScheme.error,
        actionLabel = stringResource(R.string.feedbackkit_list_retry),
        onAction = onRetry
    )
}

/** The selected status or category leaves nothing, with or without search text. */
@Composable
internal fun ShowcaseFilteredEmptyState(onClearFilters: () -> Unit, modifier: Modifier = Modifier) {
    ShowcaseMessageState(
        icon = Icons.Outlined.FilterListOff,
        title = stringResource(R.string.feedbackkit_filtered_empty),
        message = stringResource(R.string.feedbackkit_filtered_empty_description),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier,
        actionLabel = stringResource(R.string.feedbackkit_filter_clear_all),
        onAction = onClearFilters,
        fillHeight = false
    )
}

/** Only the search text leaves nothing. */
@Composable
internal fun ShowcaseSearchEmptyState(onClearSearch: () -> Unit, modifier: Modifier = Modifier) {
    ShowcaseMessageState(
        icon = Icons.Outlined.SearchOff,
        title = stringResource(R.string.feedbackkit_search_empty),
        message = stringResource(R.string.feedbackkit_search_empty_description),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier,
        actionLabel = stringResource(R.string.feedbackkit_search_clear),
        onAction = onClearSearch,
        fillHeight = false
    )
}

/**
 * Centred icon, heading, message and optional action. With [fillHeight] it fills the screen
 * and scrolls when 200% text no longer fits; without, it sits inside the list as one item.
 */
@Composable
private fun ShowcaseMessageState(
    icon: ImageVector,
    title: String,
    message: String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    fillHeight: Boolean = true
) {
    val body: @Composable () -> Unit = {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
    if (fillHeight) {
        BoxWithConstraints(modifier.fillMaxSize().padding(contentPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { body() }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { body() }
    }
}

internal val ShowcaseListPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
internal val ShowcaseCardSpacing = 12.dp

/** Placeholder rows for the skeleton. Never shown as text; only their line shapes are drawn. */
private val SkeletonItems = listOf(
    skeletonItem("skeleton-1", "Dark mode for the whole app", "Short description that wraps onto a second line in most widths.", 3),
    skeletonItem("skeleton-2", "Export to CSV", "A one line description.", 0),
    skeletonItem("skeleton-3", "Faster sync on slow networks please", "Another description, a bit longer, so the bars vary in length.", 1)
)

private fun skeletonItem(id: String, title: String, description: String, comments: Int) = Feedback(
    id = id,
    title = title,
    description = description,
    status = FeedbackStatus.APPROVED,
    category = FeedbackCategory.FEATURE_REQUEST,
    voteCount = 10,
    hasVoted = false,
    commentCount = comments,
    createdAt = ""
)
