package com.monebie.app.domain.model

import com.monebie.app.core.Money

/**
 * A transaction that has not been persisted yet.
 *
 * Separate from [Transaction] because an unsaved entry has no id, and because
 * later phases post drafts on the user's behalf (a debt payment, an EMI
 * installment) through the same single entry point. Carrying [source] here is
 * what lets those postings stay traceable back to the record that created them.
 */
data class TransactionDraft(
    val amount: Money,
    val type: TransactionType,
    val categoryId: String,
    val accountId: Long,
    /** yyyy-MM-dd HH:mm:ss */
    val date: String,
    val note: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val transferAccountId: Long? = null,
    val source: TransactionSource = TransactionSource.Manual
)

/** A pending change to one account's balance. */
data class AccountDelta(
    val accountId: Long,
    val delta: Money
)

fun Transaction.toDraft(): TransactionDraft = TransactionDraft(
    amount = amount,
    type = type,
    categoryId = categoryId,
    accountId = accountId,
    date = date,
    note = note,
    paymentMethod = paymentMethod,
    transferAccountId = transferAccountId,
    source = source
)
