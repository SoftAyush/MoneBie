package com.monebie.app.presentation.emi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monebie.app.core.Money
import com.monebie.app.core.daysBetween
import com.monebie.app.core.AppCalendar
import com.monebie.app.core.getCurrentDay
import com.monebie.app.core.monthOf
import com.monebie.app.core.totalOf
import com.monebie.app.domain.calculation.FinancialCalculator
import com.monebie.app.domain.model.Emi
import com.monebie.app.domain.model.EmiStatus
import com.monebie.app.domain.usecase.emi.GetEmisUseCase
import com.monebie.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** An EMI paired with the status and due-date facts resolved for today. */
data class EmiListItem(
    val emi: Emi,
    val status: EmiStatus,
    val nextDueDate: String?,
    val daysUntilDue: Long?
)

data class EmiListUiState(
    val isLoading: Boolean = true,
    val items: List<EmiListItem> = emptyList(),
    val statusFilter: EmiStatus? = null,
    /** Total still payable across every unfinished EMI. */
    val totalRemaining: Money = Money.ZERO,
    val totalPaid: Money = Money.ZERO,
    /** Installments falling due in the current calendar month. */
    val dueThisMonth: Money = Money.ZERO,
    val remainingInstallments: Int = 0,
    /** Installments settled and scheduled across every plan, for the ring. */
    val paidInstallments: Int = 0,
    val totalInstallments: Int = 0,
    val activeCount: Int = 0,
    val overdueCount: Int = 0,
    val overallProgress: Float = 0f,
    /**
     * How many plans sit in each status, counted across everything rather than
     * the filtered view - it is what lets a filter say in advance whether it
     * leads anywhere.
     */
    val statusCounts: Map<EmiStatus, Int> = emptyMap(),
    /**
     * The installment falling due soonest, on a plan that is still running.
     *
     * An EMI is a fixed payment on a known date, so this - not a total - is
     * what the screen is opened to find.
     */
    val nextInstallment: EmiListItem? = null,
    val currencySymbol: String = "$"
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()

    val isEmptyBecauseOfFilter: Boolean get() = isEmpty && statusFilter != null

    /** Total across every status, for the "All" pill. */
    val totalCount: Int get() = statusCounts.values.sum()

    fun countFor(status: EmiStatus): Int = statusCounts[status] ?: 0
}

class EmiListViewModel(
    getEmisUseCase: GetEmisUseCase,
    getSelectedCurrencySymbolUseCase: GetSelectedCurrencySymbolUseCase,
    private val todayProvider: () -> String = ::getCurrentDay
) : ViewModel() {

    private val statusFilter = MutableStateFlow<EmiStatus?>(null)

    val uiState: StateFlow<EmiListUiState> = combine(
        getEmisUseCase(),
        getSelectedCurrencySymbolUseCase(),
        statusFilter,
        // "Due this month" depends on whether months are AD or BS.
        AppCalendar.system
    ) { emis, symbol, filter, _ ->
        val today = todayProvider()

        val all = emis.map { emi ->
            EmiListItem(
                emi = emi,
                status = FinancialCalculator.emiStatus(emi, today),
                nextDueDate = emi.nextDueDate,
                daysUntilDue = emi.nextDueDate?.let { daysBetween(today, it) }
            )
        }

        val totalInstallments = emis.sumOf { it.totalInstallments }
        val paidInstallments = emis.sumOf { it.installmentsPaid }

        EmiListUiState(
            isLoading = false,
            items = all
                .filter { filter == null || it.status == filter }
                // Overdue first, then whatever falls due soonest; completed last.
                .sortedWith(
                    compareBy(
                        { it.status != EmiStatus.OVERDUE },
                        { it.status == EmiStatus.COMPLETED },
                        { it.daysUntilDue ?: Long.MAX_VALUE }
                    )
                ),
            statusFilter = filter,
            totalRemaining = FinancialCalculator.totalEmiOutstanding(emis),
            totalPaid = emis.totalOf { it.amountPaid },
            dueThisMonth = FinancialCalculator.emiDueInMonth(emis, monthOf(today)),
            remainingInstallments = emis.sumOf { it.remainingInstallments },
            paidInstallments = paidInstallments,
            totalInstallments = totalInstallments,
            activeCount = all.count { it.status != EmiStatus.COMPLETED },
            overdueCount = all.count { it.status == EmiStatus.OVERDUE },
            statusCounts = EmiStatus.entries.associateWith { status ->
                all.count { it.status == status }
            },
            // Soonest first, and only among plans still running - a finished
            // plan can still carry the date its last installment fell on.
            nextInstallment = all
                .filter { it.status != EmiStatus.COMPLETED && it.daysUntilDue != null }
                .minByOrNull { it.daysUntilDue!! },
            overallProgress = if (totalInstallments == 0) {
                0f
            } else {
                (paidInstallments.toFloat() / totalInstallments.toFloat()).coerceIn(0f, 1f)
            },
            currencySymbol = symbol ?: "$"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EmiListUiState()
    )

    fun setStatusFilter(status: EmiStatus?) {
        statusFilter.update { status }
    }
}
