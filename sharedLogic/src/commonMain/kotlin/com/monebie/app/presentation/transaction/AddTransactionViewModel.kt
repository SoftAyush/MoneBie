package com.monebie.app.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monebie.app.core.Money
import com.monebie.app.core.dayOf
import com.monebie.app.core.getCurrentDay
import com.monebie.app.core.timestampForDay
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.PaymentMethod
import com.monebie.app.domain.model.defaultPaymentMethodFor
import com.monebie.app.domain.model.TransactionCategories
import com.monebie.app.domain.model.TransactionCategory
import com.monebie.app.domain.model.TransactionDraft
import com.monebie.app.domain.model.TransactionSource
import com.monebie.app.domain.model.TransactionType
import com.monebie.app.domain.usecase.account.GetAccountsUseCase
import com.monebie.app.domain.usecase.category.GetActiveCategoriesUseCase
import com.monebie.app.domain.usecase.transaction.DeleteTransactionUseCase
import com.monebie.app.domain.usecase.transaction.GetTransactionDefaultsUseCase
import com.monebie.app.domain.usecase.transaction.GetTransactionUseCase
import com.monebie.app.domain.usecase.transaction.SaveTransactionUseCase
import com.monebie.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddTransactionUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val categoryId: String = "",
    val accountId: Long? = null,
    val transferAccountId: Long? = null,
    /** yyyy-MM-dd */
    val day: String = "",
    val note: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,

    val accounts: List<Account> = emptyList(),
    /** Everything the user can file against, loaded once and filtered per type. */
    val allCategories: List<TransactionCategory> = TransactionCategories.all,
    val currencySymbol: String = "$",

    val isSaving: Boolean = false,
    val isFinished: Boolean = false,
    val amountError: String? = null,
    val accountError: String? = null,
    val transferError: String? = null
) {
    val isTransfer: Boolean get() = type == TransactionType.TRANSFER

    val categories: List<TransactionCategory>
        get() = allCategories.filter { it.appliesTo == type }.ifEmpty {
            // A user can hide every category on one side; the form still has to
            // offer something to file the entry under.
            TransactionCategories.forType(type)
        }

    /** A transfer needs a second account to move money into. */
    val canSelectTransferAccount: Boolean get() = isTransfer && accounts.size > 1

    val hasNoAccounts: Boolean get() = !isLoading && accounts.isEmpty()

    fun accountName(id: Long?): String =
        accounts.firstOrNull { it.id == id }?.name ?: "Select account"

    /**
     * How money leaves or arrives in an account, derived rather than asked.
     *
     * Falls back to whatever is already set when the account is unknown, so a
     * transaction being edited keeps the method it was recorded with.
     */
    fun methodFor(accountId: Long?): PaymentMethod =
        accounts.firstOrNull { it.id == accountId }
            ?.let { defaultPaymentMethodFor(it.type) }
            ?: paymentMethod
}

/**
 * Backs both adding and editing.
 *
 * Editing reuses the same form because the fields are identical; the only
 * difference is whether the save posts a new row or replaces one, and the
 * repository handles the balance consequences of either.
 */
class AddTransactionViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getActiveCategoriesUseCase: GetActiveCategoriesUseCase,
    private val getSelectedCurrencySymbolUseCase: GetSelectedCurrencySymbolUseCase,
    private val getTransactionUseCase: GetTransactionUseCase,
    private val getTransactionDefaultsUseCase: GetTransactionDefaultsUseCase,
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val editingTransactionId: Long? = null,
    presetType: TransactionType = TransactionType.EXPENSE,
    private val todayProvider: () -> String = ::getCurrentDay
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddTransactionUiState(
            type = presetType,
            day = todayProvider(),
            categoryId = TransactionCategories.forType(presetType).first().id
        )
    )
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val accounts = getAccountsUseCase().first()
        val categories = getActiveCategoriesUseCase().first()
        val symbol = getSelectedCurrencySymbolUseCase().first() ?: "$"
        val existing = editingTransactionId?.let { getTransactionUseCase(it) }

        if (existing != null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isEditing = true,
                    type = existing.type,
                    // Rendered as a plain major-unit string so the field reads
                    // back exactly what was stored, with no locale grouping.
                    amountInput = existing.amount.toMajorDouble().toString(),
                    categoryId = existing.categoryId,
                    accountId = existing.accountId,
                    transferAccountId = existing.transferAccountId,
                    day = dayOf(existing.date),
                    note = existing.note.orEmpty(),
                    paymentMethod = existing.paymentMethod,
                    accounts = accounts,
                    allCategories = categories,
                    currencySymbol = symbol
                )
            }
            return
        }

        val defaults = getTransactionDefaultsUseCase()
        // Fall back to the first account so a first-time entry still has one selected.
        val accountId = defaults.accountId?.takeIf { id -> accounts.any { it.id == id } }
            ?: accounts.firstOrNull()?.id

        _uiState.update { state ->
            state.copy(
                isLoading = false,
                accounts = accounts,
                allCategories = categories,
                currencySymbol = symbol,
                accountId = accountId,
                // Selected only once the list is known: which category comes
                // first on a side depends on what the user has kept and how
                // they have named it.
                categoryId = categories.firstOrNull { it.appliesTo == state.type }?.id
                    ?: state.categoryId,
                // Derived from whichever account was chosen above, not from the
                // last transaction: the two can disagree, and the account is
                // the one the user can see.
                paymentMethod = accounts.firstOrNull { it.id == accountId }
                    ?.let { account -> defaultPaymentMethodFor(account.type) }
                    ?: PaymentMethod.CASH
            )
        }
    }

    fun setType(type: TransactionType) {
        _uiState.update { state ->
            if (state.type == type) return@update state
            state.copy(
                type = type,
                // Categories do not overlap between types, so keeping the old id
                // would leave the form pointing at a category the user cannot see.
                categoryId = state.allCategories.firstOrNull { it.appliesTo == type }?.id
                    ?: TransactionCategories.forType(type).first().id,
                transferAccountId = if (type == TransactionType.TRANSFER) {
                    state.transferAccountId ?: state.accounts.firstOrNull { it.id != state.accountId }?.id
                } else {
                    null
                },
                transferError = null
            )
        }
    }

    fun setAmount(input: String) {
        // Accept only what can be part of a number, so the field cannot hold
        // text that would later fail to parse.
        val sanitised = input.filter { it.isDigit() || it == '.' }
        val singleDecimalPoint = sanitised.indexOf('.').let { first ->
            if (first == -1) sanitised
            else sanitised.substring(0, first + 1) + sanitised.substring(first + 1).replace(".", "")
        }
        _uiState.update { it.copy(amountInput = singleDecimalPoint, amountError = null) }
    }

    fun setCategory(categoryId: String) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun setAccount(accountId: Long) {
        _uiState.update { state ->
            state.copy(
                accountId = accountId,
                // The account decides how the money moved: cash out of a cash
                // account, a transfer out of a bank one. Asking the user again
                // only invites the two to disagree.
                paymentMethod = state.methodFor(accountId),
                // Never let both sides of a transfer be the same account.
                transferAccountId = state.transferAccountId?.takeIf { it != accountId },
                accountError = null,
                transferError = null
            )
        }
    }

    fun setTransferAccount(accountId: Long) {
        _uiState.update { it.copy(transferAccountId = accountId, transferError = null) }
    }

    fun setDay(day: String) {
        _uiState.update { it.copy(day = day) }
    }

    fun setNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val amount = Money.parse(state.amountInput)
        val accountId = state.accountId

        val amountError = when {
            state.amountInput.isBlank() -> "Enter an amount"
            amount == null -> "Enter a valid amount"
            !amount.isPositive -> "Amount must be greater than zero"
            else -> null
        }
        val accountError = if (accountId == null) "Choose an account" else null
        val transferError = when {
            !state.isTransfer -> null
            state.transferAccountId == null -> "Choose the account to transfer into"
            state.transferAccountId == accountId -> "Pick two different accounts"
            else -> null
        }

        if (amountError != null || accountError != null || transferError != null) {
            _uiState.update {
                it.copy(
                    amountError = amountError,
                    accountError = accountError,
                    transferError = transferError
                )
            }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            saveTransactionUseCase(
                existingId = editingTransactionId,
                draft = TransactionDraft(
                    amount = amount!!,
                    type = state.type,
                    categoryId = if (state.isTransfer) TransactionCategories.transfer.id else state.categoryId,
                    accountId = accountId!!,
                    date = timestampForDay(state.day),
                    note = state.note.trim().takeIf { it.isNotEmpty() },
                    paymentMethod = state.paymentMethod,
                    transferAccountId = state.transferAccountId,
                    source = TransactionSource.Manual
                )
            )
            _uiState.update { it.copy(isSaving = false, isFinished = true) }
        }
    }

    fun delete() {
        val id = editingTransactionId ?: return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            deleteTransactionUseCase(id)
            _uiState.update { it.copy(isSaving = false, isFinished = true) }
        }
    }
}
