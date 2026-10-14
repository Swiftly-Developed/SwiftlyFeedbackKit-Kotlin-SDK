package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.getfeedbackkit.feedbackkit.R
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackSortOption
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.state.FeedbackListState
import com.swiftlydeveloped.feedbackkit.state.LocalFeedbackKitTheme

internal const val SORT_FILTER_SHEET_TAG = "fbk-list-sort-filter-sheet"

/**
 * Body of the Sort & Filter bottom sheet (FEEDBACK01 §6.6): Sort by, Status and Category, each
 * a single-choice group of filter chips, and Clear filters while anything is selected.
 * Choices apply at once; the list behind the sheet updates as the reader taps.
 */
@Composable
internal fun SortFilterSheetContent(
    state: FeedbackListState,
    showStatusFilter: Boolean,
    showCategoryFilter: Boolean,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalFeedbackKitTheme.current.statusColors
    val chipContainer = MaterialTheme.colorScheme.surfaceContainerLow
    val glyphSize = iconSizeFor(MaterialTheme.typography.labelLarge.fontSize, 1.25f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SORT_FILTER_SHEET_TAG)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.feedbackkit_sort_and_filter),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            if (state.isFiltered) {
                TextButton(onClick = state::clearSelectedFilters) {
                    Text(stringResource(R.string.feedbackkit_filter_clear_all))
                }
            }
        }

        SheetSection(stringResource(R.string.feedbackkit_sort_by)) {
            FeedbackSortOption.entries.forEach { option ->
                ChoiceChip(
                    label = stringResource(option.displayNameRes),
                    selected = state.sortOption == option,
                    onClick = { state.selectSort(option) },
                    iconSize = glyphSize
                )
            }
        }

        if (showStatusFilter) {
            SheetSection(stringResource(R.string.feedbackkit_filter_status)) {
                ChoiceChip(
                    label = stringResource(R.string.feedbackkit_filter_all_statuses),
                    selected = state.selectedStatus == null,
                    onClick = { state.selectStatus(null) },
                    iconSize = glyphSize
                )
                FeedbackStatus.entries.forEach { status ->
                    val color = readableOn(statusColors.forStatus(status), chipContainer)
                    ChoiceChip(
                        label = stringResource(status.displayNameRes),
                        selected = state.selectedStatus == status,
                        onClick = { state.selectStatus(status) },
                        leadingIcon = { StatusGlyph(status, color, glyphSize) }
                    )
                }
            }
        }

        if (showCategoryFilter) {
            SheetSection(stringResource(R.string.feedbackkit_filter_category)) {
                ChoiceChip(
                    label = stringResource(R.string.feedbackkit_filter_all_categories),
                    selected = state.selectedCategory == null,
                    onClick = { state.selectCategory(null) },
                    iconSize = glyphSize
                )
                FeedbackCategory.entries.forEach { category ->
                    ChoiceChip(
                        label = stringResource(category.pluralDisplayNameRes),
                        selected = state.selectedCategory == category,
                        onClick = { state.selectCategory(category) },
                        leadingIcon = {
                            Icon(category.icon, contentDescription = null, modifier = Modifier.size(glyphSize))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetSection(title: String, chips: @Composable () -> Unit) {
    Spacer(Modifier.height(20.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() }
    )
    Spacer(Modifier.height(8.dp))
    WrappingRow(
        horizontalSpacing = 8.dp,
        verticalSpacing = 0.dp,
        modifier = Modifier.selectableGroup()
    ) { chips() }
}

/**
 * One option in a single-choice group. TalkBack announces it as a radio button, "selected"
 * or "not selected". With no glyph of its own it shows a check while selected, as Material
 * filter chips do.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    iconSize: Dp = FilterChipDefaults.IconSize,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.semantics { role = Role.RadioButton },
        leadingIcon = leadingIcon ?: if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(iconSize)) }
        } else {
            null
        }
    )
}
