package com.monebie.app.domain.model

import com.monebie.app.core.Money

/**
 * One immutable, fully-computed view of the user's finances at a point in time.
 *
 * The dashboard renders this and nothing else. Every figure is produced by
 * FinancialCalculator from the source records, so no total is ever recomputed
 * in the UI layer and the same number cannot be derived two different ways in
 * two different screens.
 */
data class DashboardSnapshot(
    val userName: String,
    val currencySymbol: String,
    /** yyyy-MM-dd the snapshot was computed for. */
    val today: String,

    val accounts: List<Account>,
    /** Every category, hidden ones included, so any row can be labelled. */
    val categories: List<TransactionCategory> = TransactionCategories.all,
    /** Liquid balance across asset accounts only - credit cards are liabilities. */
    val totalBalance: Money,

    val todaySummary: PeriodSummary,
    val monthSummary: PeriodSummary,
    /** Last 7 days including today, oldest first. */
    val weeklyTrend: List<DailyTotal>,

    val netPosition: NetPosition,

    val totalDebtOwed: Money,
    val totalMoneyBorrowed: Money,
    val totalMoneyLent: Money,
    val remainingEmiAmount: Money,
    val emiDueThisMonth: Money,

    val upcomingPayments: List<UpcomingPayment>,
    val recentTransactions: List<Transaction>,
    /** This month's budgets measured against its spending, worst first. */
    val budgets: List<BudgetStatus> = emptyList()
) {
    /** Budgets already past their limit this month. */
    val budgetsOver: Int get() = budgets.count { it.isOver }

    /** Budgets close enough to the limit to be worth a glance. */
    val budgetsNearLimit: Int get() = budgets.count { it.isNearLimit }

    /** Debt + EMI + borrowings: the single "money I owe" figure. */
    val totalOwed: Money get() = totalDebtOwed + remainingEmiAmount + totalMoneyBorrowed

    /** Money the user is waiting to receive. */
    val totalOwedToUser: Money get() = totalMoneyLent

    /**
     * What a day usually costs, averaged over the days before today.
     *
     * Today is excluded on purpose. An average that includes the figure being
     * compared against it drags itself towards that figure, so a heavy day
     * would look less unusual than it was - and on the first day of use it
     * would compare today against itself and always read "normal".
     *
     * Zero when there is no earlier day in the window to average, which the
     * caller has to treat as "nothing to compare with" rather than "a usual
     * day costs nothing".
     */
    val usualDailyExpense: Money
        get() {
            val earlier = weeklyTrend.filter { it.day != today }
            if (earlier.isEmpty()) return Money.ZERO
            return Money(earlier.sumOf { it.expense.minorUnits } / earlier.size)
        }

    val hasAnyActivity: Boolean
        get() = recentTransactions.isNotEmpty() ||
            !totalOwed.isZero ||
            !totalMoneyLent.isZero

    /** Resolves the display name for a transaction row without a second lookup pass. */
    fun accountName(accountId: Long): String =
        accounts.firstOrNull { it.id == accountId }?.name ?: "Account"

    private val lookup: CategoryLookup get() = CategoryLookup(categories)

    /** The category behind a row, as the user has it named today. */
    fun category(categoryId: String): TransactionCategory = lookup[categoryId]
}
