package com.aurex.app.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurex.app.core.getCurrentDay
import com.aurex.app.core.monthOf
import com.aurex.app.core.shiftMonth
import com.aurex.app.domain.model.SpendingInsights
import com.aurex.app.domain.usecase.insights.GetSpendingInsightsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
    private val selectedMonth = MutableStateFlow(currentMonth)

    val uiState: StateFlow<SpendingInsights> = selectedMonth
        .flatMapLatest { month -> getSpendingInsightsUseCase(month) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SpendingInsights(month = currentMonth)
        )

    fun showPreviousMonth() {
        selectedMonth.update { shiftMonth(it, -1) }
    }

    /** Guarded so insights cannot page into a month that has not happened. */
    fun showNextMonth() {
        selectedMonth.update { if (it < currentMonth) shiftMonth(it, 1) else it }
    }
}
