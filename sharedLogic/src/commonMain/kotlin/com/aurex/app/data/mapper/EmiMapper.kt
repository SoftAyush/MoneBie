package com.aurex.app.data.mapper

import com.aurex.app.core.Money
import com.aurex.app.domain.model.Emi
import com.aurex.app.domain.model.EmiPayment
import com.aurex.app.db.EmiPayment as DbEmiPayment
import com.aurex.app.db.SelectAllEmis
import com.aurex.app.db.SelectEmiById

fun SelectAllEmis.toDomain(): Emi = buildEmi(
    id = id,
    name = name,
    provider = provider,
    principalAmountMinor = principal_amount_minor,
    totalPayableAmountMinor = total_payable_amount_minor,
    monthlyAmountMinor = monthly_amount_minor,
    interestRate = interest_rate,
    startDate = start_date,
    tenureYears = tenure_years,
    installmentsPaid = installments_paid,
    amountPaidMinor = amount_paid_minor,
    paymentAccountId = payment_account_id,
    notes = notes
)

fun SelectEmiById.toDomain(): Emi = buildEmi(
    id = id,
    name = name,
    provider = provider,
    principalAmountMinor = principal_amount_minor,
    totalPayableAmountMinor = total_payable_amount_minor,
    monthlyAmountMinor = monthly_amount_minor,
    interestRate = interest_rate,
    startDate = start_date,
    tenureYears = tenure_years,
    installmentsPaid = installments_paid,
    amountPaidMinor = amount_paid_minor,
    paymentAccountId = payment_account_id,
    notes = notes
)

fun DbEmiPayment.toDomain(): EmiPayment = EmiPayment(
    id = id,
    emiId = emi_id,
    installmentNumber = installment_number.toInt(),
    amount = Money(amount_minor),
    dueDate = due_date,
    paidDate = paid_date,
    accountId = account_id,
    transactionId = transaction_id
)

private fun buildEmi(
    id: Long,
    name: String,
    provider: String,
    principalAmountMinor: Long,
    totalPayableAmountMinor: Long,
    monthlyAmountMinor: Long,
    interestRate: Double?,
    startDate: String,
    tenureYears: Double,
    installmentsPaid: Long,
    amountPaidMinor: Long,
    paymentAccountId: Long?,
    notes: String?
): Emi = Emi(
    id = id,
    name = name,
    provider = provider,
    principalAmount = Money(principalAmountMinor),
    totalPayableAmount = Money(totalPayableAmountMinor),
    monthlyAmount = Money(monthlyAmountMinor),
    interestRate = interestRate,
    startDate = startDate,
    tenureYears = tenureYears,
    // Both counted and summed from the payment history by the query.
    installmentsPaid = installmentsPaid.toInt(),
    amountPaid = Money(amountPaidMinor),
    paymentAccountId = paymentAccountId,
    notes = notes
)
