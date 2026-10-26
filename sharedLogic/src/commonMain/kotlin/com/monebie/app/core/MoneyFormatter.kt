package com.monebie.app.core

import kotlin.math.abs

/**
 * Pure-Kotlin money rendering so both Android and iOS produce identical strings.
 * Platform number formatters are deliberately avoided - locale grouping differences
 * would make the same balance read differently across targets.
 */
object MoneyFormatter {

    /** `$1,240.50` / `-$1,240.50` / `+$1,240.50` when [signed]. */
    fun format(
        money: Money,
        symbol: String,
        showDecimals: Boolean = true,
        signed: Boolean = false
    ): String {
        val negative = money.minorUnits < 0L
        val absolute = abs(money.minorUnits)
        val major = absolute / 100L
        val minor = (absolute % 100L).toInt()

        val digits = groupDigits(major.toString())
        val body = if (showDecimals) "$digits.${minor.toString().padStart(2, '0')}" else digits
        val sign = when {
            negative -> "-"
            signed && money.minorUnits > 0L -> "+"
            else -> ""
        }
        return "$sign$symbol$body"
    }

    /** `$1.2M` / `$12.4K` - for hero figures where the full number would wrap. */
    fun compact(money: Money, symbol: String): String {
        val negative = money.minorUnits < 0L
        val majorUnits = abs(money.minorUnits) / 100L
        val sign = if (negative) "-" else ""
        return when {
            majorUnits >= 1_000_000_000L -> "$sign$symbol${oneDecimal(majorUnits, 1_000_000_000L)}B"
            majorUnits >= 1_000_000L -> "$sign$symbol${oneDecimal(majorUnits, 1_000_000L)}M"
            majorUnits >= 100_000L -> "$sign$symbol${oneDecimal(majorUnits, 1_000L)}K"
            else -> format(money, symbol, showDecimals = false)
        }
    }

    /** `62%` from a `0f..1f` progress value. */
    fun percent(progress: Float): String = "${(progress.coerceIn(0f, 1f) * 100f).toInt()}%"

    private fun oneDecimal(value: Long, divisor: Long): String {
        val whole = value / divisor
        val tenth = (value % divisor) * 10L / divisor
        return if (tenth == 0L) "$whole" else "$whole.$tenth"
    }

    private fun groupDigits(digits: String): String {
        if (digits.length <= 3) return digits
        val builder = StringBuilder(digits.length + digits.length / 3)
        var counter = 0
        for (index in digits.indices.reversed()) {
            builder.append(digits[index])
            counter++
            if (counter % 3 == 0 && index != 0) builder.append(',')
        }
        return builder.reverse().toString()
    }
}
