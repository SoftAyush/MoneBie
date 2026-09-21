package com.aurex.app.data.mapper

import com.aurex.app.core.Money
import com.aurex.app.domain.model.Budget
import com.aurex.app.domain.model.BudgetPeriod
import com.aurex.app.db.Budget as DbBudget

fun DbBudget.toDomain(): Budget = Budget(
    id = id,
    categoryId = category_id,
    amount = Money(amount_minor),
    period = BudgetPeriod.entries.firstOrNull { it.name == period } ?: BudgetPeriod.MONTHLY,
    createdAt = created_at
)
