package com.monebie.app.domain.usecase.category

import com.monebie.app.domain.model.TransactionCategory
import com.monebie.app.domain.model.TransactionType
import com.monebie.app.domain.repository.CategoryRemoval
import com.monebie.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

/** Everything, archived included - the management screen shows what it hid. */
class GetCategoriesUseCase(private val repository: CategoryRepository) {
    operator fun invoke(): Flow<List<TransactionCategory>> = repository.observeCategories()
}

/** What the pickers offer: everything not hidden. */
class GetActiveCategoriesUseCase(private val repository: CategoryRepository) {
    operator fun invoke(): Flow<List<TransactionCategory>> = repository.observeActiveCategories()
}

/**
 * Creates or renames a category.
 *
 * Validation lives here rather than in the view model so the same rules apply
 * wherever a category is saved from.
 */
class SaveCategoryUseCase(private val repository: CategoryRepository) {

    sealed interface Result {
        data class Saved(val id: String) : Result
        data class Invalid(val message: String) : Result
    }

    suspend operator fun invoke(
        id: String?,
        name: String,
        iconKey: String,
        appliesTo: TransactionType
    ): Result {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.Invalid("Give the category a name")
        if (trimmed.length > MAX_NAME_LENGTH) {
            return Result.Invalid("Keep it under $MAX_NAME_LENGTH characters")
        }

        return if (id == null) {
            Result.Saved(repository.createCategory(trimmed, iconKey, appliesTo))
        } else {
            repository.updateCategory(id, trimmed, iconKey)
            Result.Saved(id)
        }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 28
    }
}

/** Deletes an unused category, hides one with history, refuses a protected one. */
class RemoveCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(id: String): CategoryRemoval = repository.removeCategory(id)
}

/** Brings a hidden category back into the pickers. */
class RestoreCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(id: String) = repository.setArchived(id, archived = false)
}

/** How much history stands behind a category, for the confirmation copy. */
class CountCategoryUsageUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(id: String): Int = repository.countTransactionsIn(id)
}
