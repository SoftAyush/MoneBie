package com.aurex.app.domain.model

import com.aurex.app.core.Money

/** One category's share of a period's spending or earning. */
data class CategorySpend(
    val categoryId: String,
    val label: String,
    val iconKey: String,
    val amount: Money,
    /** Share of the period's total for this direction, 0f..1f. */
    val share: Float,
    val transactionCount: Int
)

/**
 * How one figure moved against the period before it.
 *
 * Direction is deliberately separate from sign: spending less is an
 * improvement and earning less is not, and only the caller knows which way
 * round a given figure reads.
 */
data class PeriodChange(
    val current: Money,
    val previous: Money
) {
    val difference: Money get() = current - previous

    val hasComparison: Boolean get() = !previous.isZero

    /** Magnitude of the change relative to the previous period, 0f..1f for display. */
    val ratio: Float get() = difference.absolute.ratioOf(previous.absolute)

    val increased: Boolean get() = difference.isPositive

    val unchanged: Boolean get() = difference.isZero
}

/**
 * Everything the insights screen shows for one month.
 *
 * Assembled by FinancialCalculator from the same transactions the rest of the
 * app reads, so a category total here can never disagree with the month total
 * on the dashboard.
 */
data class SpendingInsights(
    /** yyyy-MM */
    val month: String = "",
    val monthLabel: String = "",
    val currencySymbol: String = "$",
    val canGoToNextMonth: Boolean = false,

    val summary: PeriodSummary = PeriodSummary(),
    val expenseChange: PeriodChange = PeriodChange(Money.ZERO, Money.ZERO),
    val incomeChange: PeriodChange = PeriodChange(Money.ZERO, Money.ZERO),

    val expenseCategories: List<CategorySpend> = emptyList(),
    val incomeCategories: List<CategorySpend> = emptyList(),

    /** Spend per elapsed day, so a part-way month is not compared against a full one. */
    val dailyAverageSpend: Money = Money.ZERO,
    val heaviestDay: DailyTotal? = null,
    val transactionCount: Int = 0,
    val isLoading: Boolean = true
) {
    val hasActivity: Boolean get() = summary.hasActivity

    /**
     * Share of income kept rather than spent, 0f..1f.
     *
     * Zero when nothing was earned: with no income there is no rate to express,
     * and reporting 100% for a month of pure spending would be nonsense.
     */
    val savingsRate: Float
        get() = if (summary.income.isZero || !summary.net.isPositive) {
            0f
        } else {
            summary.net.ratioOf(summary.income)
        }

    val isOverspending: Boolean get() = summary.net.isNegative

    val topExpenseCategory: CategorySpend? get() = expenseCategories.firstOrNull()
}
