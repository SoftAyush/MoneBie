package com.aurex.app.presentation.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurex.app.domain.usecase.reminders.GetUpcomingPaymentsUseCase
import com.aurex.app.domain.usecase.reminders.ReminderBoard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class RemindersViewModel(
    getUpcomingPaymentsUseCase: GetUpcomingPaymentsUseCase
) : ViewModel() {

    val uiState: StateFlow<ReminderBoard> = getUpcomingPaymentsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReminderBoard()
        )
}
