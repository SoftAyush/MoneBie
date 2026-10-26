package com.aurex.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurex.app.core.getCurrentDateString
import com.aurex.app.domain.model.Currency
import com.aurex.app.domain.usecase.currency.GetCurrenciesUseCase
import com.aurex.app.domain.usecase.user.SaveUserUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileSetupViewModel(
    private val getCurrenciesUseCase: GetCurrenciesUseCase,
    private val saveUserUseCase: SaveUserUseCase
) : ViewModel() {

    val currencies: StateFlow<List<Currency>> = getCurrenciesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveUserProfile(name: String, currencyCode: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val dateStr = getCurrentDateString()
            saveUserUseCase(name, dateStr, currencyCode)
            onComplete()
        }
    }
}
