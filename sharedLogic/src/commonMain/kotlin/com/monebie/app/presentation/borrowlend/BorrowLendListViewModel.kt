package com.monebie.app.presentation.borrowlend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monebie.app.core.Money
import com.monebie.app.core.daysBetween
import com.monebie.app.core.getCurrentDay
import com.monebie.app.domain.calculation.FinancialCalculator
import com.monebie.app.domain.model.BorrowLendRecord
import com.monebie.app.domain.model.LoanDirection
import com.monebie.app.domain.model.LoanStatus
import com.monebie.app.domain.usecase.borrowlend.GetLoanRecordsUseCase
import com.monebie.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** A record paired with the status resolved for today. */
data class LoanListItem(
    val record: BorrowLendRecord,
    val status: LoanStatus,
    val daysUntilDue: Long?
)

data class BorrowLendUiState(
    val isLoading: Boolean = true,
    /** Which side of the arrangement the user is currently looking at. */
    val direction: LoanDirection = LoanDirection.LENT,
    val showSettled: Boolean = false,
    val items: List<LoanListItem> = emptyList(),

    /** Totals for both sides, always shown together so the contrast is visible. */
    val totalOwedByUser: Money = Money.ZERO,
    val totalOwedToUser: Money = Money.ZERO,
    val openBorrowCount: Int = 0,
    val openLendCount: Int = 0,
    val overdueCount: Int = 0,

    /**
     * The arrangement due soonest, across both directions.
     *
     * Deliberately not filtered by the direction on screen: the thing that
     * needs a conversation next does not stop mattering because the user is
     * currently reading the other side, and the hero already speaks for both.
     */
    val nextDue: LoanListItem? = null,

    val currencySymbol: String = "$"
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()

    /** Net across both directions: positive means more is owed to the user than by them. */
    val netPosition: Money get() = totalOwedToUser - totalOwedByUser
}

/**
 * Drives the borrow-and-lend list.
 *
 * The two directions are shown one at a time because mixing them in a single
 * list is how people misread which way the money goes, but the headline keeps
 * both totals on screen at once so the contrast is never lost.
 */
class BorrowLendListViewModel(
    getLoanRecordsUseCase: GetLoanRecordsUseCase,
    getSelectedCurrencySymbolUseCase: GetSelectedCurrencySymbolUseCase,
    private val todayProvider: () -> String = ::getCurrentDay
) : ViewModel() {

    private val direction = MutableStateFlow(LoanDirection.LENT)
    private val showSettled = MutableStateFlow(false)

    val uiState: StateFlow<BorrowLendUiState> = combine(
        getLoanRecordsUseCase(),
        getSelectedCurrencySymbolUseCase(),
        direction,
        showSettled
    ) { records, symbol, selectedDirection, includeSettled ->
        val today = todayProvider()

        val items = records
            .filter { it.direction == selectedDirection }
            .filter { includeSettled || !it.isSettled }
            .map { record ->
                LoanListItem(
                    record = record,
                    status = FinancialCalculator.loanStatus(record, today),
                    daysUntilDue = record.dueDate?.let { daysBetween(today, it) }
                )
            }
            // Overdue first, then soonest due; settled records sink to the bottom.
            .sortedWith(
                compareBy(
                    { it.status != LoanStatus.OVERDUE },
                    { it.status == LoanStatus.SETTLED },
                    { it.daysUntilDue ?: Long.MAX_VALUE }
                )
            )

        val open = records.filter { !it.isSettled }

        val soonest = open
            .filter { it.dueDate != null }
            .minByOrNull { daysBetween(today, it.dueDate!!) }
            ?.let { record ->
                LoanListItem(
                    record = record,
                    status = FinancialCalculator.loanStatus(record, today),
                    daysUntilDue = daysBetween(today, record.dueDate!!)
                )
            }

        BorrowLendUiState(
            isLoading = false,
            direction = selectedDirection,
            showSettled = includeSettled,
            items = items,
            totalOwedByUser = FinancialCalculator.totalBorrowed(records),
            totalOwedToUser = FinancialCalculator.totalLent(records),
            openBorrowCount = open.count { it.direction == LoanDirection.BORROWED },
            openLendCount = open.count { it.direction == LoanDirection.LENT },
            overdueCount = records.count {
                FinancialCalculator.loanStatus(it, today) == LoanStatus.OVERDUE
            },
            nextDue = soonest,
            currencySymbol = symbol ?: "$"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BorrowLendUiState()
    )

    fun setDirection(value: LoanDirection) {
        direction.update { value }
    }

    fun setShowSettled(value: Boolean) {
        showSettled.update { value }
    }
}
