package com.aurex.app.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurex.app.core.Money
import com.aurex.app.core.getCurrentDay
import com.aurex.app.domain.model.Account
import com.aurex.app.domain.model.PaymentFrequency
import com.aurex.app.domain.model.PaymentMethod
import com.aurex.app.domain.model.RecurringTransactionDraft
import com.aurex.app.domain.model.TransactionCategories
import com.aurex.app.domain.model.TransactionCategory
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.domain.model.defaultPaymentMethodFor
import com.aurex.app.domain.usecase.account.GetAccountsUseCase
import com.aurex.app.domain.usecase.category.GetActiveCategoriesUseCase
import com.aurex.app.domain.usecase.recurring.GetRecurringTransactionUseCase
import com.aurex.app.domain.usecase.recurring.SaveRecurringTransactionUseCase
import com.aurex.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditRecurringUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val name: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val categoryId: String = "",
    val accountId: Long? = null,
    val transferAccountId: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val frequency: PaymentFrequency = PaymentFrequency.MONTHLY,
    /** yyyy-MM-dd */
    val startDate: String = "",
    val endDate: String? = null,
    val note: String = "",

    val accounts: List<Account> = emptyList(),
    val allCategories: List<TransactionCategory> = TransactionCategories.all,
    val currencySymbol: String = "$",

    val isSaving: Boolean = false,
    val isFinished: Boolean = false,
    val nameError: String? = null,
    val amountError: String? = null,
    val accountError: String? = null,
    val transferError: String? = null,
    val dateError: String? = null
) {
    val isTransfer: Boolean get() = type == TransactionType.TRANSFER

    val categories: List<TransactionCategory>
        get() = allCategories.filter { it.appliesTo == type }.ifEmpty {
            TransactionCategories.forType(type)
        }

    val canSelectTransferAccount: Boolean get() = isTransfer && accounts.size > 1

    val hasNoAccounts: Boolean get() = !isLoading && accounts.isEmpty()

    /** The cycles a schedule can run on: everything but "once". */
    val frequencies: List<PaymentFrequency>
        get() = PaymentFrequency.entries.filter { it != PaymentFrequency.ONE_TIME }

    fun methodFor(accountId: Long?): PaymentMethod =
        accounts.firstOrNull { it.id == accountId }
            ?.let { defaultPaymentMethodFor(it.type) }
            ?: paymentMethod
}

