package com.monebie.app.domain.usecase.insights

import com.monebie.app.core.firstDayAfterMonth
import com.monebie.app.core.firstDayOfMonth
import com.monebie.app.core.formatMonth
import com.monebie.app.core.getCurrentDay
import com.monebie.app.core.isInMonth
import com.monebie.app.core.shiftMonth
import com.monebie.app.domain.calculation.FinancialCalculator
import com.monebie.app.domain.model.CategoryLookup
import com.monebie.app.domain.model.PeriodChange
import com.monebie.app.domain.model.SpendingInsights
import com.monebie.app.domain.model.TransactionType
import com.monebie.app.domain.repository.CategoryRepository
import com.monebie.app.domain.repository.TransactionRepository
import com.monebie.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Analyses one month of spending against the month before it.
 *
 * Reads a two-month window rather than the whole ledger: the comparison needs
 * exactly the previous month and nothing older, so the cost stays flat however
 * much history exists.
 */
class GetSpendingInsightsUseCase(
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val todayProvider: () -> String = ::getCurrentDay
) {

    /** [month] is a month key from monthOf. */
    operator fun invoke(month: String): Flow<SpendingInsights> {
        val today = todayProvider()

        val previousMonth = shiftMonth(month, -1)
        val windowStart = firstDayOfMonth(previousMonth)
        val windowEnd = firstDayAfterMonth(month)

        return combine(
            transactionRepository.observeTransactionsBetween(windowStart, windowEnd),
            userRepository.getSelectedCurrencySymbol(),
            // Archived categories included: a month that spent against one
            // still has to be able to name it.
            categoryRepository.observeCategories()
        ) { transactions, symbol, categories ->
            val lookup = CategoryLookup(categories)
            val thisMonth = transactions.filter { isInMonth(it.date, month) }
            val lastMonth = transactions.filter { isInMonth(it.date, previousMonth) }

            val summary = FinancialCalculator.summarise(thisMonth)
            val previousSummary = FinancialCalculator.summarise(lastMonth)

            SpendingInsights(
                month = month,
                monthLabel = formatMonth(month),
                currencySymbol = symbol ?: DEFAULT_CURRENCY_SYMBOL,
                canGoToNextMonth = today >= windowEnd,

                summary = summary,
                expenseChange = PeriodChange(summary.expense, previousSummary.expense),
                incomeChange = PeriodChange(summary.income, previousSummary.income),

                expenseCategories = FinancialCalculator.categoryBreakdown(
                    thisMonth,
                    TransactionType.EXPENSE,
                    lookup
                ),
                incomeCategories = FinancialCalculator.categoryBreakdown(
                    thisMonth,
                    TransactionType.INCOME,
                    lookup
                ),

                dailyAverageSpend = FinancialCalculator.dailyAverageExpense(
                    expense = summary.expense,
                    // Only the days that have actually happened, so a month in
                    // progress is not flattered by dividing across its full length.
                    days = FinancialCalculator.elapsedDaysInMonth(month, today)
                ),
                heaviestDay = FinancialCalculator.heaviestSpendingDay(thisMonth),
                transactionCount = thisMonth.size,
                isLoading = false
            )
        }
    }

    private companion object {
        const val DEFAULT_CURRENCY_SYMBOL = "$"
    }
}
