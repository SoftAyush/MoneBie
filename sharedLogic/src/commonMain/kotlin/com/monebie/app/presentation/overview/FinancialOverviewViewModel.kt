package com.monebie.app.presentation.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monebie.app.domain.model.FinancialOverview
import com.monebie.app.domain.usecase.overview.GetFinancialOverviewUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface FinancialOverviewUiState {

    data object Loading : FinancialOverviewUiState

    data class Content(val overview: FinancialOverview) : FinancialOverviewUiState

    /** Reported rather than shown as an empty balance sheet, which would read as "you own nothing". */
    data class Error(val message: String) : FinancialOverviewUiState
}

/**
 * Holds no financial logic: the overview arrives fully composed and this class
 * only wraps it in a UI state and handles failure.
 */
class FinancialOverviewViewModel(
    getFinancialOverviewUseCase: GetFinancialOverviewUseCase
) : ViewModel() {

    val uiState: StateFlow<FinancialOverviewUiState> = getFinancialOverviewUseCase()
        .map<FinancialOverview, FinancialOverviewUiState> {
            FinancialOverviewUiState.Content(it)
        }
        .catch { throwable ->
            emit(
                FinancialOverviewUiState.Error(
                    throwable.message ?: "Could not work out your financial position."
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FinancialOverviewUiState.Loading
        )
}
