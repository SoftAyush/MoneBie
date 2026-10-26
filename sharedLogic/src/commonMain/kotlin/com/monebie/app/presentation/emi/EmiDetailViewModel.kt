package com.monebie.app.presentation.emi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monebie.app.core.Money
import com.monebie.app.core.daysBetween
import com.monebie.app.core.getCurrentDay
import com.monebie.app.domain.calculation.FinancialCalculator
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.Emi
import com.monebie.app.domain.model.EmiPayment
import com.monebie.app.domain.model.EmiStatus
import com.monebie.app.domain.model.PaymentMethod
import com.monebie.app.domain.model.defaultPaymentMethodFor
import com.monebie.app.domain.usecase.account.GetAccountsUseCase
import com.monebie.app.domain.usecase.emi.DeleteEmiUseCase
import com.monebie.app.domain.usecase.emi.GetEmiPaymentsUseCase
import com.monebie.app.domain.usecase.emi.GetEmiUseCase
import com.monebie.app.domain.usecase.emi.MarkInstallmentPaidUseCase
import com.monebie.app.domain.usecase.emi.UndoLastInstallmentUseCase
import com.monebie.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmiDetailUiState(
    val isLoading: Boolean = true,
    val emi: Emi? = null,
    val status: EmiStatus = EmiStatus.ACTIVE,
    val daysUntilDue: Long? = null,
    val payments: List<EmiPayment> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val currencySymbol: String = "$",
    val isDeleted: Boolean = false
) {
    fun accountName(id: Long?): String =
        accounts.firstOrNull { it.id == id }?.name ?: "Not linked to an account"

    /** Only the newest installment can be undone; see EmiRepository. */
    val undoablePaymentId: Long? get() = payments.firstOrNull()?.id
}

/** Form state for the mark-as-paid sheet. */
data class MarkPaidUiState(
    val isVisible: Boolean = false,
    val amountInput: String = "",
    val paidDay: String = "",
    val accountId: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.BANK_TRANSFER,
    val installmentNumber: Int = 0,
    val totalInstallments: Int = 0,
    val dueDate: String? = null,
    val isSaving: Boolean = false,
    val amountError: String? = null
)

class EmiDetailViewModel(
    private val emiId: Long,
    getEmiUseCase: GetEmiUseCase,
    getEmiPaymentsUseCase: GetEmiPaymentsUseCase,
    getAccountsUseCase: GetAccountsUseCase,
    getSelectedCurrencySymbolUseCase: GetSelectedCurrencySymbolUseCase,
    private val markInstallmentPaidUseCase: MarkInstallmentPaidUseCase,
    private val undoLastInstallmentUseCase: UndoLastInstallmentUseCase,
    private val deleteEmiUseCase: DeleteEmiUseCase,
    private val todayProvider: () -> String = ::getCurrentDay
) : ViewModel() {

    private val deleted = MutableStateFlow(false)

    val uiState: StateFlow<EmiDetailUiState> = combine(
        getEmiUseCase(emiId),
        getEmiPaymentsUseCase(emiId),
        getAccountsUseCase(),
        getSelectedCurrencySymbolUseCase(),
        deleted
    ) { emi, payments, accounts, symbol, isDeleted ->
        val today = todayProvider()
        EmiDetailUiState(
            isLoading = false,
            emi = emi,
            status = emi?.let { FinancialCalculator.emiStatus(it, today) } ?: EmiStatus.ACTIVE,
            daysUntilDue = emi?.nextDueDate?.let { daysBetween(today, it) },
            payments = payments,
            accounts = accounts,
            currencySymbol = symbol ?: "$",
            isDeleted = isDeleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EmiDetailUiState()
    )

    private val _markPaidForm = MutableStateFlow(MarkPaidUiState())
    val markPaidForm: StateFlow<MarkPaidUiState> = _markPaidForm.asStateFlow()

    /**
     * Opens the sheet already filled from the schedule: the next installment's
     * amount, today as the paid date, and the EMI's usual payment account.
     */
    fun openMarkPaidSheet() {
        val state = uiState.value
        val emi = state.emi ?: return
        val installmentNumber = emi.nextInstallmentNumber ?: return

        val accountId = emi.paymentAccountId?.takeIf { id -> state.accounts.any { it.id == id } }
            ?: state.accounts.firstOrNull()?.id
        val accountType = state.accounts.firstOrNull { it.id == accountId }?.type

        _markPaidForm.value = MarkPaidUiState(
            isVisible = true,
            amountInput = emi.nextInstallmentAmount.toMajorDouble().toString(),
            paidDay = todayProvider(),
            accountId = accountId,
            paymentMethod = accountType?.let { defaultPaymentMethodFor(it) }
                ?: PaymentMethod.BANK_TRANSFER,
            installmentNumber = installmentNumber,
            totalInstallments = emi.totalInstallments,
            dueDate = emi.nextDueDate
        )
    }

    fun dismissMarkPaidSheet() {
        _markPaidForm.update { it.copy(isVisible = false) }
    }

    fun setAmount(input: String) {
        val sanitised = input.filter { it.isDigit() || it == '.' }
        val singleDecimalPoint = sanitised.indexOf('.').let { first ->
            if (first == -1) sanitised
            else sanitised.substring(0, first + 1) + sanitised.substring(first + 1).replace(".", "")
        }
        _markPaidForm.update { it.copy(amountInput = singleDecimalPoint, amountError = null) }
    }

    fun setPaidDay(day: String) {
        _markPaidForm.update { it.copy(paidDay = day) }
    }

    fun setAccount(accountId: Long?) {
        val accountType = uiState.value.accounts.firstOrNull { it.id == accountId }?.type
        _markPaidForm.update {
            it.copy(
                accountId = accountId,
                paymentMethod = accountType?.let { type -> defaultPaymentMethodFor(type) }
                    ?: it.paymentMethod
            )
        }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _markPaidForm.update { it.copy(paymentMethod = method) }
    }

    fun markPaid() {
        val form = _markPaidForm.value
        if (form.isSaving) return

        val emi = uiState.value.emi ?: return
        val amount = Money.parse(form.amountInput)

        val error = when {
            form.amountInput.isBlank() -> "Enter an amount"
            amount == null -> "Enter a valid amount"
            !amount.isPositive -> "Amount must be greater than zero"
            // Paying more than the whole EMI still owes would push the remaining
            // balance negative while the schedule still shows installments left.
            amount > emi.remainingAmount ->
                "That is more than the " + emi.remainingAmount.toMajorDouble() + " still payable"
            else -> null
        }

        if (error != null) {
            _markPaidForm.update { it.copy(amountError = error) }
            return
        }

        _markPaidForm.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            markInstallmentPaidUseCase(
                emiId = emiId,
                amount = amount!!,
                paidDay = form.paidDay,
                accountId = form.accountId,
                paymentMethod = form.paymentMethod
            )
            _markPaidForm.value = MarkPaidUiState()
        }
    }

    fun undoLastInstallment() {
        viewModelScope.launch { undoLastInstallmentUseCase(emiId) }
    }

    fun deleteEmi() {
        viewModelScope.launch {
            deleteEmiUseCase(emiId)
            deleted.value = true
        }
    }
}
