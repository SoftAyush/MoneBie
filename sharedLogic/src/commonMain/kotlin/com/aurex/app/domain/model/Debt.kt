package com.aurex.app.domain.model

import com.aurex.app.core.Money

enum class DebtStatus { ACTIVE, PARTIALLY_PAID, PAID, OVERDUE }

val DebtStatus.displayName: String
    get() = when (this) {
        DebtStatus.ACTIVE -> "Active"
        DebtStatus.PARTIALLY_PAID -> "Partially Paid"
        DebtStatus.PAID -> "Paid"
        DebtStatus.OVERDUE -> "Overdue"
    }

/**
 * Money the user owes a person, bank or institution.
 *
 * amountPaid is the only mutable money field, and it is the sum of this debt's
 * DebtPayment history. remainingAmount is derived rather than stored so the
 * headline figure can never drift away from the payments that produced it.
 * Status is derived too - see FinancialCalculator.debtStatus, which needs
 * today's date and therefore cannot live on the model.
 */
data class Debt(
    val id: Long,
    val name: String,
    val lender: String,
    val originalAmount: Money,
    val amountPaid: Money = Money.ZERO,
    val interestRate: Double? = null,
    /** yyyy-MM-dd */
    val startDate: String,
    val dueDate: String? = null,
    val paymentFrequency: PaymentFrequency = PaymentFrequency.MONTHLY,
    /** Expected amount per cycle, when the user set one. */
    val installmentAmount: Money? = null,
    val notes: String? = null,
    /** Account payments default to being made from. */
    val defaultAccountId: Long? = null
) {
    val remainingAmount: Money get() = (originalAmount - amountPaid).coerceAtLeastZero()

    val isSettled: Boolean get() = amountPaid >= originalAmount

    /** Share of the original amount already repaid, 0f..1f. */
    val progress: Float get() = amountPaid.ratioOf(originalAmount)

    /** Amount expected at the next due date, falling back to whatever is left. */
    val nextPaymentAmount: Money
        get() = installmentAmount?.coerceAtMost(remainingAmount) ?: remainingAmount
}

/**
 * One repayment against a Debt. Never delete or mutate these - the remaining
 * balance is recomputed from them, and they are the audit trail linking the debt
 * to the account the money left and to the transaction it created.
 */
data class DebtPayment(
    val id: Long,
    val debtId: Long,
    val amount: Money,
    /** yyyy-MM-dd */
    val date: String,
    val accountId: Long? = null,
    val transactionId: Long? = null,
    val note: String? = null
)
