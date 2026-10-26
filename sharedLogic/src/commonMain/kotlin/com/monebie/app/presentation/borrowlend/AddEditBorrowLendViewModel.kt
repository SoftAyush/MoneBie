package com.monebie.app.presentation.borrowlend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monebie.app.core.Money
import com.monebie.app.core.getCurrentDay
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.BorrowLendDraft
import com.monebie.app.domain.model.LoanDirection
import com.monebie.app.domain.usecase.account.GetAccountsUseCase
import com.monebie.app.domain.usecase.borrowlend.GetLoanRecordUseCase
import com.monebie.app.domain.usecase.borrowlend.SaveLoanRecordUseCase
import com.monebie.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditLoanUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val direction: LoanDirection = LoanDirection.LENT,
    val personName: String = "",
    val amountInput: String = "",
    /** yyyy-MM-dd */
    val date: String = "",
    val dueDate: String? = null,
    val notes: String = "",
    val accountId: Long? = null,

    val accounts: List<Account> = emptyList(),
    val currencySymbol: String = "$",

    val isSaving: Boolean = false,
    val isFinished: Boolean = false,
    val nameError: String? = null,
    val amountError: String? = null
) {
    val isBorrowed: Boolean get() = direction == LoanDirection.BORROWED

    /** Explains, in the user's terms, what selecting an account will do. */
    val accountEffectHint: String
        get() = if (isBorrowed) {
            "The amount will be added to this account, since the money came to you."
        } else {
            "The amount will be taken from this account, since the money has left you."
        }

    /**
     * Direction, amount and account are locked once the record exists.
     *
     * Changing any of them would invalidate the principal movement already
     * posted and every repayment recorded against it.
     */
    val canEditTerms: Boolean get() = !isEditing
}

class AddEditBorrowLendViewModel(
    private val editingRecordId: Long? = null,
    initialDirection: LoanDirection = LoanDirection.LENT,
    private val getLoanRecordUseCase: GetLoanRecordUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getSelectedCurrencySymbolUseCase: GetSelectedCurrencySymbolUseCase,
    private val saveLoanRecordUseCase: SaveLoanRecordUseCase,
    private val todayProvider: () -> String = ::getCurrentDay
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddEditLoanUiState(direction = initialDirection, date = todayProvider())
    )
    val uiState: StateFlow<AddEditLoanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val accounts = getAccountsUseCase().first()
        val symbol = getSelectedCurrencySymbolUseCase().first() ?: "$"
        val existing = editingRecordId?.let { getLoanRecordUseCase.once(it) }

        if (existing != null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isEditing = true,
                    direction = existing.direction,
                    personName = existing.personName,
                    amountInput = existing.principalAmount.toMajorDouble().toString(),
                    date = existing.date,
                    dueDate = existing.dueDate,
                    notes = existing.notes.orEmpty(),
                    accountId = existing.accountId,
                    accounts = accounts,
                    currencySymbol = symbol
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                accounts = accounts,
                accountId = accounts.firstOrNull()?.id,
                currencySymbol = symbol
            )
        }
    }

    fun setDirection(direction: LoanDirection) {
        if (_uiState.value.isEditing) return
        _uiState.update { it.copy(direction = direction) }
    }

    fun setPersonName(value: String) =
        _uiState.update { it.copy(personName = value, nameError = null) }

    fun setAmount(value: String) {
        if (_uiState.value.isEditing) return
        val sanitised = value.filter { it.isDigit() || it == '.' }
        val first = sanitised.indexOf('.')
        val cleaned = if (first == -1) {
            sanitised
        } else {
            sanitised.substring(0, first + 1) + sanitised.substring(first + 1).replace(".", "")
        }
        _uiState.update { it.copy(amountInput = cleaned, amountError = null) }
    }

    fun setDate(day: String) = _uiState.update { it.copy(date = day) }

    fun setDueDate(day: String?) = _uiState.update { it.copy(dueDate = day) }

    fun setNotes(value: String) = _uiState.update { it.copy(notes = value) }

    fun setAccount(accountId: Long?) {
        if (_uiState.value.isEditing) return
        _uiState.update { it.copy(accountId = accountId) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val amount = Money.parse(state.amountInput)

        val nameError = if (state.personName.isBlank()) "Who is this with?" else null
        val amountError = when {
            state.amountInput.isBlank() -> "Enter the amount"
            amount == null -> "Enter a valid amount"
            !amount.isPositive -> "Amount must be greater than zero"
            else -> null
        }

        if (nameError != null || amountError != null) {
            _uiState.update { it.copy(nameError = nameError, amountError = amountError) }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            saveLoanRecordUseCase(
                existingId = editingRecordId,
                draft = BorrowLendDraft(
                    personName = state.personName,
                    direction = state.direction,
                    principalAmount = amount!!,
                    date = state.date,
                    dueDate = state.dueDate,
                    notes = state.notes.trim().takeIf { it.isNotEmpty() },
                    accountId = state.accountId
                )
            )
            _uiState.update { it.copy(isSaving = false, isFinished = true) }
        }
    }
}
