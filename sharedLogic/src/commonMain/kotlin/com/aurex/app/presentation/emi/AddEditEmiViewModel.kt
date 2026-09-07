package com.aurex.app.presentation.emi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurex.app.core.Money
import com.aurex.app.core.MoneyFormatter
import com.aurex.app.core.addMonths
import com.aurex.app.core.formatDisplayDate
import com.aurex.app.core.getCurrentDay
import com.aurex.app.domain.calculation.EmiSchedule
import com.aurex.app.domain.model.Account
import com.aurex.app.domain.model.EmiDraft
import com.aurex.app.domain.usecase.account.GetAccountsUseCase
import com.aurex.app.domain.usecase.emi.GetEmiUseCase
import com.aurex.app.domain.usecase.emi.SaveEmiUseCase
import com.aurex.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditEmiUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val name: String = "",
    val provider: String = "",
    val principalInput: String = "",
    val interestRateInput: String = "",
    val tenureYearsInput: String = "",
    val monthlyAmountInput: String = "",
    val totalPayableInput: String = "",
    /** yyyy-MM-dd */
    val startDate: String = "",
    val paymentAccountId: Long? = null,
    val notes: String = "",

    val accounts: List<Account> = emptyList(),
    val currencySymbol: String = "$",

    val isSaving: Boolean = false,
    val isFinished: Boolean = false,
    val nameError: String? = null,
    val providerError: String? = null,
    val principalError: String? = null,
    val tenureError: String? = null,
    val monthlyError: String? = null,
    /** Set when installments are already paid and the schedule is being changed. */
    val scheduleWarning: String? = null
) {
    private val tenureYears: Double? get() = tenureYearsInput.toDoubleOrNull()

    val installmentCount: Int?
        get() = tenureYears?.takeIf { it > 0.0 }?.let { EmiSchedule.installmentCount(it) }

    /** Live preview of the derived end date, so the tenure entered is checkable. */
    val endDatePreview: String?
        get() {
            val count = installmentCount ?: return null
            if (startDate.isBlank()) return null
            return formatDisplayDate(addMonths(startDate, count))
        }

    /** Enabled only when there is enough to compute a suggestion from. */
    val canAutoCalculate: Boolean
        get() = Money.parse(principalInput)?.isPositive == true && (tenureYears ?: 0.0) > 0.0
}

/**
 * Create or edit an EMI.
 *
 * The monthly amount and total payable can be auto-calculated but are always
 * stored as entered: a lender's real schedule can differ from the textbook
 * formula, and the app must record the actual obligation.
 */
