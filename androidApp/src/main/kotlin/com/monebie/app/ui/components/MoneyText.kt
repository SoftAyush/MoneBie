package com.aurex.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.aurex.app.core.Money
import com.aurex.app.core.MoneyFormatter
import com.aurex.app.ui.theme.AurexTheme

/**
 * Renders a monetary value.
 *
 * Formatting always goes through MoneyFormatter so a figure reads identically
 * wherever it appears, and colour is opt-in: most amounts on a dashboard are
 * neutral facts, and tinting everything green or red destroys the hierarchy
 * that makes the few directional numbers stand out.
 */
@Composable
fun MoneyText(
    money: Money,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = MaterialTheme.colorScheme.onSurface,
    showDecimals: Boolean = true,
    signed: Boolean = false,
    compact: Boolean = false
) {
    Text(
        text = if (compact) {
            MoneyFormatter.compact(money, currencySymbol)
        } else {
            MoneyFormatter.format(money, currencySymbol, showDecimals = showDecimals, signed = signed)
        },
        style = style,
        fontWeight = fontWeight,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** Colour for a value where the sign carries meaning (net balance, net position). */
@Composable
fun signedMoneyColor(money: Money): Color = when {
    money.isPositive -> AurexTheme.finance.positive
    money.isNegative -> AurexTheme.finance.negative
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
