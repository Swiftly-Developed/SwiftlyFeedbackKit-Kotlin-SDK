package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import kotlin.math.PI

/**
 * Status is encoded by shape as well as colour (FEEDBACK01 §6.3), so a host that sets every
 * status to one colour still gets six distinguishable glyphs. Internal on purpose: the
 * artwork is not public API.
 */
internal enum class StatusGlyphShape {
    DASHED_RING,
    RING,
    HALF_RING,
    THREE_QUARTER_RING,
    CHECK,
    CROSS
}

internal val FeedbackStatus.glyphShape: StatusGlyphShape
    get() = when (this) {
        FeedbackStatus.PENDING -> StatusGlyphShape.DASHED_RING
        FeedbackStatus.APPROVED -> StatusGlyphShape.RING
        FeedbackStatus.IN_PROGRESS -> StatusGlyphShape.HALF_RING
        FeedbackStatus.TESTFLIGHT -> StatusGlyphShape.THREE_QUARTER_RING
        FeedbackStatus.COMPLETED -> StatusGlyphShape.CHECK
        FeedbackStatus.REJECTED -> StatusGlyphShape.CROSS
    }

/**
 * Decorative status glyph. It never carries a content description because the status name
 * is always rendered next to it.
 *
 * The rings use the same 20/24 proportion as the Material filled icons used for Completed
 * and Rejected, so all six read as one family. Partial fills start at the reading edge, so
 * they mirror in right-to-left layouts.
 */
@Composable
internal fun StatusGlyph(
    status: FeedbackStatus,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    when (val shape = status.glyphShape) {
        StatusGlyphShape.CHECK -> Icon(Icons.Filled.CheckCircle, null, modifier.size(size), tint = color)
        StatusGlyphShape.CROSS -> Icon(Icons.Filled.Cancel, null, modifier.size(size), tint = color)
        else -> Canvas(modifier.size(size)) {
            val radius = this.size.minDimension * (10f / 24f)
            val strokeWidth = this.size.minDimension * (2f / 24f)
            val ringRadius = radius - strokeWidth / 2f
            val dash = if (shape == StatusGlyphShape.DASHED_RING) {
                val segment = (2 * PI * ringRadius / 16).toFloat()
                PathEffect.dashPathEffect(floatArrayOf(segment, segment))
            } else {
                null
            }
            drawCircle(color, ringRadius, style = Stroke(width = strokeWidth, pathEffect = dash))

            val fillRadius = radius - strokeWidth * 2f
            val topLeft = Offset(center.x - fillRadius, center.y - fillRadius)
            val fillSize = Size(fillRadius * 2, fillRadius * 2)
            val rtl = layoutDirection == LayoutDirection.Rtl
            when (shape) {
                StatusGlyphShape.HALF_RING -> drawArc(
                    color = color,
                    startAngle = 90f,
                    sweepAngle = if (rtl) -180f else 180f,
                    useCenter = true,
                    topLeft = topLeft,
                    size = fillSize
                )
                StatusGlyphShape.THREE_QUARTER_RING -> drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = if (rtl) -270f else 270f,
                    useCenter = true,
                    topLeft = topLeft,
                    size = fillSize
                )
                else -> Unit
            }
        }
    }
}