class AddEditEmiViewModel(
    private val editingEmiId: Long? = null,
    private val getEmiUseCase: GetEmiUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getSelectedCurrencySymbolUseCase: GetSelectedCurrencySymbolUseCase,
    private val saveEmiUseCase: SaveEmiUseCase,
    private val todayProvider: () -> String = ::getCurrentDay
) : ViewModel() {

    /** Held so an edit can warn before changing a schedule that is already under way. */
    private var installmentsAlreadyPaid: Int = 0
    private var amountAlreadyPaid: Money = Money.ZERO

    private val _uiState = MutableStateFlow(AddEditEmiUiState(startDate = todayProvider()))
    val uiState: StateFlow<AddEditEmiUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val accounts = getAccountsUseCase().first()
        val symbol = getSelectedCurrencySymbolUseCase().first() ?: "$"
        val existing = editingEmiId?.let { getEmiUseCase.once(it) }

        if (existing != null) {
            installmentsAlreadyPaid = existing.installmentsPaid
            amountAlreadyPaid = existing.amountPaid
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isEditing = true,
                    name = existing.name,
                    provider = existing.provider,
                    principalInput = existing.principalAmount.toMajorDouble().toString(),
                    interestRateInput = existing.interestRate?.toString().orEmpty(),
                    tenureYearsInput = existing.tenureYears.toString(),
                    monthlyAmountInput = existing.monthlyAmount.toMajorDouble().toString(),
                    totalPayableInput = existing.totalPayableAmount.toMajorDouble().toString(),
                    startDate = existing.startDate,
                    paymentAccountId = existing.paymentAccountId,
                    notes = existing.notes.orEmpty(),
                    accounts = accounts,
                    currencySymbol = symbol,
                    scheduleWarning = if (existing.installmentsPaid > 0) {
                        "${existing.installmentsPaid} installments are already recorded. " +
                            "Changing the start date or tenure will move the remaining schedule."
                    } else {
                        null
                    }
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                accounts = accounts,
                paymentAccountId = accounts.firstOrNull()?.id,
                currencySymbol = symbol
            )
        }
    }

    fun setName(value: String) = _uiState.update { it.copy(name = value, nameError = null) }

    fun setProvider(value: String) =
        _uiState.update { it.copy(provider = value, providerError = null) }

    fun setPrincipal(value: String) =
        _uiState.update { it.copy(principalInput = numeric(value), principalError = null) }

    fun setInterestRate(value: String) =
        _uiState.update { it.copy(interestRateInput = numeric(value)) }

    fun setTenureYears(value: String) =
        _uiState.update { it.copy(tenureYearsInput = numeric(value), tenureError = null) }

    fun setMonthlyAmount(value: String) =
        _uiState.update { it.copy(monthlyAmountInput = numeric(value), monthlyError = null) }

    fun setTotalPayable(value: String) =
        _uiState.update { it.copy(totalPayableInput = numeric(value)) }

    fun setStartDate(day: String) = _uiState.update { it.copy(startDate = day) }

    fun setPaymentAccount(accountId: Long?) =
        _uiState.update { it.copy(paymentAccountId = accountId) }

    fun setNotes(value: String) = _uiState.update { it.copy(notes = value) }

    /** Fills the monthly amount and total payable from principal, rate and tenure. */
    fun autoCalculate() {
        val state = _uiState.value
        val suggestion = EmiSchedule.suggest(
            principal = Money.parse(state.principalInput),
            annualInterestRate = state.interestRateInput.toDoubleOrNull(),
            tenureYears = state.tenureYearsInput.toDoubleOrNull()
        ) ?: return

        _uiState.update {
            it.copy(
                monthlyAmountInput = suggestion.monthlyAmount.toMajorDouble().toString(),
                totalPayableInput = suggestion.totalPayable.toMajorDouble().toString(),
                monthlyError = null
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val principal = Money.parse(state.principalInput)
        val monthly = Money.parse(state.monthlyAmountInput)
        val tenure = state.tenureYearsInput.toDoubleOrNull()

        val nameError = if (state.name.isBlank()) "Give this EMI a name" else null
        val providerError = if (state.provider.isBlank()) "Who is it with?" else null
        val principalError = when {
            state.principalInput.isBlank() -> "Enter the amount financed"
            principal == null || !principal.isPositive -> "Enter a valid amount"
            else -> null
        }
        val tenureError = when {
            state.tenureYearsInput.isBlank() -> "Enter the tenure in years"
            tenure == null || tenure <= 0.0 -> "Enter a valid tenure"
            // Shortening a tenure below what has already been paid would leave
            // the schedule claiming fewer installments than exist in history.
            installmentsAlreadyPaid > EmiSchedule.installmentCount(tenure) ->
                "$installmentsAlreadyPaid installments are already paid, so the tenure cannot be this short"
            else -> null
        }
        val monthlyError = when {
            state.monthlyAmountInput.isBlank() -> "Enter the monthly amount"
            monthly == null || !monthly.isPositive -> "Enter a valid amount"
            else -> null
        }

        if (nameError != null || providerError != null || principalError != null ||
            tenureError != null || monthlyError != null
        ) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    providerError = providerError,
                    principalError = principalError,
                    tenureError = tenureError,
                    monthlyError = monthlyError
                )
            }
            return
        }

        // Falls back to monthly across the full tenure when the user left the
        // total blank, which is the correct default for a level schedule.
        val totalPayable = Money.parse(state.totalPayableInput)?.takeIf { it.isPositive }
            ?: EmiSchedule.totalPayable(monthly!!, tenure!!)

        if (totalPayable < amountAlreadyPaid) {
            _uiState.update {
                it.copy(
                    monthlyError = "Already paid " +
                        MoneyFormatter.format(amountAlreadyPaid, state.currencySymbol) +
                        " - the total payable cannot be lower"
                )
            }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            saveEmiUseCase(
                existingId = editingEmiId,
                draft = EmiDraft(
                    name = state.name,
                    provider = state.provider,
                    principalAmount = principal!!,
                    totalPayableAmount = totalPayable,
                    monthlyAmount = monthly!!,
                    interestRate = state.interestRateInput.toDoubleOrNull(),
                    startDate = state.startDate,
                    tenureYears = tenure!!,
                    paymentAccountId = state.paymentAccountId,
                    notes = state.notes.trim().takeIf { it.isNotEmpty() }
                )
            )
            _uiState.update { it.copy(isSaving = false, isFinished = true) }
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
