package com.monebie.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.monebie.app.core.getCurrentDateString
import com.monebie.app.data.database.DatabaseHelper
import com.monebie.app.data.mapper.toDomain
import com.monebie.app.domain.model.Budget
import com.monebie.app.domain.model.BudgetDraft
import com.monebie.app.domain.repository.BudgetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BudgetRepositoryImpl(
    private val dbHelper: DatabaseHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : BudgetRepository {

    private val queries get() = dbHelper.budgetQueries

    override fun observeBudgets(): Flow<List<Budget>> =
        queries.selectAllBudgets()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getBudget(id: Long): Budget? = withContext(ioDispatcher) {
        queries.getBudgetById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getBudgetForCategory(categoryId: String): Budget? =
        withContext(ioDispatcher) {
            queries.getBudgetForCategory(categoryId).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun createBudget(draft: BudgetDraft): Long = withContext(ioDispatcher) {
        queries.transactionWithResult {
            queries.insertBudget(
                category_id = draft.categoryId,
                amount_minor = draft.amount.minorUnits,
                period = draft.period.name,
                created_at = getCurrentDateString()
            )
            queries.lastInsertedBudgetId().executeAsOne()
        }
    }

    override suspend fun updateBudget(id: Long, draft: BudgetDraft) {
        withContext(ioDispatcher) {
            queries.updateBudget(
                categoryId = draft.categoryId,
                amountMinor = draft.amount.minorUnits,
                period = draft.period.name,
                id = id
            )
        }
    }

    override suspend fun deleteBudget(id: Long) {
        withContext(ioDispatcher) { queries.deleteBudget(id) }
    }
}
