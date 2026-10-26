package com.monebie.app.domain.model

/** How often a scheduled obligation recurs. Drives next-due-date projection. */
enum class PaymentFrequency {
    ONE_TIME, WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, YEARLY
}

val PaymentFrequency.displayName: String
    get() = when (this) {
        PaymentFrequency.ONE_TIME -> "One time"
        PaymentFrequency.WEEKLY -> "Weekly"
        PaymentFrequency.BIWEEKLY -> "Every 2 weeks"
        PaymentFrequency.MONTHLY -> "Monthly"
        PaymentFrequency.QUARTERLY -> "Quarterly"
        PaymentFrequency.YEARLY -> "Yearly"
    }

/**
 * The cycle written to follow an amount: "1,041 / month".
 *
 * Separate from [displayName] because that one names the schedule and this one
 * qualifies a figure - "1,041 Monthly" reads as a label stuck onto a number
 * rather than a rate.
 */
val PaymentFrequency.perLabel: String
    get() = when (this) {
        PaymentFrequency.ONE_TIME -> "once"
        PaymentFrequency.WEEKLY -> "/ week"
        PaymentFrequency.BIWEEKLY -> "/ 2 weeks"
        PaymentFrequency.MONTHLY -> "/ month"
        PaymentFrequency.QUARTERLY -> "/ quarter"
        PaymentFrequency.YEARLY -> "/ year"
    }

/** Days added per cycle; null for month-based or non-recurring cycles. */
val PaymentFrequency.periodDays: Int?
    get() = when (this) {
        PaymentFrequency.WEEKLY -> 7
        PaymentFrequency.BIWEEKLY -> 14
        else -> null
    }

/** Months added per cycle; null for day-based or non-recurring cycles. */
val PaymentFrequency.periodMonths: Int?
    get() = when (this) {
        PaymentFrequency.MONTHLY -> 1
        PaymentFrequency.QUARTERLY -> 3
        PaymentFrequency.YEARLY -> 12
        else -> null
    }
