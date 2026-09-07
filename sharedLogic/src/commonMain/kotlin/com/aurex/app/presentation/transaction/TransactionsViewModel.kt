package com.aurex.app.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurex.app.core.addMonths
import com.aurex.app.core.firstDayOfMonth
import com.aurex.app.core.formatMonthYear
import com.aurex.app.core.getCurrentDay
import com.aurex.app.core.monthOf
import com.aurex.app.core.shiftMonth
import com.aurex.app.domain.calculation.FinancialCalculator
import com.aurex.app.domain.model.Account
import com.aurex.app.domain.model.CategoryLookup
import com.aurex.app.domain.model.TransactionCategories
import com.aurex.app.domain.model.TransactionCategory
import com.aurex.app.domain.model.DayGroup
import com.aurex.app.domain.model.PeriodSummary
import com.aurex.app.domain.model.TransactionFilter
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.domain.usecase.account.GetAccountsUseCase
import com.aurex.app.domain.usecase.category.GetActiveCategoriesUseCase
import com.aurex.app.domain.usecase.transaction.DeleteTransactionUseCase
import com.aurex.app.domain.usecase.transaction.GetTransactionsInRangeUseCase
import com.aurex.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val isLoading: Boolean = true,
    /** yyyy-MM */
    val month: String = "",
    val monthLabel: String = "",
    val canGoToNextMonth: Boolean = false,
    val filter: TransactionFilter = TransactionFilter(),
    /** Totals for the whole month, unaffected by the active filter. */
    val monthSummary: PeriodSummary = PeriodSummary(),
    /** Totals for what is actually on screen. */
    val visibleSummary: PeriodSummary = PeriodSummary(),
    val groups: List<DayGroup> = emptyList(),
    val accounts: List<Account> = emptyList(),
    /** What the filter sheet can offer, and what labels a row. */
    val categories: List<TransactionCategory> = TransactionCategories.all,
    val currencySymbol: String = "$"
) {
    val isEmpty: Boolean get() = !isLoading && groups.isEmpty()

    /** Distinguishes "no transactions this month" from "none match your filter". */
    val isEmptyBecauseOfFilter: Boolean get() = isEmpty && filter.isActive

    fun accountName(accountId: Long?): String =
        accounts.firstOrNull { it.id == accountId }?.name ?: "Account"

    /** Categories the user can still file against, for the filter sheet. */
    fun categoriesFor(type: TransactionType): List<TransactionCategory> =
        categories.filter { it.appliesTo == type && !it.isArchived }

    private val lookup: CategoryLookup get() = CategoryLookup(categories)

    /**
     * The category behind a row.
     *
     * Resolved here rather than in the row so a renamed category shows its new
     * name everywhere at once, and a deleted one still renders.
     */
    fun category(categoryId: String): TransactionCategory = lookup[categoryId]
}

/**
 * Drives the transaction list.
 *
 * Loads one month at a time and applies filters in memory, so paging back
 * through history costs one bounded query per month rather than growing with
 * the size of the ledger.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    getTransactionsInRangeUseCase: GetTransactionsInRangeUseCase,
    getAccountsUseCase: GetAccountsUseCase,
    getActiveCategoriesUseCase: GetActiveCategoriesUseCase,
    getSelectedCurrencySymbolUseCase: GetSelectedCurrencySymbolUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val todayProvider: () -> String = ::getCurrentDay
) : ViewModel() {

    private val currentMonth = monthOf(todayProvider())

    private val selectedMonth = MutableStateFlow(currentMonth)
    private val filter = MutableStateFlow(TransactionFilter())

    private val monthTransactions = selectedMonth.flatMapLatest { month ->
        val start = firstDayOfMonth(month)
        getTransactionsInRangeUseCase(start, addMonths(start, 1))
    }

    /** Accounts, categories and the currency move together and rarely change. */
    private val reference = combine(
        getAccountsUseCase(),
        getActiveCategoriesUseCase(),
        getSelectedCurrencySymbolUseCase()
    ) { accounts, categories, symbol -> Reference(accounts, categories, symbol ?: "$") }

    val uiState: StateFlow<TransactionsUiState> = combine(
        selectedMonth,
        filter,
        monthTransactions,
        reference
    ) { month, activeFilter, transactions, reference ->
        val visible = transactions.filter { activeFilter.matches(it) }
        TransactionsUiState(
            isLoading = false,
            month = month,
            monthLabel = formatMonthYear(firstDayOfMonth(month)),
            canGoToNextMonth = month < currentMonth,
            filter = activeFilter,
            monthSummary = FinancialCalculator.summarise(transactions),
            visibleSummary = FinancialCalculator.summarise(visible),
            groups = FinancialCalculator.dayGroups(visible),
            accounts = reference.accounts,
            categories = reference.categories,
            currencySymbol = reference.currencySymbol
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState(
            month = currentMonth,
            monthLabel = formatMonthYear(firstDayOfMonth(currentMonth))
        )
    )

    fun showPreviousMonth() {
        selectedMonth.update { shiftMonth(it, -1) }
    }

    /** Guarded so the list cannot page into months that have not happened yet. */
    fun showNextMonth() {
        selectedMonth.update { if (it < currentMonth) shiftMonth(it, 1) else it }
    }

    fun setTypeFilter(type: TransactionType?) {
        filter.update { it.copy(type = type) }
    }

    fun setAccountFilter(accountId: Long?) {
        filter.update { it.copy(accountId = accountId) }
    }

    fun setCategoryFilter(categoryId: String?) {
        filter.update { it.copy(categoryId = categoryId) }
    }

    fun clearFilters() {
        filter.update { it.cleared() }
    }

    /**
     * Deleting reverses the transaction's balance effect. The confirmation that
     * guards it lives in the UI; by the time this is called the user has agreed.
     */
    fun deleteTransaction(id: Long) {
        viewModelScope.launch { deleteTransactionUseCase(id) }
    }
}

/** The slowly-changing half of the screen's state, combined once. */
private data class Reference(
    val accounts: List<Account>,
    val categories: List<TransactionCategory>,
    val currencySymbol: String
)
