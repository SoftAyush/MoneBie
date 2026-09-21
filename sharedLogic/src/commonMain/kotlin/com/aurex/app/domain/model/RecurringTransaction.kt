package com.aurex.app.domain.model

import com.aurex.app.core.Money
import com.aurex.app.core.daysBetween

/**
 * A transaction that repeats on a schedule.
 *
 * The template only. Each occurrence that falls due becomes an ordinary
 * [Transaction] with a source pointing back here, so the ledger remains the
 * one record of what happened and this row can be edited, paused or deleted
 * without rewriting any of it.
 */
data class RecurringTransaction(
    val id: Long,
    val name: String,
    val amount: Money,
    val type: TransactionType,
    val categoryId: String,
    val accountId: Long,
    /** Destination account for a TRANSFER. */
    val transferAccountId: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val frequency: PaymentFrequency,
    /** yyyy-MM-dd */
    val startDate: String,
    /** yyyy-MM-dd. The next occurrence not yet posted or skipped. */
    val nextDueDate: String,
    /** yyyy-MM-dd, or null for an open-ended schedule. */
    val endDate: String? = null,
    val note: String? = null,
    val isPaused: Boolean = false,
    val createdAt: String
) {
    /** Whether the schedule has run past its end date. */
    val hasEnded: Boolean get() = endDate != null && nextDueDate > endDate

    val isActive: Boolean get() = !isPaused && !hasEnded

    /** Days until the next occurrence; negative when it is overdue for posting. */
    fun daysUntilNext(today: String): Long = daysBetween(today, nextDueDate)

    /** Whether an occurrence is waiting to be posted as of [today]. */
    fun isDue(today: String): Boolean = isActive && nextDueDate <= today

    /** Rough monthly cost, for the "per month" total at the top of the list. */
    val monthlyEquivalent: Money
        get() = when (frequency) {
            PaymentFrequency.WEEKLY -> Money(amount.minorUnits * 52L / 12L)
            PaymentFrequency.BIWEEKLY -> Money(amount.minorUnits * 26L / 12L)
            PaymentFrequency.MONTHLY -> amount
            PaymentFrequency.QUARTERLY -> Money(amount.minorUnits / 3L)
            PaymentFrequency.YEARLY -> Money(amount.minorUnits / 12L)
            PaymentFrequency.ONE_TIME -> Money.ZERO
        }
}

/** A recurring transaction that has not been persisted yet. */
data class RecurringTransactionDraft(
    val name: String,
    val amount: Money,
    val type: TransactionType,
    val categoryId: String,
    val accountId: Long,
    val transferAccountId: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val frequency: PaymentFrequency,
    /** yyyy-MM-dd */
    val startDate: String,
    val endDate: String? = null,
    val note: String? = null
)

/** What one pass over the due schedules produced. */
data class RecurringPostingResult(
    /** Transactions written to the ledger. */
    val postedCount: Int = 0,
    /** Names of the schedules that had something posted, for the confirmation. */
    val postedNames: List<String> = emptyList()
) {
    val isEmpty: Boolean get() = postedCount == 0
}
