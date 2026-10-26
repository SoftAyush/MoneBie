package com.aurex.app.data.mapper

import com.aurex.app.core.Money
import com.aurex.app.domain.model.PaymentFrequency
import com.aurex.app.domain.model.PaymentMethod
import com.aurex.app.domain.model.RecurringTransaction
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.db.RecurringTransaction as DbRecurring

fun DbRecurring.toDomain(): RecurringTransaction = RecurringTransaction(
    id = id,
    name = name,
    amount = Money(amount_minor),
    type = TransactionType.entries.firstOrNull { it.name == type } ?: TransactionType.EXPENSE,
    categoryId = category_id,
    accountId = account_id,
    transferAccountId = transfer_account_id,
    paymentMethod = PaymentMethod.entries.firstOrNull { it.name == payment_method }
        ?: PaymentMethod.OTHER,
    frequency = PaymentFrequency.entries.firstOrNull { it.name == frequency }
        ?: PaymentFrequency.MONTHLY,
    startDate = start_date,
    nextDueDate = next_due_date,
    endDate = end_date,
    note = note,
    isPaused = is_paused != 0L,
    createdAt = created_at
)
