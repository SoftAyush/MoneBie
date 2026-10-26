package com.monebie.app.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monebie.app.core.Money
import com.monebie.app.core.getCurrentDay
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.CategoryLookup
import com.monebie.app.domain.model.RecurringTransaction
import com.monebie.app.domain.model.TransactionCategory
import com.monebie.app.domain.model.TransactionType
import com.monebie.app.domain.usecase.account.GetAccountsUseCase
import com.monebie.app.domain.usecase.category.GetCategoriesUseCase
import com.monebie.app.domain.usecase.recurring.DeleteRecurringTransactionUseCase
import com.monebie.app.domain.usecase.recurring.GetRecurringTransactionsUseCase
import com.monebie.app.domain.usecase.recurring.PostDueRecurringUseCase
import com.monebie.app.domain.usecase.recurring.PostRecurringNowUseCase
import com.monebie.app.domain.usecase.recurring.SetRecurringPausedUseCase
import com.monebie.app.domain.usecase.recurring.SkipRecurringOccurrenceUseCase
import com.monebie.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A schedule with everything the row needs resolved, so the list renders no logic. */
data class RecurringListItem(
    val schedule: RecurringTransaction,
    val category: TransactionCategory,
    val accountName: String,
    val daysUntilNext: Long
) {
    val isOverdue: Boolean get() = schedule.isActive && daysUntilNext < 0
}

data class RecurringListUiState(
    val isLoading: Boolean = true,
    val items: List<RecurringListItem> = emptyList(),
    val currencySymbol: String = "$"
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()

    val active: List<RecurringListItem> get() = items.filter { it.schedule.isActive }

    val paused: List<RecurringListItem> get() = items.filter { it.schedule.isPaused }

    val ended: List<RecurringListItem> get() = items.filter { !it.schedule.isPaused && it.schedule.hasEnded }

    /** What the active schedules cost each month, expenses only. */
    val monthlyOutgoing: Money
        get() = Money(
            active.filter { it.schedule.type == TransactionType.EXPENSE }
                .sumOf { it.schedule.monthlyEquivalent.minorUnits }
        )

    /** What the active schedules bring in each month. */
    val monthlyIncoming: Money
        get() = Money(
            active.filter { it.schedule.type == TransactionType.INCOME }
                .sumOf { it.schedule.monthlyEquivalent.minorUnits }
        )

    /** The next occurrence across everything active, for the hero. */
    val nextUp: RecurringListItem? get() = active.minByOrNull { it.daysUntilNext }
}

sealed interface RecurringEvent {
    data class Posted(val count: Int, val names: List<String>) : RecurringEvent
    data class PostedOne(val name: String) : RecurringEvent
    data class Skipped(val name: String) : RecurringEvent
    data class Paused(val name: String, val paused: Boolean) : RecurringEvent
    data class Deleted(val name: String) : RecurringEvent
}

class RecurringListViewModel(
    getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase,
    getCategoriesUseCase: GetCategoriesUseCase,
    getAccountsUseCase: GetAccountsUseCase,
    getSelectedCurrencySymbolUseCase: GetSelectedCurrencySymbolUseCase,
    private val postDueRecurringUseCase: PostDueRecurringUseCase,
    private val postRecurringNowUseCase: PostRecurringNowUseCase,
    private val skipRecurringOccurrenceUseCase: SkipRecurringOccurrenceUseCase,
    private val setRecurringPausedUseCase: SetRecurringPausedUseCase,
    private val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase,
    private val todayProvider: () -> String = ::getCurrentDay
) : ViewModel() {

    val uiState: StateFlow<RecurringListUiState> = combine(
        getRecurringTransactionsUseCase(),
        getCategoriesUseCase(),
        getAccountsUseCase(),
        getSelectedCurrencySymbolUseCase()
    ) { schedules, categories, accounts, symbol ->
        val today = todayProvider()
        val lookup = CategoryLookup(categories)
        val accountNames = accounts.associate { it.id to it.name }
        RecurringListUiState(
            isLoading = false,
            items = schedules.map { schedule ->
                RecurringListItem(
                    schedule = schedule,
                    category = lookup[schedule.categoryId],
                    accountName = accountNames[schedule.accountId] ?: "Account",
                    daysUntilNext = schedule.daysUntilNext(today)
                )
            },
            currencySymbol = symbol ?: "$"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringListUiState()
    )

    private val _event = MutableStateFlow<RecurringEvent?>(null)
    val event: StateFlow<RecurringEvent?> = _event.asStateFlow()

    /**
     * Catches up on anything due.
     *
     * Called by the screen every time it comes to the front rather than once
     * here: the view model outlives a trip to the add form, and a schedule
     * created there for today has to be posted when the list is back, not on
     * the next launch.
     */
    fun postDue() {
        viewModelScope.launch {
            val result = postDueRecurringUseCase()
            if (!result.isEmpty) {
                _event.value = RecurringEvent.Posted(result.postedCount, result.postedNames)
            }
        }
    }

    fun postNow(item: RecurringListItem) {
        viewModelScope.launch {
            postRecurringNowUseCase(item.schedule.id)
            _event.value = RecurringEvent.PostedOne(item.schedule.name)
        }
    }

    fun skipNext(item: RecurringListItem) {
        viewModelScope.launch {
            skipRecurringOccurrenceUseCase(item.schedule.id)
            _event.value = RecurringEvent.Skipped(item.schedule.name)
        }
    }

    fun setPaused(item: RecurringListItem, paused: Boolean) {
        viewModelScope.launch {
            setRecurringPausedUseCase(item.schedule.id, paused)
            _event.value = RecurringEvent.Paused(item.schedule.name, paused)
        }
    }

    fun delete(item: RecurringListItem) {
        viewModelScope.launch {
            deleteRecurringTransactionUseCase(item.schedule.id)
            _event.value = RecurringEvent.Deleted(item.schedule.name)
        }
    }

    fun consumeEvent() {
        _event.value = null
    }
}
