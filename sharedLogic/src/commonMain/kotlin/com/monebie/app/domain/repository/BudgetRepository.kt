package com.aurex.app.domain.repository

import com.aurex.app.domain.model.Budget
import com.aurex.app.domain.model.BudgetDraft
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {

    fun observeBudgets(): Flow<List<Budget>>

    suspend fun getBudget(id: Long): Budget?

    suspend fun getBudgetForCategory(categoryId: String): Budget?

    /** Returns the new id. Fails if the category already has a budget. */
    suspend fun createBudget(draft: BudgetDraft): Long

    suspend fun updateBudget(id: Long, draft: BudgetDraft)

    suspend fun deleteBudget(id: Long)
}
