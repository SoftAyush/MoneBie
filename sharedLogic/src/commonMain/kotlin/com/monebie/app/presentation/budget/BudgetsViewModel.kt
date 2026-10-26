package com.monebie.app.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monebie.app.core.Money
import com.monebie.app.core.AppCalendar
import com.monebie.app.core.getCurrentDay
import com.monebie.app.core.monthKeyOf
import com.monebie.app.core.monthOf
import com.monebie.app.core.shiftMonth
import com.monebie.app.domain.model.BudgetDraft
import com.monebie.app.domain.model.BudgetOverview
import com.monebie.app.domain.model.BudgetStatus
import com.monebie.app.domain.usecase.budget.DeleteBudgetUseCase
import com.monebie.app.domain.usecase.budget.GetBudgetOverviewUseCase
import com.monebie.app.domain.usecase.budget.SaveBudgetUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The add/edit sheet. */
data class BudgetFormState(
    val isVisible: Boolean = false,
    /** Null while creating. */
    val editingId: Long? = null,
    val categoryId: String = "",
    val amountInput: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
) {
    val isEditing: Boolean get() = editingId != null
}

sealed interface BudgetEvent {
    data class Saved(val categoryName: String) : BudgetEvent
    data class Deleted(val categoryName: String) : BudgetEvent
    data class Failed(val message: String) : BudgetEvent
}

/**
 * Budgets for one month at a time, paged like insights so the two screens
 * navigate the same way.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModel(
    getBudgetOverviewUseCase: GetBudgetOverviewUseCase,
    private val saveBudgetUseCase: SaveBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val todayProvider: () -> String = ::getCurrentDay
) : ViewModel() {

    private val currentMonth = monthOf(todayProvider())
    // Months back from the current one; a count so a calendar switch keeps the user's place.
    private val monthsBack = MutableStateFlow(0)
    private val selectedMonth = combine(AppCalendar.system, monthsBack) { system, back ->
        shiftMonth(monthKeyOf(todayProvider(), system), -back)
    }.distinctUntilChanged()

    val uiState: StateFlow<BudgetOverview> = selectedMonth
        .flatMapLatest { month -> getBudgetOverviewUseCase(month) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BudgetOverview(month = currentMonth)
        )

    private val _form = MutableStateFlow(BudgetFormState())
    val form: StateFlow<BudgetFormState> = _form.asStateFlow()

    private val _event = MutableStateFlow<BudgetEvent?>(null)
    val event: StateFlow<BudgetEvent?> = _event.asStateFlow()

    fun showPreviousMonth() {
        monthsBack.update { it + 1 }
    }

    fun showNextMonth() {
        monthsBack.update { (it - 1).coerceAtLeast(0) }
    }

    fun startCreating(categoryId: String? = null) {
        _form.value = BudgetFormState(
            isVisible = true,
            categoryId = categoryId ?: uiState.value.availableCategories.firstOrNull()?.id.orEmpty()
        )
    }

    fun startEditing(status: BudgetStatus) {
        _form.value = BudgetFormState(
            isVisible = true,
            editingId = status.budget.id,
            categoryId = status.budget.categoryId,
            amountInput = status.limit.toMajorDouble().toString().removeSuffix(".0")
        )
    }

    fun dismissForm() {
        _form.value = BudgetFormState()
    }

    fun setCategory(id: String) = _form.update { it.copy(categoryId = id, error = null) }

    fun setAmount(value: String) = _form.update { it.copy(amountInput = numeric(value), error = null) }

    fun save() {
        val current = _form.value
        if (current.isSaving) return

        val amount = Money.parse(current.amountInput)
        if (amount == null || !amount.isPositive) {
            _form.update { it.copy(error = "Enter a limit greater than zero") }
            return
        }
        if (current.categoryId.isBlank()) {
            _form.update { it.copy(error = "Pick a category") }
            return
        }

        _form.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (
                val result = saveBudgetUseCase(
                    existingId = current.editingId,
                    draft = BudgetDraft(categoryId = current.categoryId, amount = amount)
                )
            ) {
                is SaveBudgetUseCase.Result.Saved -> {
                    val name = uiState.value.statuses
                        .firstOrNull { it.budget.categoryId == current.categoryId }?.category?.name
                        ?: uiState.value.availableCategories
                            .firstOrNull { it.id == current.categoryId }?.name
                        ?: "Budget"
                    _event.value = BudgetEvent.Saved(name)
                    _form.value = BudgetFormState()
                }

                is SaveBudgetUseCase.Result.Invalid ->
                    _form.update { it.copy(isSaving = false, error = result.message) }
            }
        }
    }

    fun delete(status: BudgetStatus) {
        viewModelScope.launch {
            deleteBudgetUseCase(status.budget.id)
            _event.value = BudgetEvent.Deleted(status.category.name)
            if (_form.value.editingId == status.budget.id) _form.value = BudgetFormState()
        }
    }

    fun consumeEvent() {
        _event.value = null
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
