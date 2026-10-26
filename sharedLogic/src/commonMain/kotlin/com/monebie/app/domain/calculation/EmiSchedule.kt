package com.aurex.app.domain.calculation

import com.aurex.app.core.Money
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Standard reducing-balance EMI arithmetic.
 *
 * Used only to *suggest* figures on the add form. What the user confirms is
 * what gets stored, because a lender's actual schedule can differ from the
 * textbook formula through fees, rounding conventions or a part month - and the
 * app must reflect the real obligation, not an idealised one.
 *
 * Rate maths runs in [Double] and converts to [Money] exactly once at the end,
 * which is the only place floating point is allowed near a monetary value.
 */
object EmiSchedule {

    /** Months in a tenure, rounded to the nearest whole month and at least one. */
    fun installmentCount(tenureYears: Double): Int =
        (tenureYears * 12.0).roundToInt().coerceAtLeast(1)

    /**
     * Monthly installment for a principal at an annual percentage rate.
     *
     * `EMI = P * r * (1 + r)^n / ((1 + r)^n - 1)` where `r` is the monthly rate.
     * A zero or missing rate degenerates to an even split of the principal,
     * which is the correct answer for interest-free instalment plans.
     */
    fun monthlyInstallment(
        principal: Money,
        annualInterestRate: Double?,
        tenureYears: Double
    ): Money {
        val months = installmentCount(tenureYears)
        val rate = (annualInterestRate ?: 0.0) / 12.0 / 100.0

        if (rate <= 0.0) return Money(principal.minorUnits / months)

        val growth = (1.0 + rate).pow(months)
        val monthlyMinor = principal.minorUnits * rate * growth / (growth - 1.0)
        // roundToLong, not roundToInt: minor units of a large loan overflow Int
        // (which silently clamps to Int.MAX_VALUE) well before the amount is
        // unreasonable - Int tops out around 21 million major units.
        return Money(monthlyMinor.roundToLong())
    }

    /** Total payable over the life of the EMI: the monthly amount across every installment. */
    fun totalPayable(monthlyAmount: Money, tenureYears: Double): Money =
        monthlyAmount * installmentCount(tenureYears)

    /**
     * Suggests both figures at once for the add form.
     *
     * Returns null when there is not enough input to compute anything, so the
     * caller can leave the fields untouched rather than filling them with zeros.
     */
    fun suggest(
        principal: Money?,
        annualInterestRate: Double?,
        tenureYears: Double?
    ): Suggestion? {
        if (principal == null || !principal.isPositive) return null
        if (tenureYears == null || tenureYears <= 0.0) return null

        val monthly = monthlyInstallment(principal, annualInterestRate, tenureYears)
        return Suggestion(
            monthlyAmount = monthly,
            totalPayable = totalPayable(monthly, tenureYears),
            installments = installmentCount(tenureYears)
        )
    }

    data class Suggestion(
        val monthlyAmount: Money,
        val totalPayable: Money,
        val installments: Int
    )
}
