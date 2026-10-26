package com.monebie.app.domain.usecase.dashboard

import com.monebie.app.core.AppCalendar
import com.monebie.app.core.addDays
import com.monebie.app.core.getCurrentDay
import com.monebie.app.core.isInMonth
import com.monebie.app.core.monthOf
import com.monebie.app.core.startOfMonth
import com.monebie.app.domain.calculation.FinancialCalculator
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.Budget
import com.monebie.app.domain.model.CategoryLookup
import com.monebie.app.domain.model.BorrowLendRecord
import com.monebie.app.domain.model.TransactionCategory
import com.monebie.app.domain.model.DashboardSnapshot
import com.monebie.app.domain.model.Debt
import com.monebie.app.domain.model.Emi
import com.monebie.app.domain.model.Transaction
import com.monebie.app.domain.repository.AccountRepository
import com.monebie.app.domain.repository.BorrowLendRepository
import com.monebie.app.domain.repository.BudgetRepository
import com.monebie.app.domain.repository.CategoryRepository
import com.monebie.app.domain.repository.DebtRepository
import com.monebie.app.domain.repository.EmiRepository
import com.monebie.app.domain.repository.TransactionRepository
import com.monebie.app.domain.repository.UserRepository
import com.monebie.app.domain.usecase.budget.BudgetMeasure
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Assembles the complete dashboard view from every module.
 *
 * This is the only place the modules are joined. It reads raw records from the
 * repositories, hands them to [FinancialCalculator], and emits one finished
 * [DashboardSnapshot]. The ViewModel adds no arithmetic, and the UI does none
 * at all - which is what stops the same total being derived two ways in two
 * places as the app grows.
 *
 * Every source is a Flow, so a new transaction, a recorded debt payment or a
 * paid EMI installment recomputes the entire financial picture automatically.
 */
class GetDashboardSnapshotUseCase(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val debtRepository: DebtRepository,
    private val emiRepository: EmiRepository,
    private val borrowLendRepository: BorrowLendRepository,
    /** Injectable so snapshots are reproducible in tests. */
    private val todayProvider: () -> String = ::getCurrentDay,
    /**
     * Optional so the dashboard can be assembled without the budgets module,
     * which is what the existing tests and the iOS host do.
     */
    private val budgetRepository: BudgetRepository? = null
) {

    private data class Profile(val name: String, val currencySymbol: String)

    private data class Obligations(
        val debts: List<Debt>,
        val emis: List<Emi>,
        val records: List<BorrowLendRecord>,
        val budgets: List<Budget> = emptyList()
    )

    private data class References(
        val accounts: List<Account>,
        val categories: List<TransactionCategory>
    )

    private data class Ledger(
        /** Bounded window covering everything the dashboard summarises. */
        val window: List<Transaction>,
        val recent: List<Transaction>
    )

    /**
     * Rebuilt whenever the user switches between AD and BS, since that moves
     * where "this month" starts and so which transactions the window must read.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(recentTransactionLimit: Int = 5): Flow<DashboardSnapshot> =
        AppCalendar.system.flatMapLatest { snapshots(recentTransactionLimit) }

    private fun snapshots(recentTransactionLimit: Int): Flow<DashboardSnapshot> {
        val today = todayProvider()

        val profile = combine(
            userRepository.getAllUsers(),
            userRepository.getSelectedCurrencySymbol()
        ) { users, symbol ->
            Profile(
                name = users.firstOrNull()?.name.orEmpty(),
                currencySymbol = symbol ?: DEFAULT_CURRENCY_SYMBOL
            )
        }

        /*
         * The dashboard needs today, this month and the last seven days. Early
         * in a month the 7-day trend reaches back past the 1st, so the window
         * starts at whichever of those two is earlier - both are zero-padded
         * yyyy-MM-dd, so a string comparison picks the right one.
         *
         * Reading a window rather than the whole ledger keeps the dashboard's
         * cost flat as history grows.
         */
        val windowStart = minOf(startOfMonth(today), addDays(today, -(TREND_DAYS - 1)))
        val windowEnd = addDays(today, 1)

        val ledger = combine(
            transactionRepository.observeTransactionsBetween(windowStart, windowEnd),
            transactionRepository.observeRecentTransactions(recentTransactionLimit)
        ) { window, recent -> Ledger(window, recent) }

        // Grouped into fewer flows because combine tops out at five typed sources.
        val obligations = combine(
            debtRepository.observeDebts(),
            emiRepository.observeEmis(),
            borrowLendRepository.observeRecords(),
            budgetRepository?.observeBudgets() ?: flowOf(emptyList())
        ) { debts, emis, records, budgets -> Obligations(debts, emis, records, budgets) }

        // Archived categories come too: a recent row may be filed under one
        // the user has since hidden, and it still has to render with its name.
        val references = combine(
            accountRepository.observeActiveAccounts(),
            categoryRepository.observeCategories()
        ) { accounts, categories -> References(accounts, categories) }

        return combine(
            profile,
            references,
            ledger,
            obligations
        ) { user, reference, entries, obligation ->
            buildSnapshot(user, reference, entries, obligation)
        }
    }

    private fun buildSnapshot(
        profile: Profile,
        references: References,
        ledger: Ledger,
        obligations: Obligations
    ): DashboardSnapshot {
        val accounts = references.accounts
        val today = todayProvider()
        val month = monthOf(today)
        val transactions = ledger.window
        val (debts, emis, records, budgets) = obligations

        return DashboardSnapshot(
            userName = profile.name,
            currencySymbol = profile.currencySymbol,
            today = today,

            accounts = accounts,
            categories = references.categories,
            totalBalance = FinancialCalculator.totalBalance(accounts),

            todaySummary = FinancialCalculator.summaryForDay(transactions, today),
            monthSummary = FinancialCalculator.summaryForMonth(transactions, month),
            weeklyTrend = FinancialCalculator.dailyTrend(transactions, today, TREND_DAYS),

            netPosition = FinancialCalculator.netPosition(accounts, debts, emis, records),

            totalDebtOwed = FinancialCalculator.totalDebtOutstanding(debts),
            totalMoneyBorrowed = FinancialCalculator.totalBorrowed(records),
            totalMoneyLent = FinancialCalculator.totalLent(records),
            remainingEmiAmount = FinancialCalculator.totalEmiOutstanding(emis),
            emiDueThisMonth = FinancialCalculator.emiDueInMonth(emis, month),

            upcomingPayments = FinancialCalculator.upcomingPayments(
                debts = debts,
                emis = emis,
                records = records,
                today = today,
                limit = UPCOMING_PAYMENT_LIMIT
            ),
            // Already ordered and limited by the query.
            recentTransactions = ledger.recent,

            // The window always covers the month to date, which is exactly
            // the period a monthly budget is measured over.
            budgets = BudgetMeasure.measure(
                budgets = budgets,
                transactions = transactions.filter { isInMonth(it.date, month) },
                categories = CategoryLookup(references.categories)
            )
        )
    }

    private companion object {
        const val DEFAULT_CURRENCY_SYMBOL = "$"
        const val UPCOMING_PAYMENT_LIMIT = 4
        const val TREND_DAYS = 7
    }
}
