package com.monebie.app.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monebie.app.core.Money
import com.monebie.app.domain.calculation.FinancialCalculator
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.AccountType
import com.monebie.app.domain.usecase.account.ArchiveAccountUseCase
import com.monebie.app.domain.usecase.account.GetAccountsUseCase
import com.monebie.app.domain.usecase.account.SaveAccountUseCase
import com.monebie.app.domain.usecase.user.GetCurrentUserUseCase
import com.monebie.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountListUiState(
    val accounts: List<Account> = emptyList(),
    val totalBalance: Money = Money.ZERO,
    val currencySymbol: String = "$",
    val isLoading: Boolean = true
) {
    /** Blocks archiving the last one: the app routes to setup when none are left. */
    val canArchive: Boolean get() = accounts.size > 1
}

/** The add/edit sheet. */
data class AccountFormState(
    val isVisible: Boolean = false,
    /** Null while creating. */
    val editingId: Long? = null,
    val name: String = "",
    val type: AccountType = AccountType.BANK,
    val openingBalanceInput: String = "",
    val isSaving: Boolean = false,
    val nameError: String? = null,
    val balanceError: String? = null
) {
    val isEditing: Boolean get() = editingId != null
}

sealed interface AccountEvent {
    data class Saved(val name: String) : AccountEvent
    data class Archived(val name: String) : AccountEvent
    data object LastAccountRefused : AccountEvent
}

class AccountListViewModel(
    getAccountsUseCase: GetAccountsUseCase,
    getSelectedCurrencySymbolUseCase: GetSelectedCurrencySymbolUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val saveAccountUseCase: SaveAccountUseCase,
    private val archiveAccountUseCase: ArchiveAccountUseCase
) : ViewModel() {

    val uiState: StateFlow<AccountListUiState> = combine(
        getAccountsUseCase(),
        getSelectedCurrencySymbolUseCase()
    ) { accounts, symbol ->
        AccountListUiState(
            accounts = accounts,
            // Same calculator the dashboard uses, so the two totals cannot disagree.
            totalBalance = FinancialCalculator.totalBalance(accounts),
            currencySymbol = symbol ?: "$",
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountListUiState()
    )

    private val _form = MutableStateFlow(AccountFormState())
    val form: StateFlow<AccountFormState> = _form.asStateFlow()

    private val _event = MutableStateFlow<AccountEvent?>(null)
    val event: StateFlow<AccountEvent?> = _event.asStateFlow()

    fun startCreating() {
        _form.value = AccountFormState(isVisible = true)
    }

    fun startEditing(account: Account) {
        _form.value = AccountFormState(
            isVisible = true,
            editingId = account.id,
            name = account.name,
            type = account.type
        )
    }

    fun dismissForm() {
        _form.value = AccountFormState()
    }

    fun setName(value: String) {
        _form.update { it.copy(name = value, nameError = null) }
    }

    fun setType(type: AccountType) {
        _form.update { it.copy(type = type) }
    }

    fun setOpeningBalance(input: String) {
        // Only what can be part of a number, so the field cannot hold text that
        // would later fail to parse.
        val sanitised = input.filter { it.isDigit() || it == '.' }
        _form.update { it.copy(openingBalanceInput = sanitised, balanceError = null) }
    }

    fun save() {
        val current = _form.value
        if (current.isSaving) return

        _form.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val currencyCode = getCurrentUserUseCase().first()?.currencyCode ?: FALLBACK_CURRENCY

            when (
                val result = saveAccountUseCase(
                    id = current.editingId,
                    name = current.name,
                    type = current.type,
                    openingBalanceInput = current.openingBalanceInput,
                    currencyCode = currencyCode
                )
            ) {
                is SaveAccountUseCase.Result.Saved -> {
                    _event.value = AccountEvent.Saved(current.name.trim())
                    _form.value = AccountFormState()
                }

                is SaveAccountUseCase.Result.Invalid -> _form.update {
                    // The message lands on whichever field it is about, so the
                    // user is not left hunting for what went wrong.
                    if (result.message.contains("balance", ignoreCase = true)) {
                        it.copy(isSaving = false, balanceError = result.message)
                    } else {
                        it.copy(isSaving = false, nameError = result.message)
                    }
                }
            }
        }
    }

    fun archive(account: Account) {
        viewModelScope.launch {
            _event.value = when (archiveAccountUseCase(account.id)) {
                is ArchiveAccountUseCase.Result.Archived -> AccountEvent.Archived(account.name)
                is ArchiveAccountUseCase.Result.LastAccount -> AccountEvent.LastAccountRefused
            }
        }
    }

    fun consumeEvent() {
        _event.value = null
    }

    private companion object {
        const val FALLBACK_CURRENCY = "USD"
    }
}
