package com.monebie.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colours for financial meaning, separate from the Material scheme.
 *
 * The brand ramp is a single blue hue, and money direction cannot be told in one
 * hue: income and expense would differ only in lightness, which disappears for
 * anyone with low vision and on a dim screen. So green and red stay - retuned
 * to sit beside the ramp rather than fight it.
 *
 * [receivable] is the exception and comes straight from the palette: money owed
 * to the user is neither income nor loss, and the cyan step says "still out
 * there" without borrowing either signal.
 */
@Immutable
data class FinanceColors(
    /** Income, gains, a positive net position. */
    val positive: Color,
    val positiveContainer: Color,
    /** Expenses, money owed, a negative net position. */
    val negative: Color,
    val negativeContainer: Color,
    /** Due soon, partially paid. */
    val caution: Color,
    val cautionContainer: Color,
    /** Money owed to the user - distinct from income so the two never merge. */
    val receivable: Color,
    val receivableContainer: Color
)

val LightFinanceColors = FinanceColors(
    positive = Color(0xFF0F7A4A),
    positiveContainer = Color(0xFFD3F3E2),
    negative = Color(0xFFC0342B),
    negativeContainer = Color(0xFFFBE2E0),
    caution = Color(0xFF8A5600),
    cautionContainer = Color(0xFFFFEBCC),
    // Palette cyan taken down until it clears 4.5:1 on white - #00B4D8 itself
    // manages only 1.9:1, and even a light darkening only reached 3.6:1.
    receivable = Color(0xFF057086),
    receivableContainer = Color(0xFFDDF4FA)
)

val DarkFinanceColors = FinanceColors(
    positive = Color(0xFF5FD6A0),
    positiveContainer = Color(0xFF0D3D2A),
    negative = Color(0xFFFF9E95),
    negativeContainer = Color(0xFF4A1512),
    caution = Color(0xFFFFC46B),
    cautionContainer = Color(0xFF452D00),
    // Palette500 straight from the ramp; primary is the paler Palette300, so
    // the two stay tellable apart.
    receivable = Palette500,
    receivableContainer = Color(0xFF06364A)
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }
