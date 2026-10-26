package com.monebie.app.presentation.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monebie.app.core.Money
import com.monebie.app.core.getCurrentDay
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.DebtDraft
import com.monebie.app.domain.model.PaymentFrequency
import com.monebie.app.domain.usecase.account.GetAccountsUseCase
import com.monebie.app.domain.usecase.debt.GetDebtUseCase
import com.monebie.app.domain.usecase.debt.SaveDebtUseCase
import com.monebie.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditDebtUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val name: String = "",
    val lender: String = "",
    val originalAmountInput: String = "",
    val interestRateInput: String = "",
    /** yyyy-MM-dd */
    val startDate: String = "",
    val dueDate: String? = null,
    val paymentFrequency: PaymentFrequency = PaymentFrequency.MONTHLY,
    val installmentAmountInput: String = "",
    val notes: String = "",
    val defaultAccountId: Long? = null,

    val accounts: List<Account> = emptyList(),
    val currencySymbol: String = "$",

    val isSaving: Boolean = false,
    val isFinished: Boolean = false,
    val nameError: String? = null,
    val lenderError: String? = null,
    val amountError: String? = null,
    /** Set when editing would drop below what has already been repaid. */
    val amountWarning: String? = null
) {
    val showsInstallmentField: Boolean get() = paymentFrequency != PaymentFrequency.ONE_TIME
}

class AddEditDebtViewModel(
    private val editingDebtId: Long? = null,
    private val getDebtUseCase: GetDebtUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getSelectedCurrencySymbolUseCase: GetSelectedCurrencySymbolUseCase,
    private val saveDebtUseCase: SaveDebtUseCase,
    private val todayProvider: () -> String = ::getCurrentDay
) : ViewModel() {

    /** Held so an edit can be validated against what has already been repaid. */
    private var alreadyPaid: Money = Money.ZERO

    private val _uiState = MutableStateFlow(AddEditDebtUiState(startDate = todayProvider()))
    val uiState: StateFlow<AddEditDebtUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val accounts = getAccountsUseCase().first()
        val symbol = getSelectedCurrencySymbolUseCase().first() ?: "$"
        val existing = editingDebtId?.let { getDebtUseCase.once(it) }

        if (existing != null) {
            alreadyPaid = existing.amountPaid
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isEditing = true,
                    name = existing.name,
                    lender = existing.lender,
                    originalAmountInput = existing.originalAmount.toMajorDouble().toString(),
                    interestRateInput = existing.interestRate?.toString().orEmpty(),
                    startDate = existing.startDate,
                    dueDate = existing.dueDate,
                    paymentFrequency = existing.paymentFrequency,
                    installmentAmountInput = existing.installmentAmount
                        ?.toMajorDouble()?.toString().orEmpty(),
                    notes = existing.notes.orEmpty(),
                    defaultAccountId = existing.defaultAccountId,
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
                defaultAccountId = accounts.firstOrNull()?.id,
                currencySymbol = symbol
            )
        }
    }

    fun setName(value: String) = _uiState.update { it.copy(name = value, nameError = null) }

    fun setLender(value: String) = _uiState.update { it.copy(lender = value, lenderError = null) }

    fun setOriginalAmount(value: String) =
        _uiState.update { it.copy(originalAmountInput = numeric(value), amountError = null) }

    fun setInterestRate(value: String) =
        _uiState.update { it.copy(interestRateInput = numeric(value)) }

    fun setStartDate(day: String) = _uiState.update { it.copy(startDate = day) }

    fun setDueDate(day: String?) = _uiState.update { it.copy(dueDate = day) }

    fun setPaymentFrequency(frequency: PaymentFrequency) =
        _uiState.update { it.copy(paymentFrequency = frequency) }

    fun setInstallmentAmount(value: String) =
        _uiState.update { it.copy(installmentAmountInput = numeric(value)) }

    fun setNotes(value: String) = _uiState.update { it.copy(notes = value) }

    fun setDefaultAccount(accountId: Long?) =
        _uiState.update { it.copy(defaultAccountId = accountId) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val amount = Money.parse(state.originalAmountInput)

        val nameError = if (state.name.isBlank()) "Give this debt a name" else null
        val lenderError = if (state.lender.isBlank()) "Who is it owed to?" else null
        val amountError = when {
            state.originalAmountInput.isBlank() -> "Enter the amount borrowed"
            amount == null -> "Enter a valid amount"
            !amount.isPositive -> "Amount must be greater than zero"
            // The remaining balance is original minus paid, so an original
            // below what is already repaid would show a negative debt.
            amount < alreadyPaid ->
                "Already repaid " + alreadyPaid.toMajorDouble() + " - the total cannot be lower"
            else -> null
        }

        if (nameError != null || lenderError != null || amountError != null) {
            _uiState.update {
                it.copy(nameError = nameError, lenderError = lenderError, amountError = amountError)
            }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            saveDebtUseCase(
                existingId = editingDebtId,
                draft = DebtDraft(
                    name = state.name,
                    lender = state.lender,
                    originalAmount = amount!!,
                    interestRate = state.interestRateInput.toDoubleOrNull(),
                    startDate = state.startDate,
                    dueDate = state.dueDate,
                    paymentFrequency = state.paymentFrequency,
                    installmentAmount = if (state.showsInstallmentField) {
                        Money.parse(state.installmentAmountInput)?.takeIf { it.isPositive }
                    } else {
                        null
                    },
                    notes = state.notes.trim().takeIf { it.isNotEmpty() },
                    defaultAccountId = state.defaultAccountId
                )
            )
            _uiState.update { it.copy(isSaving = false, isFinished = true) }
        }
    }

    /** Keeps the field free of anything that could not later parse as a number. */
    private fun numeric(input: String): String {
        val sanitised = input.filter { it.isDigit() || it == '.' }
        val first = sanitised.indexOf('.')
        return if (first == -1) {
            sanitised
        } else {
            sanitised.substring(0, first + 1) + sanitised.substring(first + 1).replace(".", "")
        }
    }
}
