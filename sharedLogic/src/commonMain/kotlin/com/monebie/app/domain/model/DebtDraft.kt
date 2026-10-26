package com.monebie.app.domain.model

import com.monebie.app.core.Money

/**
 * The editable fields of a debt.
 *
 * Deliberately excludes amountPaid and status: both are derived from the
 * payment history and today's date, so there is no way for an edit form to set
 * them to something the records do not support.
 */
data class DebtDraft(
    val name: String,
    val lender: String,
    val originalAmount: Money,
    val interestRate: Double? = null,
    /** yyyy-MM-dd */
    val startDate: String,
    val dueDate: String? = null,
    val paymentFrequency: PaymentFrequency = PaymentFrequency.MONTHLY,
    val installmentAmount: Money? = null,
    val notes: String? = null,
    val defaultAccountId: Long? = null
)

fun Debt.toDraft(): DebtDraft = DebtDraft(
    name = name,
    lender = lender,
    originalAmount = originalAmount,
    interestRate = interestRate,
    startDate = startDate,
    dueDate = dueDate,
    paymentFrequency = paymentFrequency,
    installmentAmount = installmentAmount,
    notes = notes,
    defaultAccountId = defaultAccountId
)

/**
 * The payment method that best fits an account, used to pre-fill the payment
 * form so the user does not have to restate something the account implies.
 */
fun defaultPaymentMethodFor(accountType: AccountType): PaymentMethod = when (accountType) {
    AccountType.CASH -> PaymentMethod.CASH
    AccountType.BANK -> PaymentMethod.BANK_TRANSFER
    AccountType.DIGITAL_WALLET -> PaymentMethod.DIGITAL_WALLET
    AccountType.CREDIT_CARD -> PaymentMethod.CARD
    AccountType.LOAN, AccountType.EMI -> PaymentMethod.OTHER
}