class AddEditRecurringViewModel(
    private val editingId: Long? = null,
    presetType: TransactionType = TransactionType.EXPENSE,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getActiveCategoriesUseCase: GetActiveCategoriesUseCase,
    private val getSelectedCurrencySymbolUseCase: GetSelectedCurrencySymbolUseCase,
    private val getRecurringTransactionUseCase: GetRecurringTransactionUseCase,
    private val saveRecurringTransactionUseCase: SaveRecurringTransactionUseCase,
    private val todayProvider: () -> String = ::getCurrentDay
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddEditRecurringUiState(
            type = presetType,
            startDate = todayProvider(),
            categoryId = TransactionCategories.forType(presetType).first().id
        )
    )
    val uiState: StateFlow<AddEditRecurringUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val accounts = getAccountsUseCase().first()
        val categories = getActiveCategoriesUseCase().first()
        val symbol = getSelectedCurrencySymbolUseCase().first() ?: "$"
        val existing = editingId?.let { getRecurringTransactionUseCase.once(it) }

        if (existing != null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isEditing = true,
                    name = existing.name,
                    type = existing.type,
                    amountInput = existing.amount.toMajorDouble().toString().removeSuffix(".0"),
                    categoryId = existing.categoryId,
                    accountId = existing.accountId,
                    transferAccountId = existing.transferAccountId,
                    paymentMethod = existing.paymentMethod,
                    frequency = existing.frequency,
                    startDate = existing.startDate,
                    endDate = existing.endDate,
                    note = existing.note.orEmpty(),
                    accounts = accounts,
                    allCategories = categories,
                    currencySymbol = symbol
                )
            }
            return
        }

        val defaultAccount = accounts.firstOrNull { !it.isArchived } ?: accounts.firstOrNull()
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                accounts = accounts,
                allCategories = categories,
                currencySymbol = symbol,
                accountId = defaultAccount?.id,
                paymentMethod = defaultAccount?.let { defaultPaymentMethodFor(it.type) }
                    ?: state.paymentMethod,
                categoryId = categories.firstOrNull { it.appliesTo == state.type }?.id
                    ?: state.categoryId
            )
        }
    }

    fun setName(value: String) = _uiState.update { it.copy(name = value, nameError = null) }

    fun setType(type: TransactionType) = _uiState.update { state ->
        if (state.type == type) return@update state
        state.copy(
            type = type,
            categoryId = state.allCategories.firstOrNull { it.appliesTo == type }?.id
                ?: TransactionCategories.forType(type).first().id,
            transferAccountId = null,
            transferError = null
        )
    }

    fun setAmount(value: String) =
        _uiState.update { it.copy(amountInput = numeric(value), amountError = null) }

    fun setCategory(id: String) = _uiState.update { it.copy(categoryId = id) }

    fun setAccount(id: Long) = _uiState.update {
        it.copy(
            accountId = id,
            paymentMethod = it.methodFor(id),
            accountError = null,
            transferAccountId = if (it.transferAccountId == id) null else it.transferAccountId
        )
    }

    fun setTransferAccount(id: Long) =
        _uiState.update { it.copy(transferAccountId = id, transferError = null) }

    fun setPaymentMethod(method: PaymentMethod) = _uiState.update { it.copy(paymentMethod = method) }

    fun setFrequency(frequency: PaymentFrequency) = _uiState.update { it.copy(frequency = frequency) }

    fun setStartDate(day: String) = _uiState.update { it.copy(startDate = day, dateError = null) }

    fun setEndDate(day: String?) = _uiState.update { it.copy(endDate = day, dateError = null) }

    fun setNote(value: String) = _uiState.update { it.copy(note = value) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val amount = Money.parse(state.amountInput)
        val nameError = if (state.name.isBlank()) "Give this a name, like Rent or Salary" else null
        val amountError = when {
            state.amountInput.isBlank() -> "Enter an amount"
            amount == null -> "Enter a valid amount"
            !amount.isPositive -> "Amount must be greater than zero"
            else -> null
        }
        val accountError = if (state.accountId == null) "Choose an account" else null
        val transferError = when {
            !state.isTransfer -> null
            state.transferAccountId == null -> "Choose an account to move it to"
            state.transferAccountId == state.accountId -> "Pick two different accounts"
            else -> null
        }
        val dateError = state.endDate?.let { end ->
            if (end < state.startDate) "The end date is before the start date" else null
        }

        if (listOf(nameError, amountError, accountError, transferError, dateError).any { it != null }) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    amountError = amountError,
                    accountError = accountError,
                    transferError = transferError,
                    dateError = dateError
                )
            }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = saveRecurringTransactionUseCase(
                existingId = editingId,
                draft = RecurringTransactionDraft(
                    name = state.name,
                    amount = amount!!,
                    type = state.type,
                    categoryId = if (state.isTransfer) TransactionCategories.transfer.id else state.categoryId,
                    accountId = state.accountId!!,
                    transferAccountId = state.transferAccountId.takeIf { state.isTransfer },
                    paymentMethod = state.paymentMethod,
                    frequency = state.frequency,
                    startDate = state.startDate,
                    endDate = state.endDate,
                    note = state.note.trim().takeIf { it.isNotEmpty() }
                )
            )
            when (result) {
                is SaveRecurringTransactionUseCase.Result.Saved ->
                    _uiState.update { it.copy(isSaving = false, isFinished = true) }

                is SaveRecurringTransactionUseCase.Result.Invalid ->
                    _uiState.update { it.copy(isSaving = false, nameError = result.message) }
            }
        }
    }

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
