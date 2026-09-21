package com.aurex.app.domain.usecase.budget

import com.aurex.app.core.Money
import com.aurex.app.core.addMonths
import com.aurex.app.core.firstDayOfMonth
import com.aurex.app.core.formatMonthYear
import com.aurex.app.core.getCurrentDay
import com.aurex.app.core.monthOf
import com.aurex.app.domain.model.Budget
import com.aurex.app.domain.model.BudgetDraft
import com.aurex.app.domain.model.BudgetOverview
import com.aurex.app.domain.model.BudgetStatus
import com.aurex.app.domain.model.CategoryLookup
import com.aurex.app.domain.model.Transaction
import com.aurex.app.domain.model.TransactionCategory
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.domain.repository.BudgetRepository
import com.aurex.app.domain.repository.CategoryRepository
import com.aurex.app.domain.repository.TransactionRepository
import com.aurex.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetBudgetsUseCase(private val repository: BudgetRepository) {
    operator fun invoke(): Flow<List<Budget>> = repository.observeBudgets()
}

/**
 * Creates or replaces a budget after checking it makes sense.
 *
 * Validation lives here rather than in the form so the same rules apply to
 * every way a budget can be made, including a future quick-add from insights.
 */
class SaveBudgetUseCase(
    private val repository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) {

    sealed interface Result {
        data class Saved(val id: Long) : Result
        data class Invalid(val message: String) : Result
    }

    suspend operator fun invoke(existingId: Long?, draft: BudgetDraft): Result {
        if (!draft.amount.isPositive) return Result.Invalid("Enter a limit greater than zero")

        val category = categoryRepository.getCategory(draft.categoryId)
            ?: return Result.Invalid("Pick a category")
        if (category.appliesTo != TransactionType.EXPENSE) {
            return Result.Invalid("Budgets apply to spending categories")
        }

        // One budget per category. Editing keeps its own row out of the check.
        val clash = repository.getBudgetForCategory(draft.categoryId)
        if (clash != null && clash.id != existingId) {
            return Result.Invalid(category.name + " already has a budget - edit that one instead")
        }

        if (existingId == null) return Result.Saved(repository.createBudget(draft))
        repository.updateBudget(existingId, draft)
        return Result.Saved(existingId)
    }
}

class DeleteBudgetUseCase(private val repository: BudgetRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteBudget(id)
}

/**
 * Every budget measured against one month of spending.
 *
 * Reads only that month's transactions: budgets are monthly, so nothing older
 * can change the answer.
 */
class GetBudgetOverviewUseCase(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository,
    private val todayProvider: () -> String = ::getCurrentDay
) {

    /** [month] is yyyy-MM. */
    operator fun invoke(month: String): Flow<BudgetOverview> {
        val today = todayProvider()
        val start = firstDayOfMonth(month)
        val end = addMonths(start, 1)

        return combine(
            budgetRepository.observeBudgets(),
            transactionRepository.observeTransactionsBetween(start, end),
            categoryRepository.observeCategories(),
            userRepository.getSelectedCurrencySymbol()
        ) { budgets, transactions, categories, symbol ->
            BudgetOverview(
                month = month,
                monthLabel = formatMonthYear(start),
                currencySymbol = symbol ?: DEFAULT_CURRENCY_SYMBOL,
                canGoToNextMonth = month < monthOf(today),
                statuses = BudgetMeasure.measure(budgets, transactions, CategoryLookup(categories)),
                availableCategories = BudgetMeasure.unbudgeted(categories, budgets),
                isLoading = false
            )
        }
    }

    private companion object {
        const val DEFAULT_CURRENCY_SYMBOL = "$"
    }
}

/**
 * The arithmetic behind a budget status, kept pure so it can be tested
 * without a database.
 */
object BudgetMeasure {

    /**
     * Measures [budgets] against [transactions], which should already be
     * limited to the period in question.
     *
     * Only expenses count. A refund filed under Shopping is income and does
     * not give the budget room back - which is arguable, but the alternative
     * has a refund of last month's purchase flattering this month's budget.
     */
    fun measure(
        budgets: List<Budget>,
        transactions: List<Transaction>,
        categories: CategoryLookup
    ): List<BudgetStatus> {
        val spending = transactions.filter { it.type == TransactionType.EXPENSE }
        return budgets
            .map { budget ->
                val inCategory = spending.filter { it.categoryId == budget.categoryId }
                BudgetStatus(
                    budget = budget,
                    category = categories[budget.categoryId],
                    spent = Money(inCategory.sumOf { it.amount.minorUnits }),
                    transactionCount = inCategory.size
                )
            }
            // Worst first, then by how much of the limit is gone.
            .sortedWith(compareByDescending<BudgetStatus> { it.isOver }.thenByDescending { it.usedRatio })
    }

    /** The categories a budget could still be added to. */
    fun unbudgeted(
        categories: List<TransactionCategory>,
        budgets: List<Budget>
    ): List<TransactionCategory> = categories.filter { category ->
        category.appliesTo == TransactionType.EXPENSE &&
            !category.isArchived &&
            budgets.none { it.categoryId == category.id }
    }
}
