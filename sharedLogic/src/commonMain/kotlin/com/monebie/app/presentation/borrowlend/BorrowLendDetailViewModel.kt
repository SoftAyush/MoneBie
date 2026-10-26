package com.monebie.app.presentation.borrowlend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monebie.app.core.Money
import com.monebie.app.core.daysBetween
import com.monebie.app.core.getCurrentDay
import com.monebie.app.domain.calculation.FinancialCalculator
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.BorrowLendRecord
import com.monebie.app.domain.model.LoanDirection
import com.monebie.app.domain.model.LoanRepayment
import com.monebie.app.domain.model.LoanStatus
import com.monebie.app.domain.model.PaymentMethod
import com.monebie.app.domain.model.defaultPaymentMethodFor
import com.monebie.app.domain.usecase.account.GetAccountsUseCase
import com.monebie.app.domain.usecase.borrowlend.DeleteLoanRecordUseCase
import com.monebie.app.domain.usecase.borrowlend.DeleteLoanRepaymentUseCase
import com.monebie.app.domain.usecase.borrowlend.GetLoanRecordUseCase
import com.monebie.app.domain.usecase.borrowlend.GetLoanRepaymentsUseCase
import com.monebie.app.domain.usecase.borrowlend.RecordLoanRepaymentUseCase
import com.monebie.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoanDetailUiState(
    val isLoading: Boolean = true,
    val record: BorrowLendRecord? = null,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val daysUntilDue: Long? = null,
    val repayments: List<LoanRepayment> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val currencySymbol: String = "$",
    val isDeleted: Boolean = false
) {
    val isBorrowed: Boolean get() = record?.direction == LoanDirection.BORROWED

    /** "Repay" when the user owes, "Record repayment" when they are owed. */
    val actionLabel: String get() = if (isBorrowed) "Record a repayment" else "Record money received"

    fun accountName(id: Long?): String =
        accounts.firstOrNull { it.id == id }?.name ?: "Not linked to an account"
}

/** Form state for the repayment sheet, in whichever direction settles the record. */
data class LoanRepaymentUiState(
    val isVisible: Boolean = false,
    val amountInput: String = "",
    val day: String = "",
    val accountId: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val note: String = "",
    val isSaving: Boolean = false,
    val amountError: String? = null
)

class BorrowLendDetailViewModel(
    private val recordId: Long,
    getLoanRecordUseCase: GetLoanRecordUseCase,
    getLoanRepaymentsUseCase: GetLoanRepaymentsUseCase,
    getAccountsUseCase: GetAccountsUseCase,
    getSelectedCurrencySymbolUseCase: GetSelectedCurrencySymbolUseCase,
    private val recordLoanRepaymentUseCase: RecordLoanRepaymentUseCase,
    private val deleteLoanRepaymentUseCase: DeleteLoanRepaymentUseCase,
    private val deleteLoanRecordUseCase: DeleteLoanRecordUseCase,
    private val todayProvider: () -> String = ::getCurrentDay
) : ViewModel() {

    private val deleted = MutableStateFlow(false)

    val uiState: StateFlow<LoanDetailUiState> = combine(
        getLoanRecordUseCase(recordId),
        getLoanRepaymentsUseCase(recordId),
        getAccountsUseCase(),
        getSelectedCurrencySymbolUseCase(),
        deleted
    ) { record, repayments, accounts, symbol, isDeleted ->
        val today = todayProvider()
        LoanDetailUiState(
            isLoading = false,
            record = record,
            status = record?.let { FinancialCalculator.loanStatus(it, today) } ?: LoanStatus.ACTIVE,
            daysUntilDue = record?.dueDate?.let { daysBetween(today, it) },
            repayments = repayments,
            accounts = accounts,
            currencySymbol = symbol ?: "$",
            isDeleted = isDeleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LoanDetailUiState()
    )

    private val _repaymentForm = MutableStateFlow(LoanRepaymentUiState())
    val repaymentForm: StateFlow<LoanRepaymentUiState> = _repaymentForm.asStateFlow()

    /**
     * Opens the sheet with the full outstanding amount filled in.
     *
     * Person-to-person loans are usually settled in one go, unlike a scheduled
     * debt, so the whole remainder is the better default.
     */
    fun openRepaymentSheet() {
        val state = uiState.value
        val record = state.record ?: return

        val accountId = record.accountId?.takeIf { id -> state.accounts.any { it.id == id } }
            ?: state.accounts.firstOrNull()?.id
        val accountType = state.accounts.firstOrNull { it.id == accountId }?.type

        _repaymentForm.value = LoanRepaymentUiState(
            isVisible = true,
            amountInput = record.remainingAmount.toMajorDouble().toString(),
            day = todayProvider(),
            accountId = accountId,
            paymentMethod = accountType?.let { defaultPaymentMethodFor(it) } ?: PaymentMethod.CASH
        )
    }

    fun dismissRepaymentSheet() {
        _repaymentForm.update { it.copy(isVisible = false) }
    }

    fun setAmount(input: String) {
        val sanitised = input.filter { it.isDigit() || it == '.' }
        val singleDecimalPoint = sanitised.indexOf('.').let { first ->
            if (first == -1) sanitised
            else sanitised.substring(0, first + 1) + sanitised.substring(first + 1).replace(".", "")
        }
        _repaymentForm.update { it.copy(amountInput = singleDecimalPoint, amountError = null) }
    }

    fun setDay(day: String) {
        _repaymentForm.update { it.copy(day = day) }
    }

    fun setAccount(accountId: Long?) {
        val accountType = uiState.value.accounts.firstOrNull { it.id == accountId }?.type
        _repaymentForm.update {
            it.copy(
                accountId = accountId,
                paymentMethod = accountType?.let { type -> defaultPaymentMethodFor(type) }
                    ?: it.paymentMethod
            )
        }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _repaymentForm.update { it.copy(paymentMethod = method) }
    }

    fun setNote(note: String) {
        _repaymentForm.update { it.copy(note = note) }
    }

    fun recordRepayment() {
        val form = _repaymentForm.value
        if (form.isSaving) return

        val record = uiState.value.record ?: return
        val amount = Money.parse(form.amountInput)

        val error = when {
            form.amountInput.isBlank() -> "Enter an amount"
            amount == null -> "Enter a valid amount"
            !amount.isPositive -> "Amount must be greater than zero"
            // Settling more than is outstanding would mark the record complete
            // while the surplus disappeared from every total.
            amount > record.remainingAmount ->
                "That is more than the " + record.remainingAmount.toMajorDouble() + " outstanding"
            else -> null
        }

        if (error != null) {
            _repaymentForm.update { it.copy(amountError = error) }
            return
        }

        _repaymentForm.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            recordLoanRepaymentUseCase(
                recordId = recordId,
                amount = amount!!,
                day = form.day,
                accountId = form.accountId,
                paymentMethod = form.paymentMethod,
                note = form.note
            )
            _repaymentForm.value = LoanRepaymentUiState()
        }
    }

    fun deleteRepayment(repaymentId: Long) {
        viewModelScope.launch { deleteLoanRepaymentUseCase(repaymentId) }
    }

    fun deleteRecord() {
        viewModelScope.launch {
            deleteLoanRecordUseCase(recordId)
            deleted.value = true
        }
    }
}
