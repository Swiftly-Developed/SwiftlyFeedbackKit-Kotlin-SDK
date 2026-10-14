package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** WCAG 2.x contrast ratio between two opaque colours, from 1 to 21. */
internal fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

/**
 * Returns [foreground] when it already reaches [minRatio] against [background]. Otherwise it
 * moves the colour toward black or white, whichever contrasts more with the background, by
 * the smallest step that reaches the ratio. The hue is kept, so a host's status colour stays
 * recognisable while its text stays legible on both light and dark cards.
 */
internal fun readableOn(foreground: Color, background: Color, minRatio: Float = 4.5f): Color {
    val opaque = foreground.copy(alpha = 1f)
    if (contrastRatio(opaque, background) >= minRatio) return opaque
    val target = if (contrastRatio(Color.Black, background) >= contrastRatio(Color.White, background)) {
        Color.Black
    } else {
        Color.White
    }
    var low = 0f
    var high = 1f
    repeat(16) {
        val mid = (low + high) / 2f
        if (contrastRatio(lerp(opaque, target, mid), background) >= minRatio) high = mid else low = mid
    }
    return lerp(opaque, target, high)
}
