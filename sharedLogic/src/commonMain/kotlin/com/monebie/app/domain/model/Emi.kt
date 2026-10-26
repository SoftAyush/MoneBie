package com.aurex.app.domain.model

import com.aurex.app.core.Money
import com.aurex.app.core.addMonths
import kotlin.math.roundToInt

enum class EmiStatus { UPCOMING, ACTIVE, OVERDUE, COMPLETED }

val EmiStatus.displayName: String
    get() = when (this) {
        EmiStatus.UPCOMING -> "Upcoming"
        EmiStatus.ACTIVE -> "Active"
        EmiStatus.OVERDUE -> "Overdue"
        EmiStatus.COMPLETED -> "Completed"
    }

/**
 * A fixed-installment obligation (loan, appliance, vehicle...).
 *
 * Everything the spec lists as automatically calculated is a derived property
 * here rather than a stored column: total installments, end date and next due
 * date are pure functions of [startDate], [tenureYears] and [installmentsPaid],
 * so they cannot fall out of sync with the payment history.
 *
 * [installmentsPaid] and [amountPaid] are themselves a COUNT and a SUM over the
 * EmiPayment rows, filled in by the query. Nothing in this model is a number
 * someone has to remember to update.
 *
 * Installment *n* (1-based) is due at `startDate + n months`, which makes
 * `endDate == startDate + totalInstallments months`.
 */
data class Emi(
    val id: Long,
    val name: String,
    val provider: String,
    val principalAmount: Money,
    val totalPayableAmount: Money,
    val monthlyAmount: Money,
    val interestRate: Double? = null,
    /** yyyy-MM-dd */
    val startDate: String,
    val tenureYears: Double,
    /** COUNT of this EMI's payment rows. */
    val installmentsPaid: Int = 0,
    /** SUM of this EMI's payment amounts. */
    val amountPaid: Money = Money.ZERO,
    val paymentAccountId: Long? = null,
    val notes: String? = null
) {
    val totalInstallments: Int get() = (tenureYears * 12.0).roundToInt().coerceAtLeast(1)

    val remainingInstallments: Int get() = (totalInstallments - installmentsPaid).coerceAtLeast(0)

    val endDate: String get() = addMonths(startDate, totalInstallments)

    /** Null once every installment has been paid. */
    val nextDueDate: String?
        get() = if (remainingInstallments == 0) null else addMonths(startDate, installmentsPaid + 1)

    /** 1-based number of the installment that comes next, or null when finished. */
    val nextInstallmentNumber: Int?
        get() = if (remainingInstallments == 0) null else installmentsPaid + 1

    /**
     * What is still payable.
     *
     * Derived from the total payable minus what was actually paid, rather than
     * `monthly * remaining`, so a rounded or uneven final installment stays
     * accurate instead of leaving a few units stranded.
     */
    val remainingAmount: Money get() = (totalPayableAmount - amountPaid).coerceAtLeastZero()

    /**
     * Amount due for the next installment.
     *
     * Capped at what is left so the final installment collects the remainder
     * rather than overshooting the total payable.
     */
    val nextInstallmentAmount: Money get() = monthlyAmount.coerceAtMost(remainingAmount)

    /** Interest component over the life of the EMI. */
    val totalInterest: Money get() = (totalPayableAmount - principalAmount).coerceAtLeastZero()

    val isCompleted: Boolean get() = remainingInstallments == 0

    /**
     * Progress by installment count rather than by amount.
     *
     * "14 of 60 paid" is how people track an EMI, and it matches the schedule
     * even when a final installment differs slightly from the others.
     */
    val progress: Float
        get() = if (totalInstallments == 0) 0f
        else (installmentsPaid.toFloat() / totalInstallments.toFloat()).coerceIn(0f, 1f)
}

/**
 * One paid installment. Preserved permanently: [Emi.installmentsPaid] is the
 * count of these rows, and each links back to the account debited and the
 * transaction created.
 */
data class EmiPayment(
    val id: Long,
    val emiId: Long,
    val installmentNumber: Int,
    val amount: Money,
    /** yyyy-MM-dd the installment was scheduled for. */
    val dueDate: String,
    /** yyyy-MM-dd it was actually paid. */
    val paidDate: String,
    val accountId: Long? = null,
    val transactionId: Long? = null
)
