package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * Places children left to right (start to end in RTL) and wraps to a new line when the next
 * child does not fit. Children on one line are centred vertically. Used for the filter chips
 * and the card's facts; Compose's own FlowRow is still experimental in this Compose version.
 */
@Composable
internal fun WrappingRow(
    horizontalSpacing: Dp,
    verticalSpacing: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val hSpace = horizontalSpacing.roundToPx()
        val vSpace = verticalSpacing.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }

        val lines = mutableListOf<MutableList<Placeable>>()
        var lineWidth = 0
        for (placeable in placeables) {
            val current = lines.lastOrNull()
            val needed = if (current.isNullOrEmpty()) placeable.width else lineWidth + hSpace + placeable.width
            if (current == null || (current.isNotEmpty() && constraints.hasBoundedWidth && needed > constraints.maxWidth)) {
                lines += mutableListOf(placeable)
                lineWidth = placeable.width
            } else {
                current += placeable
                lineWidth = needed
            }
        }

        val lineHeights = lines.map { line -> line.maxOf { it.height } }
        val widest = lines.maxOfOrNull { line -> line.sumOf { it.width } + hSpace * (line.size - 1) } ?: 0
        val totalHeight = lineHeights.sum() + vSpace * (lines.size - 1).coerceAtLeast(0)
        val width = widest.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = totalHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width, height) {
            var y = 0
            lines.forEachIndexed { index, line ->
                var x = 0
                val lineHeight = lineHeights[index]
                for (placeable in line) {
                    placeable.placeRelative(x, y + (lineHeight - placeable.height) / 2)
                    x += placeable.width + hSpace
                }
                y += lineHeight + vSpace
            }
        }
    }
}

/**
 * The card footer (FEEDBACK01 §6.3 item 5). With room, [facts] sit at the start and [trailing]
 * at the end, centred on each other. When the facts on one line plus the capsule do not fit,
 * the footer stacks: facts first, then the capsule, both at the start. Never truncates.
 */
@Composable
internal fun AdaptiveFooter(
    spacing: Dp,
    modifier: Modifier = Modifier,
    facts: @Composable () -> Unit,
    trailing: @Composable () -> Unit
) {
    Layout(
        content = {
            Box(Modifier.layoutId(FACTS)) { facts() }
            Box(Modifier.layoutId(TRAILING)) { trailing() }
        },
        modifier = modifier
    ) { measurables, constraints ->
        val factMeasurables = measurables.filter { it.layoutId == FACTS }
        val trailingMeasurables = measurables.filter { it.layoutId == TRAILING }
        val gap = spacing.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val trailingPlaceables = trailingMeasurables.map { it.measure(loose) }
        val trailingWidth = trailingPlaceables.maxOfOrNull { it.width } ?: 0
        val trailingHeight = trailingPlaceables.maxOfOrNull { it.height } ?: 0
        val factsSingleLineWidth = factMeasurables.maxOfOrNull { it.maxIntrinsicWidth(Constraints.Infinity) } ?: 0
        val hasFacts = factMeasurables.isNotEmpty() && factsSingleLineWidth > 0
        val hasTrailing = trailingPlaceables.isNotEmpty() && trailingWidth > 0

        val fitsOnOneRow = !constraints.hasBoundedWidth ||
            !hasFacts || !hasTrailing ||
            factsSingleLineWidth + gap + trailingWidth <= constraints.maxWidth

        if (fitsOnOneRow) {
            val factsMax = if (constraints.hasBoundedWidth && hasTrailing) {
                (constraints.maxWidth - trailingWidth - gap).coerceAtLeast(0)
            } else {
                constraints.maxWidth
            }
            val factPlaceables = factMeasurables.map { it.measure(loose.copy(maxWidth = factsMax)) }
            val factsHeight = factPlaceables.maxOfOrNull { it.height } ?: 0
            val height = maxOf(factsHeight, trailingHeight).coerceIn(constraints.minHeight, constraints.maxHeight)
            val width = if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                (factPlaceables.maxOfOrNull { it.width } ?: 0) + gap + trailingWidth
            }
            layout(width, height) {
                factPlaceables.forEach { it.placeRelative(0, (height - it.height) / 2) }
                trailingPlaceables.forEach { it.placeRelative(width - it.width, (height - it.height) / 2) }
            }
        } else {
            val factPlaceables = factMeasurables.map { it.measure(loose) }
            val factsHeight = factPlaceables.maxOfOrNull { it.height } ?: 0
            val height = (factsHeight + gap + trailingHeight).coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(constraints.maxWidth, height) {
                factPlaceables.forEach { it.placeRelative(0, 0) }
                trailingPlaceables.forEach { it.placeRelative(0, factsHeight + gap) }
            }
        }
    }
}

private const val FACTS = "facts"
private const val TRAILING = "trailing"
