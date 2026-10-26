package com.monebie.app.core

import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.round

/**
 * A currency-agnostic monetary value stored in **minor units** (cents/paisa).
 *
 * Money is never represented as [Double] anywhere in the domain: floating point
 * accumulation drifts, and this application sums hundreds of transactions,
 * debt payments and EMI installments into a single net-position figure.
 * Every financial calculation in [com.monebie.app.domain.calculation] operates on
 * this type so rounding happens exactly once - at the input boundary.
 */
@JvmInline
value class Money(val minorUnits: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(minorUnits + other.minorUnits)

    operator fun minus(other: Money): Money = Money(minorUnits - other.minorUnits)

    operator fun times(factor: Int): Money = Money(minorUnits * factor)

    operator fun unaryMinus(): Money = Money(-minorUnits)

    val isZero: Boolean get() = minorUnits == 0L

    val isPositive: Boolean get() = minorUnits > 0L

    val isNegative: Boolean get() = minorUnits < 0L

    val absolute: Money get() = Money(abs(minorUnits))

    /** Clamps negatives to zero. Used wherever an over-payment must not create a negative balance. */
    fun coerceAtLeastZero(): Money = if (minorUnits < 0L) ZERO else this

    fun coerceAtMost(other: Money): Money = if (minorUnits > other.minorUnits) other else this

    /** Fraction of [other] this value represents, in `0f..1f`. Returns `0f` when [other] is zero. */
    fun ratioOf(other: Money): Float {
        if (other.minorUnits == 0L) return 0f
        return (minorUnits.toDouble() / other.minorUnits.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    /** Only for chart geometry and percentages - never for storing or summing money. */
    fun toMajorDouble(): Double = minorUnits / 100.0

    override fun compareTo(other: Money): Int = minorUnits.compareTo(other.minorUnits)

    companion object {
        val ZERO = Money(0L)

        fun fromMajor(value: Double): Money = Money(round(value * 100.0).toLong())

        fun fromMajor(value: Long): Money = Money(value * 100L)

        /**
         * Parses user input such as `1,240`, `1240.5` or `रू 1 240.55`.
         * Returns `null` when the text holds no usable number so callers can surface
         * a validation error instead of silently persisting zero.
         */
        fun parse(input: String): Money? {
            val cleaned = input.trim().filter { it.isDigit() || it == '.' || it == '-' }
            if (cleaned.isEmpty() || cleaned == "-" || cleaned == ".") return null
            val negative = cleaned.startsWith('-')
            val unsigned = cleaned.removePrefix("-")
            val parts = unsigned.split('.')
            if (parts.size > 2) return null
            val major = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
            val fraction = parts.getOrNull(1).orEmpty().take(2).padEnd(2, '0')
            val minor = fraction.toLongOrNull() ?: return null
            val total = major * 100L + minor
            return Money(if (negative) -total else total)
        }
    }
}

/** Sums money without leaving the exact-integer domain. */
fun Iterable<Money>.total(): Money = Money(sumOf { it.minorUnits })

inline fun <T> Iterable<T>.totalOf(selector: (T) -> Money): Money =
    Money(sumOf { selector(it).minorUnits })
