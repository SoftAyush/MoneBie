package com.aurex.app.domain.model

import com.aurex.app.core.Money

/**
 * The editable terms of an EMI.
 *
 * Excludes installments paid and amount paid: both come from the payment
 * history, so no form can set them to something the records do not support.
 */
data class EmiDraft(
    val name: String,
    val provider: String,
    val principalAmount: Money,
    val totalPayableAmount: Money,
    val monthlyAmount: Money,
    val interestRate: Double? = null,
    /** yyyy-MM-dd */
    val startDate: String,
    val tenureYears: Double,
    val paymentAccountId: Long? = null,
    val notes: String? = null
)

fun Emi.toDraft(): EmiDraft = EmiDraft(
    name = name,
    provider = provider,
    principalAmount = principalAmount,
    totalPayableAmount = totalPayableAmount,
    monthlyAmount = monthlyAmount,
    interestRate = interestRate,
    startDate = startDate,
    tenureYears = tenureYears,
    paymentAccountId = paymentAccountId,
    notes = notes
)
