package com.monebie.app.data.mapper

import com.monebie.app.core.Money
import com.monebie.app.domain.model.PaymentMethod
import com.monebie.app.domain.model.Transaction
import com.monebie.app.domain.model.TransactionSource
import com.monebie.app.domain.model.TransactionSourceType
import com.monebie.app.domain.model.TransactionType
import com.monebie.app.db.TransactionRecord as DbTransaction

fun DbTransaction.toDomain(): Transaction = Transaction(
    id = id,
    amount = Money(amount_minor),
    type = type.toTransactionType(),
    categoryId = category_id,
    accountId = account_id,
    date = date,
    note = note,
    paymentMethod = payment_method.toPaymentMethod(),
    source = TransactionSource(
        type = source_type.toSourceType(),
        referenceId = source_reference_id,
        paymentId = source_payment_id
    ),
    transferAccountId = transfer_account_id
)

/*
 * Enums persist by name, never by ordinal: reordering an enum must not silently
 * reinterpret existing rows. Unknown names degrade to a safe default rather
 * than throwing, so a row written by a newer build cannot crash an older one.
 */

private fun String.toTransactionType(): TransactionType =
    TransactionType.entries.firstOrNull { it.name == this } ?: TransactionType.EXPENSE

private fun String.toPaymentMethod(): PaymentMethod =
    PaymentMethod.entries.firstOrNull { it.name == this } ?: PaymentMethod.OTHER

private fun String.toSourceType(): TransactionSourceType =
    TransactionSourceType.entries.firstOrNull { it.name == this } ?: TransactionSourceType.MANUAL
