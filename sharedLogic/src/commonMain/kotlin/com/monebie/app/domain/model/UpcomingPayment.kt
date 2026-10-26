package com.monebie.app.domain.model

import com.monebie.app.core.Money

enum class UpcomingPaymentKind {
    DEBT, EMI,

    /** A borrowing the user must repay. */
    BORROWED,

    /** A loan the user expects back. */
    LENT
}

/**
 * A normalised due-date entry projected from debts, EMIs and person-to-person
 * loans, so the dashboard renders one chronological list instead of three.
 *
 * Purely derived - never persisted. Recomputing it from the source records means
 * it can never describe an obligation that has since been settled.
 */
data class UpcomingPayment(
    /** Stable within a snapshot, e.g. "emi-4". */
    val id: String,
    val kind: UpcomingPaymentKind,
    val title: String,
    val subtitle: String,
    val amount: Money,
    /** yyyy-MM-dd */
    val dueDate: String,
    val daysUntilDue: Long
) {
    val isOverdue: Boolean get() = daysUntilDue < 0L

    val isDueToday: Boolean get() = daysUntilDue == 0L

    /** Incoming money the user is waiting on, rather than money going out. */
    val isReceivable: Boolean get() = kind == UpcomingPaymentKind.LENT
}
