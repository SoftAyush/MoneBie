package com.monebie.app.data.mapper

import com.monebie.app.core.Money
import com.monebie.app.domain.model.Debt
import com.monebie.app.domain.model.DebtPayment
import com.monebie.app.domain.model.PaymentFrequency
import com.monebie.app.db.DebtPayment as DbDebtPayment
import com.monebie.app.db.SelectAllDebts
import com.monebie.app.db.SelectDebtById

/*
 * The two debt selects share a shape - the Debt columns plus a SUMmed
 * amount_paid_minor - but SQLDelight generates a distinct row type per query,
 * so both funnel into one builder. Adding a third select means one more
 * three-line extension, not a second copy of the mapping rules.
 */

fun SelectAllDebts.toDomain(): Debt = buildDebt(
    id = id,
    name = name,
    lender = lender,
    originalAmountMinor = original_amount_minor,
    interestRate = interest_rate,
    startDate = start_date,
    dueDate = due_date,
    paymentFrequency = payment_frequency,
    installmentAmountMinor = installment_amount_minor,
    notes = notes,
    defaultAccountId = default_account_id,
    amountPaidMinor = amount_paid_minor
)

fun SelectDebtById.toDomain(): Debt = buildDebt(
    id = id,
    name = name,
    lender = lender,
    originalAmountMinor = original_amount_minor,
    interestRate = interest_rate,
    startDate = start_date,
    dueDate = due_date,
    paymentFrequency = payment_frequency,
    installmentAmountMinor = installment_amount_minor,
    notes = notes,
    defaultAccountId = default_account_id,
    amountPaidMinor = amount_paid_minor
)

fun DbDebtPayment.toDomain(): DebtPayment = DebtPayment(
    id = id,
    debtId = debt_id,
    amount = Money(amount_minor),
    date = date,
    accountId = account_id,
    transactionId = transaction_id,
    note = note
)

private fun buildDebt(
    id: Long,
    name: String,
    lender: String,
    originalAmountMinor: Long,
    interestRate: Double?,
    startDate: String,
    dueDate: String?,
    paymentFrequency: String,
    installmentAmountMinor: Long?,
    notes: String?,
    defaultAccountId: Long?,
    amountPaidMinor: Long
): Debt = Debt(
    id = id,
    name = name,
    lender = lender,
    originalAmount = Money(originalAmountMinor),
    // Summed from the payment history by the query, never stored.
    amountPaid = Money(amountPaidMinor),
    interestRate = interestRate,
    startDate = startDate,
    dueDate = dueDate,
    paymentFrequency = paymentFrequency.toPaymentFrequency(),
    installmentAmount = installmentAmountMinor?.let { Money(it) },
    notes = notes,
    defaultAccountId = defaultAccountId
)

/** Persisted by name, so reordering the enum cannot reinterpret existing rows. */
private fun String.toPaymentFrequency(): PaymentFrequency =
    PaymentFrequency.entries.firstOrNull { it.name == this } ?: PaymentFrequency.MONTHLY
