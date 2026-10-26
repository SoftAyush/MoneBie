package com.monebie.app.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monebie.app.core.AppCalendar
import com.monebie.app.core.firstDayAfterMonth
import com.monebie.app.core.firstDayOfMonth
import com.monebie.app.core.formatMonth
import com.monebie.app.core.getCurrentDay
import com.monebie.app.core.monthKeyOf
import com.monebie.app.core.monthOf
import com.monebie.app.core.shiftMonth
import com.monebie.app.domain.calculation.FinancialCalculator
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.CategoryLookup
import com.monebie.app.domain.model.TransactionCategories
import com.monebie.app.domain.model.TransactionCategory
import com.monebie.app.domain.model.DayGroup
import com.monebie.app.domain.model.PeriodSummary
import com.monebie.app.domain.model.TransactionFilter
import com.monebie.app.domain.model.TransactionType
import com.monebie.app.domain.usecase.account.GetAccountsUseCase
import com.monebie.app.domain.usecase.category.GetActiveCategoriesUseCase
import com.monebie.app.domain.usecase.transaction.DeleteTransactionUseCase
import com.monebie.app.domain.usecase.transaction.GetTransactionsInRangeUseCase
import com.monebie.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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

    /**
     * How many months back from the current one the user has paged. Kept as a
     * count rather than a month so switching calendars in Settings keeps their
     * place instead of stranding them on a month of the other calendar.
     */
    private val monthsBack = MutableStateFlow(0)

    private val selectedMonth = combine(AppCalendar.system, monthsBack) { system, back ->
        shiftMonth(monthKeyOf(todayProvider(), system), -back)
    }.distinctUntilChanged()
    private val filter = MutableStateFlow(TransactionFilter())

    private val monthTransactions = selectedMonth.flatMapLatest { month ->
        getTransactionsInRangeUseCase(firstDayOfMonth(month), firstDayAfterMonth(month))
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
            monthLabel = formatMonth(month),
            canGoToNextMonth = todayProvider() >= firstDayAfterMonth(month),
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
            monthLabel = formatMonth(currentMonth)
        )
    )

    fun showPreviousMonth() {
        monthsBack.update { it + 1 }
    }

    /** Guarded so the list cannot page into months that have not happened yet. */
    fun showNextMonth() {
        monthsBack.update { (it - 1).coerceAtLeast(0) }
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
