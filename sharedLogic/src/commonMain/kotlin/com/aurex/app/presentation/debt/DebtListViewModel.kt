package com.aurex.app.presentation.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurex.app.core.Money
import com.aurex.app.core.getCurrentDay
import com.aurex.app.core.totalOf
import com.aurex.app.domain.calculation.FinancialCalculator
import com.aurex.app.domain.model.Debt
import com.aurex.app.domain.model.DebtStatus
import com.aurex.app.domain.usecase.debt.GetDebtsUseCase
import com.aurex.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** A debt paired with the status resolved for today, so the list renders no logic of its own. */
data class DebtListItem(
    val debt: Debt,
    val status: DebtStatus,
    val nextDueDate: String?,
    val daysUntilDue: Long?
)

data class DebtListUiState(
    val isLoading: Boolean = true,
    val items: List<DebtListItem> = emptyList(),
    val statusFilter: DebtStatus? = null,
    /** Original amount across every unsettled debt. */
    val totalBorrowed: Money = Money.ZERO,
    val totalRemaining: Money = Money.ZERO,
    val totalPaid: Money = Money.ZERO,
    val overallProgress: Float = 0f,
    val activeCount: Int = 0,
    val overdueCount: Int = 0,
    /**
     * How many debts sit in each status, counted across everything rather than
     * the filtered view - it is what lets a filter say in advance whether it
     * leads anywhere.
     */
    val statusCounts: Map<DebtStatus, Int> = emptyMap(),
    /**
     * The debt whose payment falls due soonest, still unsettled.
     *
     * The one thing a debt screen is actually asked at a glance. A total says
     * how deep the hole is; this says what to do about it this week.
     */
    val nextPayment: DebtListItem? = null,
    val currencySymbol: String = "$"
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()

    val isEmptyBecauseOfFilter: Boolean get() = isEmpty && statusFilter != null

    /** Total across every status, for the "All" pill. */
    val totalCount: Int get() = statusCounts.values.sum()

    fun countFor(status: DebtStatus): Int = statusCounts[status] ?: 0
}

class DebtListViewModel(
    getDebtsUseCase: GetDebtsUseCase,
    getSelectedCurrencySymbolUseCase: GetSelectedCurrencySymbolUseCase,
    private val todayProvider: () -> String = ::getCurrentDay
) : ViewModel() {

    private val statusFilter = MutableStateFlow<DebtStatus?>(null)

    val uiState: StateFlow<DebtListUiState> = combine(
        getDebtsUseCase(),
        getSelectedCurrencySymbolUseCase(),
        statusFilter
    ) { debts, symbol, filter ->
        val today = todayProvider()

        val all = debts.map { debt ->
            val nextDue = FinancialCalculator.nextDebtDueDate(debt, today)
            DebtListItem(
                debt = debt,
                status = FinancialCalculator.debtStatus(debt, today),
                nextDueDate = nextDue,
                daysUntilDue = nextDue?.let { com.aurex.app.core.daysBetween(today, it) }
            )
        }

        // Headline totals always describe every debt, not the filtered subset:
        // narrowing the list must not look like the user owes less.
        val unsettled = debts.filter { !it.isSettled }

        DebtListUiState(
            isLoading = false,
            items = all
                .filter { filter == null || it.status == filter }
                // Overdue first, then by how soon the next payment falls due.
                .sortedWith(
                    compareBy(
                        { it.status != DebtStatus.OVERDUE },
                        { it.status == DebtStatus.PAID },
                        { it.daysUntilDue ?: Long.MAX_VALUE }
                    )
                ),
            statusFilter = filter,
            totalBorrowed = unsettled.totalOf { it.originalAmount },
            totalRemaining = FinancialCalculator.totalDebtOutstanding(debts),
            totalPaid = debts.totalOf { it.amountPaid },
            overallProgress = FinancialCalculator.progress(
                paid = debts.totalOf { it.amountPaid },
                total = debts.totalOf { it.originalAmount }
            ),
            activeCount = all.count { it.status != DebtStatus.PAID },
            overdueCount = all.count { it.status == DebtStatus.OVERDUE },
            statusCounts = DebtStatus.entries.associateWith { status ->
                all.count { it.status == status }
            },
            // Soonest first, and only among debts that still owe something -
            // a settled debt can still carry a due date it will never meet.
            nextPayment = all
                .filter { it.status != DebtStatus.PAID && it.daysUntilDue != null }
                .minByOrNull { it.daysUntilDue!! },
            currencySymbol = symbol ?: "$"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebtListUiState()
    )

    fun setStatusFilter(status: DebtStatus?) {
        statusFilter.update { status }
    }
}
