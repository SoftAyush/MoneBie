package com.aurex.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurex.app.domain.model.DashboardSnapshot
import com.aurex.app.domain.usecase.dashboard.GetDashboardSnapshotUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Holds no financial logic of its own: the snapshot arrives fully computed and
 * this class only wraps it in a UI state and handles failure.
 */
class DashboardViewModel(
    getDashboardSnapshotUseCase: GetDashboardSnapshotUseCase
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = getDashboardSnapshotUseCase()
        .map<DashboardSnapshot, DashboardUiState> { DashboardUiState.Content(it) }
        .catch { throwable ->
            emit(DashboardUiState.Error(throwable.message ?: "Could not load your finances."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )
}
