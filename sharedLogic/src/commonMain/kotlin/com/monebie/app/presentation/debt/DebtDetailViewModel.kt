package com.monebie.app.presentation.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monebie.app.core.Money
import com.monebie.app.core.getCurrentDay
import com.monebie.app.domain.calculation.FinancialCalculator
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.Debt
import com.monebie.app.domain.model.DebtPayment
import com.monebie.app.domain.model.DebtStatus
import com.monebie.app.domain.model.PaymentMethod
import com.monebie.app.domain.model.defaultPaymentMethodFor
import com.monebie.app.domain.usecase.account.GetAccountsUseCase
import com.monebie.app.domain.usecase.debt.DeleteDebtPaymentUseCase
import com.monebie.app.domain.usecase.debt.DeleteDebtUseCase
import com.monebie.app.domain.usecase.debt.GetDebtPaymentsUseCase
import com.monebie.app.domain.usecase.debt.GetDebtUseCase
import com.monebie.app.domain.usecase.debt.RecordDebtPaymentUseCase
import com.monebie.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DebtDetailUiState(
    val isLoading: Boolean = true,
    val debt: Debt? = null,
    val status: DebtStatus = DebtStatus.ACTIVE,
    val nextDueDate: String? = null,
    val daysUntilDue: Long? = null,
    val payments: List<DebtPayment> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val currencySymbol: String = "$",
    /** True once the debt is gone, so the screen can navigate away. */
    val isDeleted: Boolean = false
) {
    fun accountName(id: Long?): String =
        accounts.firstOrNull { it.id == id }?.name ?: "Not linked to an account"
}

/** Form state for the record-payment sheet. */
data class RecordPaymentUiState(
    val isVisible: Boolean = false,
    val amountInput: String = "",
    val day: String = "",
    val accountId: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val note: String = "",
    val isSaving: Boolean = false,
    val amountError: String? = null
)

class DebtDetailViewModel(
    private val debtId: Long,
    getDebtUseCase: GetDebtUseCase,
    getDebtPaymentsUseCase: GetDebtPaymentsUseCase,
    getAccountsUseCase: GetAccountsUseCase,
    getSelectedCurrencySymbolUseCase: GetSelectedCurrencySymbolUseCase,
    private val recordDebtPaymentUseCase: RecordDebtPaymentUseCase,
    private val deleteDebtPaymentUseCase: DeleteDebtPaymentUseCase,
    private val deleteDebtUseCase: DeleteDebtUseCase,
    private val todayProvider: () -> String = ::getCurrentDay
) : ViewModel() {

    private val deleted = MutableStateFlow(false)

    val uiState: StateFlow<DebtDetailUiState> = combine(
        getDebtUseCase(debtId),
        getDebtPaymentsUseCase(debtId),
        getAccountsUseCase(),
        getSelectedCurrencySymbolUseCase(),
        deleted
    ) { debt, payments, accounts, symbol, isDeleted ->
        val today = todayProvider()
        val nextDue = debt?.let { FinancialCalculator.nextDebtDueDate(it, today) }

        DebtDetailUiState(
            isLoading = false,
            debt = debt,
            status = debt?.let { FinancialCalculator.debtStatus(it, today) } ?: DebtStatus.ACTIVE,
            nextDueDate = nextDue,
            daysUntilDue = nextDue?.let { com.monebie.app.core.daysBetween(today, it) },
            payments = payments,
            accounts = accounts,
            currencySymbol = symbol ?: "$",
            isDeleted = isDeleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebtDetailUiState()
    )

    private val _paymentForm = MutableStateFlow(RecordPaymentUiState())
    val paymentForm: StateFlow<RecordPaymentUiState> = _paymentForm.asStateFlow()

    /**
     * Opens the payment sheet pre-filled with what the user most likely wants:
     * the scheduled installment (capped at what is actually left), today's date,
     * and the account this debt is normally paid from.
     */
    fun openPaymentSheet() {
        val state = uiState.value
        val debt = state.debt ?: return
        val suggested = debt.nextPaymentAmount
        val accountId = debt.defaultAccountId?.takeIf { id -> state.accounts.any { it.id == id } }
            ?: state.accounts.firstOrNull()?.id
        val accountType = state.accounts.firstOrNull { it.id == accountId }?.type

        _paymentForm.value = RecordPaymentUiState(
            isVisible = true,
            amountInput = if (suggested.isPositive) suggested.toMajorDouble().toString() else "",
            day = todayProvider(),
            accountId = accountId,
            paymentMethod = accountType?.let { defaultPaymentMethodFor(it) } ?: PaymentMethod.CASH
        )
    }

    fun dismissPaymentSheet() {
        _paymentForm.update { it.copy(isVisible = false) }
    }

    fun setPaymentAmount(input: String) {
        val sanitised = input.filter { it.isDigit() || it == '.' }
        val singleDecimalPoint = sanitised.indexOf('.').let { first ->
            if (first == -1) sanitised
            else sanitised.substring(0, first + 1) + sanitised.substring(first + 1).replace(".", "")
        }
        _paymentForm.update { it.copy(amountInput = singleDecimalPoint, amountError = null) }
    }

    fun setPaymentDay(day: String) {
        _paymentForm.update { it.copy(day = day) }
    }

    fun setPaymentAccount(accountId: Long?) {
        val accountType = uiState.value.accounts.firstOrNull { it.id == accountId }?.type
        _paymentForm.update {
            it.copy(
                accountId = accountId,
                paymentMethod = accountType?.let { type -> defaultPaymentMethodFor(type) }
                    ?: it.paymentMethod
            )
        }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _paymentForm.update { it.copy(paymentMethod = method) }
    }

    fun setPaymentNote(note: String) {
        _paymentForm.update { it.copy(note = note) }
    }

    fun recordPayment() {
        val form = _paymentForm.value
        if (form.isSaving) return

        val debt = uiState.value.debt ?: return
        val amount = Money.parse(form.amountInput)

        val error = when {
            form.amountInput.isBlank() -> "Enter an amount"
            amount == null -> "Enter a valid amount"
            !amount.isPositive -> "Amount must be greater than zero"
            // Overpaying is almost always a typo, and letting it through would
            // make the debt look settled while the extra money silently vanishes.
            amount > debt.remainingAmount ->
                "That is more than the " + debt.remainingAmount.toMajorDouble() + " still owed"
            else -> null
        }

        if (error != null) {
            _paymentForm.update { it.copy(amountError = error) }
            return
        }

        _paymentForm.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            recordDebtPaymentUseCase(
                debtId = debtId,
                amount = amount!!,
                day = form.day,
                accountId = form.accountId,
                paymentMethod = form.paymentMethod,
                note = form.note
            )
            _paymentForm.value = RecordPaymentUiState()
        }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch { deleteDebtPaymentUseCase(paymentId) }
    }

    fun deleteDebt() {
        viewModelScope.launch {
            deleteDebtUseCase(debtId)
            deleted.value = true
        }
    }
}
