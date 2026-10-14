package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/** Fill for skeleton shapes: a faint wash of the content colour, legible in light and dark. */
@Composable
internal fun placeholderColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

/**
 * Text that can render as a skeleton. While [placeholder] is true the text is laid out as
 * normal but drawn as one rounded bar per line, so the skeleton has exactly the real card's
 * line breaks and height and nothing shifts when content arrives.
 */
@Composable
internal fun ShowcaseText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    placeholder: Boolean = false
) {
    if (!placeholder) {
        Text(text = text, style = style, color = color, modifier = modifier)
        return
    }
    val barColor = placeholderColor()
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        style = style,
        color = Color.Transparent,
        onTextLayout = { layout = it },
        modifier = modifier.drawBehind {
            val result = layout ?: return@drawBehind
            val inset = 3.dp.toPx()
            val radius = CornerRadius(4.dp.toPx())
            for (line in 0 until result.lineCount) {
                val left = result.getLineLeft(line)
                val right = result.getLineRight(line)
                val top = result.getLineTop(line) + inset
                val bottom = result.getLineBottom(line) - inset
                if (right <= left || bottom <= top) continue
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    cornerRadius = radius
                )
            }
        }
    )
}
