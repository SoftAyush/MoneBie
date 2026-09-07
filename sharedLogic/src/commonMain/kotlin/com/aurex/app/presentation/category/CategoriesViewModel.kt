package com.aurex.app.presentation.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurex.app.domain.model.TransactionCategory
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.domain.repository.CategoryRemoval
import com.aurex.app.domain.usecase.category.CountCategoryUsageUseCase
import com.aurex.app.domain.usecase.category.GetCategoriesUseCase
import com.aurex.app.domain.usecase.category.RemoveCategoryUseCase
import com.aurex.app.domain.usecase.category.RestoreCategoryUseCase
import com.aurex.app.domain.usecase.category.SaveCategoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val isLoading: Boolean = true,
    val categories: List<TransactionCategory> = emptyList()
) {
    val income: List<TransactionCategory>
        get() = categories.filter { it.appliesTo == TransactionType.INCOME && !it.isArchived }

    val expense: List<TransactionCategory>
        get() = categories.filter { it.appliesTo == TransactionType.EXPENSE && !it.isArchived }

    /**
     * Hidden categories, listed apart.
     *
     * They are still doing a job - naming the history filed under them - so the
     * screen shows them rather than pretending they are gone.
     */
    val hidden: List<TransactionCategory> get() = categories.filter { it.isArchived }

    val customCount: Int get() = categories.count { !it.isBuiltIn }
}

/** The add/edit sheet. */
data class CategoryFormState(
    val isVisible: Boolean = false,
    /** Null while creating. */
    val editingId: String? = null,
    val name: String = "",
    val iconKey: String = DEFAULT_ICON_KEY,
    val appliesTo: TransactionType = TransactionType.EXPENSE,
    /** A protected category can be renamed but not moved to the other side. */
    val canChangeSide: Boolean = true,
    val isSaving: Boolean = false,
    val nameError: String? = null
) {
    val isEditing: Boolean get() = editingId != null

    companion object {
        const val DEFAULT_ICON_KEY = "other"
    }
}

/** What just happened, for a one-line confirmation. */
sealed interface CategoryEvent {
    data class Saved(val name: String) : CategoryEvent
    data class Deleted(val name: String) : CategoryEvent
    data class Hidden(val name: String, val transactionCount: Int) : CategoryEvent
    data class Restored(val name: String) : CategoryEvent
    data class Refused(val message: String) : CategoryEvent
}

/**
 * Drives category management.
 *
 * The list is whatever the repository holds; everything else here is the state
 * of the one sheet used to add and edit, kept in the view model so a rotation
 * mid-edit does not lose what was typed.
 */
class CategoriesViewModel(
    getCategoriesUseCase: GetCategoriesUseCase,
    private val saveCategoryUseCase: SaveCategoryUseCase,
    private val removeCategoryUseCase: RemoveCategoryUseCase,
    private val restoreCategoryUseCase: RestoreCategoryUseCase,
    private val countCategoryUsageUseCase: CountCategoryUsageUseCase
) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> = getCategoriesUseCase()
        .map { CategoriesUiState(isLoading = false, categories = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CategoriesUiState()
        )

    private val _form = MutableStateFlow(CategoryFormState())
    val form: StateFlow<CategoryFormState> = _form.asStateFlow()

    private val _event = MutableStateFlow<CategoryEvent?>(null)
    val event: StateFlow<CategoryEvent?> = _event.asStateFlow()

    /** How many transactions stand behind the category being removed. */
    private val _pendingRemovalUsage = MutableStateFlow<Int?>(null)
    val pendingRemovalUsage: StateFlow<Int?> = _pendingRemovalUsage.asStateFlow()

    fun startCreating(type: TransactionType) {
        _form.value = CategoryFormState(isVisible = true, appliesTo = type)
    }

    fun startEditing(category: TransactionCategory) {
        _form.value = CategoryFormState(
            isVisible = true,
            editingId = category.id,
            name = category.name,
            iconKey = category.iconKey,
            appliesTo = category.appliesTo,
            // The side is fixed once transactions exist under it, and every
            // category on this screen already can have.
            canChangeSide = false
        )
    }

    fun dismissForm() {
        _form.update { CategoryFormState() }
    }

    fun setName(value: String) {
        _form.update { it.copy(name = value, nameError = null) }
    }

    fun setIcon(iconKey: String) {
        _form.update { it.copy(iconKey = iconKey) }
    }

    fun setAppliesTo(type: TransactionType) {
        _form.update { if (it.canChangeSide) it.copy(appliesTo = type) else it }
    }

    fun save() {
        val current = _form.value
        if (current.isSaving) return

        _form.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (
                val result = saveCategoryUseCase(
                    id = current.editingId,
                    name = current.name,
                    iconKey = current.iconKey,
                    appliesTo = current.appliesTo
                )
            ) {
                is SaveCategoryUseCase.Result.Saved -> {
                    _event.value = CategoryEvent.Saved(current.name.trim())
                    _form.value = CategoryFormState()
                }

                is SaveCategoryUseCase.Result.Invalid -> {
                    _form.update { it.copy(isSaving = false, nameError = result.message) }
                }
            }
        }
    }

    /** Loads the usage count so the confirmation can say what removing will do. */
    fun prepareRemoval(category: TransactionCategory) {
        viewModelScope.launch {
            _pendingRemovalUsage.value = countCategoryUsageUseCase(category.id)
        }
    }

    fun clearPendingRemoval() {
        _pendingRemovalUsage.value = null
    }

    fun remove(category: TransactionCategory) {
        viewModelScope.launch {
            _event.value = when (val outcome = removeCategoryUseCase(category.id)) {
                is CategoryRemoval.Deleted -> CategoryEvent.Deleted(category.name)
                is CategoryRemoval.Archived ->
                    CategoryEvent.Hidden(category.name, outcome.transactionCount)

                is CategoryRemoval.Protected -> CategoryEvent.Refused(
                    "Aurex files debt, EMI and loan payments under ${category.name}, " +
                        "so it has to stay."
                )
            }
            _pendingRemovalUsage.value = null
        }
    }

    fun restore(category: TransactionCategory) {
        viewModelScope.launch {
            restoreCategoryUseCase(category.id)
            _event.value = CategoryEvent.Restored(category.name)
        }
    }

    fun consumeEvent() {
        _event.value = null
    }
}
