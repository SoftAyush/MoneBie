package com.monebie.app.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monebie.app.core.AppCalendar
import com.monebie.app.core.getCurrentDay
import com.monebie.app.core.monthKeyOf
import com.monebie.app.core.monthOf
import com.monebie.app.core.shiftMonth
import com.monebie.app.domain.model.SpendingInsights
import com.monebie.app.domain.usecase.insights.GetSpendingInsightsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * One month of insights at a time, paged the same way as the transaction list
 * so the two screens navigate identically.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(
    getSpendingInsightsUseCase: GetSpendingInsightsUseCase,
    private val todayProvider: () -> String = ::getCurrentDay
) : ViewModel() {

    private val currentMonth = monthOf(todayProvider())
    // Months back from the current one; a count so a calendar switch keeps the user's place.
    private val monthsBack = MutableStateFlow(0)
    private val selectedMonth = combine(AppCalendar.system, monthsBack) { system, back ->
        shiftMonth(monthKeyOf(todayProvider(), system), -back)
    }.distinctUntilChanged()

    val uiState: StateFlow<SpendingInsights> = selectedMonth
        .flatMapLatest { month -> getSpendingInsightsUseCase(month) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SpendingInsights(month = currentMonth)
        )

    fun showPreviousMonth() {
        monthsBack.update { it + 1 }
    }

    /** Guarded so insights cannot page into a month that has not happened. */
    fun showNextMonth() {
        monthsBack.update { (it - 1).coerceAtLeast(0) }
    }
}
