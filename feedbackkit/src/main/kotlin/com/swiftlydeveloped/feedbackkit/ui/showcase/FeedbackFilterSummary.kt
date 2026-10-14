package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.getfeedbackkit.feedbackkit.R
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus

internal const val FILTER_SUMMARY_TAG = "fbk-list-filter-summary"
internal const val FILTER_SUMMARY_CLEAR_TAG = "fbk-list-filter-clear"

/**
 * The names the "Filtered by" row shows, in order: category first (plural form), then status
 * (FEEDBACK01 §6.5). Empty when nothing is selected.
 */
internal fun filterSummaryNameRes(status: FeedbackStatus?, category: FeedbackCategory?): List<Int> =
    listOfNotNull(category?.pluralDisplayNameRes, status?.displayNameRes)

/**
 * The active filter names joined for display ("Bug Reports · In Progress") or, with [spoken],
 * for TalkBack ("Bug Reports, In Progress"). The separators come from resources so a locale
 * controls them. Null when nothing is selected.
 */
@Composable
@ReadOnlyComposable
internal fun filterNames(status: FeedbackStatus?, category: FeedbackCategory?, spoken: Boolean): String? {
    // Displayed names never break inside themselves, so a wrap at large text sizes falls between
    // whole names ("Feature Requests ·" / "In Progress") rather than splitting "In / Progress".
    val names = filterSummaryNameRes(status, category).map {
        val name = stringResource(it)
        if (spoken) name else name.replace(' ', '\u00A0')
    }
    return when (names.size) {
        0 -> null
        1 -> names[0]
        else -> stringResource(
            if (spoken) R.string.feedbackkit_filter_names_join_spoken else R.string.feedbackkit_filter_names_join,
            names[0],
            names[1]
        )
    }
}

/**
 * "Filtered by **Bug Reports · In Progress**   Clear". Shown above the first card while a
 * status or category is selected. The text and icon form one TalkBack element with a
 * "Clear filters" action; the Clear button is also reachable on its own.
 */
@Composable
internal fun FeedbackFilterSummaryRow(
    status: FeedbackStatus?,
    category: FeedbackCategory?,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val names = filterNames(status, category, spoken = false) ?: return
    val spokenNames = filterNames(status, category, spoken = true) ?: names
    val template = stringResource(R.string.feedbackkit_filtered_by)
    val spokenLabel = stringResource(R.string.feedbackkit_filtered_by, spokenNames)
    val clearFilters = stringResource(R.string.feedbackkit_filter_clear_all)
    val colors = MaterialTheme.colorScheme

    val placeholderIndex = template.indexOf(ARGUMENT)
    val text = buildAnnotatedString {
        appendInlineContent(ICON_ID, "[icon]")
        append(" ")
        if (placeholderIndex < 0) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(names) }
        } else {
            append(template.substring(0, placeholderIndex))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(names) }
            append(template.substring(placeholderIndex + ARGUMENT.length))
        }
    }
    val inlineIcon = mapOf(
        ICON_ID to InlineTextContent(
            Placeholder(width = 1.4.em, height = 1.2.em, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter)
        ) {
            Icon(Icons.Filled.FilterList, contentDescription = null, tint = colors.primary)
        }
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(FILTER_SUMMARY_TAG)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = text,
            inlineContent = inlineIcon,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier
                .weight(1f)
                .alignByBaseline()
                .padding(vertical = 12.dp)
                .clearAndSetSemantics {
                    contentDescription = spokenLabel
                    customActions = listOf(CustomAccessibilityAction(clearFilters) { onClear(); true })
                }
        )
        TextButton(
            onClick = onClear,
            modifier = Modifier
                .alignByBaseline()
                .testTag(FILTER_SUMMARY_CLEAR_TAG)
                .semantics { contentDescription = clearFilters }
        ) {
            Text(stringResource(R.string.feedbackkit_filter_clear), fontWeight = FontWeight.Bold)
        }
    }
}

private const val ARGUMENT = "%1\$s"
private const val ICON_ID = "filter"
