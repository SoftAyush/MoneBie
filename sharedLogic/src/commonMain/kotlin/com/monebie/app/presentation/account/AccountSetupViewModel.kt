package com.monebie.app.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monebie.app.core.Money
import com.monebie.app.domain.model.AccountType
import com.monebie.app.domain.usecase.account.CreateAccountUseCase
import com.monebie.app.domain.usecase.user.GetCurrentUserUseCase
import com.monebie.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountSetupUiState(
    val isSaving: Boolean = false,
    val nameError: String? = null,
    val balanceError: String? = null
)

class AccountSetupViewModel(
    getSelectedCurrencySymbolUseCase: GetSelectedCurrencySymbolUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val createAccountUseCase: CreateAccountUseCase
) : ViewModel() {

    val currencySymbol: StateFlow<String> = getSelectedCurrencySymbolUseCase()
        .map { it ?: "$" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "$"
        )

    private val _uiState = MutableStateFlow(AccountSetupUiState())
    val uiState: StateFlow<AccountSetupUiState> = _uiState.asStateFlow()

    /**
     * Persists the first account, then completes setup.
     *
     * An empty balance is a valid "start from zero" rather than an error, but
     * text that is present and unparseable is reported instead of silently
     * becoming zero - a wrong opening balance is invisible later and skews the
     * net position permanently.
     */
    fun createAccount(
        name: String,
        type: AccountType,
        balanceInput: String,
        onComplete: () -> Unit
    ) {
        if (_uiState.value.isSaving) return

        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(nameError = "Give this account a name", balanceError = null) }
            return
        }

        val openingBalance = when {
            balanceInput.isBlank() -> Money.ZERO
            else -> Money.parse(balanceInput) ?: run {
                _uiState.update { it.copy(nameError = null, balanceError = "Enter a valid amount") }
                return
            }
        }

        _uiState.update { it.copy(isSaving = true, nameError = null, balanceError = null) }

        viewModelScope.launch {
            val currencyCode = getCurrentUserUseCase().first()?.currencyCode ?: FALLBACK_CURRENCY
            createAccountUseCase(
                name = trimmedName,
                type = type,
                openingBalance = openingBalance,
                currencyCode = currencyCode
            )
            _uiState.update { it.copy(isSaving = false) }
            onComplete()
        }
    }

    private companion object {
        const val FALLBACK_CURRENCY = "USD"
    }
}
