package com.monebie.app.data.mapper

import com.monebie.app.core.Money
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.AccountType
import com.monebie.app.db.Account as DbAccount

fun DbAccount.toDomain(): Account = Account(
    id = id,
    name = name,
    type = type.toAccountType(),
    balance = Money(balance_minor),
    currencyCode = currency_code,
    isArchived = is_archived == 1L,
    createdAt = created_at
)

/**
 * Account type is persisted by name rather than ordinal so reordering the enum
 * can never silently reinterpret existing rows. Unknown names fall back to
 * OTHER-like CASH instead of throwing, so a downgraded install still opens.
 */
private fun String.toAccountType(): AccountType =
    AccountType.entries.firstOrNull { it.name == this } ?: AccountType.CASH
