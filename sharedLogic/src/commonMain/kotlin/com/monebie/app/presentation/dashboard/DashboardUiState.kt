package com.monebie.app.presentation.dashboard

import com.monebie.app.domain.model.DashboardSnapshot

sealed interface DashboardUiState {

    data object Loading : DashboardUiState

    data class Content(val snapshot: DashboardSnapshot) : DashboardUiState

    /**
     * Kept as a first-class state rather than a silent empty dashboard: showing
     * zero balances when a read actually failed is worse than saying so.
     */
    data class Error(val message: String) : DashboardUiState
}
