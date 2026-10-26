package com.monebie.app.data.mapper

import com.monebie.app.core.Money
import com.monebie.app.domain.model.Budget
import com.monebie.app.domain.model.BudgetPeriod
import com.monebie.app.db.Budget as DbBudget

fun DbBudget.toDomain(): Budget = Budget(
    id = id,
    categoryId = category_id,
    amount = Money(amount_minor),
    period = BudgetPeriod.entries.firstOrNull { it.name == period } ?: BudgetPeriod.MONTHLY,
    createdAt = created_at
)
