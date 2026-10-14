package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalDensity
import androidx.core.os.ConfigurationCompat
import java.text.NumberFormat
import java.util.Locale

/** The locale of the current configuration, so counts follow an in-app locale override. */
@Composable
@ReadOnlyComposable
internal fun currentLocale(): Locale =
    ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()

/** A count with the locale's digits and grouping ("1,234", "1.234", "١٬٢٣٤"). */
@Composable
@ReadOnlyComposable
internal fun formatCount(count: Int): String = NumberFormat.getIntegerInstance(currentLocale()).format(count)

/**
 * A plural resource with the count already locale-formatted. Reading [LocalConfiguration]
 * re-resolves it when the locale changes; Compose's own plural helper is still experimental
 * in this Compose version.
 */
@Composable
@ReadOnlyComposable
internal fun quantityString(@PluralsRes id: Int, count: Int): String {
    LocalConfiguration.current
    return LocalContext.current.resources.getQuantityString(id, count, formatCount(count))
}

/** An icon size that follows the font scale, so glyphs grow with the text beside them. */
@Composable
@ReadOnlyComposable
internal fun iconSizeFor(fontSize: TextUnit, factor: Float): Dp =
    with(LocalDensity.current) { fontSize.toDp() } * factor
