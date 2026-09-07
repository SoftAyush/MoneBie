package com.aurex.app.domain.repository

import com.aurex.app.domain.model.TransactionCategory
import com.aurex.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

/** What happened to a category the user asked to remove. */
sealed interface CategoryRemoval {
    /** Nothing was filed under it, so it is gone. */
    data object Deleted : CategoryRemoval

    /** It had history behind it and was hidden instead, so that history keeps its label. */
    data class Archived(val transactionCount: Int) : CategoryRemoval

    /** The app posts to this category itself and cannot lose it. */
    data object Protected : CategoryRemoval
}

interface CategoryRepository {

    /** Everything, archived included - for the management screen. */
    fun observeCategories(): Flow<List<TransactionCategory>>

    /** What a picker should offer. */
    fun observeActiveCategories(): Flow<List<TransactionCategory>>

    suspend fun getCategory(id: String): TransactionCategory?

    /** Returns the generated id of the new category. */
    suspend fun createCategory(
        name: String,
        iconKey: String,
        appliesTo: TransactionType
    ): String

    /** Only the label and icon: the side it belongs to is baked into its transactions. */
    suspend fun updateCategory(id: String, name: String, iconKey: String)

    suspend fun setArchived(id: String, archived: Boolean)

    /**
     * Deletes an unused custom category, or archives one with history.
     *
     * The choice is made here rather than by the caller because it depends on
     * a count only the database can answer.
     */
    suspend fun removeCategory(id: String): CategoryRemoval

    /** How many transactions are filed under a category, for the confirmation copy. */
    suspend fun countTransactionsIn(id: String): Int
}
