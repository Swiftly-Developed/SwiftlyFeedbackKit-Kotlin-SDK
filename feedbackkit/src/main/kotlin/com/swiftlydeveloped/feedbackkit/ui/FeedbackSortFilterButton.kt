package com.swiftlydeveloped.feedbackkit.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.getfeedbackkit.feedbackkit.R
import com.swiftlydeveloped.feedbackkit.state.FeedbackListState
import com.swiftlydeveloped.feedbackkit.ui.showcase.SortFilterSheetContent
import com.swiftlydeveloped.feedbackkit.ui.showcase.filterNames

internal const val SORT_FILTER_BUTTON_TAG = "fbk-list-sort-filter"

/**
 * Top app bar action that opens the Sort & Filter bottom sheet for [state].
 *
 * The icon button turns tonal while a status or category is selected. TalkBack reads
 * "Sort and filter" with the state "Filtered by Bug Reports, In Progress", or "Sorted by
 * Votes" when nothing is filtered. [FeedbackListScreen] places it for you; use it directly
 * when you build your own top app bar around [FeedbackList].
 *
 * @param state The list state the sheet reads and writes.
 * @param showStatusFilter Whether the sheet offers the Status filter.
 * @param showCategoryFilter Whether the sheet offers the Category filter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSortFilterButton(
    state: FeedbackListState,
    modifier: Modifier = Modifier,
    showStatusFilter: Boolean = true,
    showCategoryFilter: Boolean = true
) {
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    val label = stringResource(R.string.feedbackkit_sort_and_filter)
    val spokenFilters = filterNames(state.selectedStatus, state.selectedCategory, spoken = true)
    val stateText = if (spokenFilters != null) {
        stringResource(R.string.feedbackkit_filtered_by, spokenFilters)
    } else {
        stringResource(R.string.feedbackkit_sorted_by, stringResource(state.sortOption.displayNameRes))
    }
    val buttonModifier = modifier
        .testTag(SORT_FILTER_BUTTON_TAG)
        .semantics { stateDescription = stateText }

    if (state.isFiltered) {
        FilledTonalIconButton(onClick = { sheetOpen = true }, modifier = buttonModifier) {
            Icon(Icons.Filled.FilterList, contentDescription = label)
        }
    } else {
        IconButton(onClick = { sheetOpen = true }, modifier = buttonModifier) {
            Icon(Icons.Filled.FilterList, contentDescription = label)
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SortFilterSheetContent(
                state = state,
                showStatusFilter = showStatusFilter,
                showCategoryFilter = showCategoryFilter
            )
        }
    }
}
