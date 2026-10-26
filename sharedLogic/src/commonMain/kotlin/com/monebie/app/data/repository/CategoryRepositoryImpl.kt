package com.aurex.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.aurex.app.core.getCurrentDateString
import com.aurex.app.data.database.DatabaseHelper
import com.aurex.app.data.mapper.toDomain
import com.aurex.app.domain.model.TransactionCategory
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.domain.repository.CategoryRemoval
import com.aurex.app.domain.repository.CategoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CategoryRepositoryImpl(
    private val dbHelper: DatabaseHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : CategoryRepository {

    override fun observeCategories(): Flow<List<TransactionCategory>> =
        dbHelper.categoryQueries.selectAllCategories()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeActiveCategories(): Flow<List<TransactionCategory>> =
        dbHelper.categoryQueries.selectActiveCategories()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getCategory(id: String): TransactionCategory? = withContext(ioDispatcher) {
        dbHelper.categoryQueries.getCategoryById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun createCategory(
        name: String,
        iconKey: String,
        appliesTo: TransactionType
    ): String = withContext(ioDispatcher) {
        val id = generateId(name)
        dbHelper.categoryQueries.transaction {
            val sortOrder = dbHelper.categoryQueries
                .nextSortOrder(appliesTo.name)
                .executeAsOne()

            dbHelper.categoryQueries.insertCategory(
                id = id,
                name = name,
                icon_key = iconKey,
                applies_to = appliesTo.name,
                sort_order = sortOrder,
                created_at = getCurrentDateString()
            )
        }
        id
    }

    override suspend fun updateCategory(id: String, name: String, iconKey: String) {
        withContext(ioDispatcher) {
            dbHelper.categoryQueries.updateCategory(name = name, iconKey = iconKey, id = id)
        }
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        withContext(ioDispatcher) {
            dbHelper.categoryQueries.setCategoryArchived(
                archived = if (archived) 1L else 0L,
                id = id
            )
        }
    }

    override suspend fun removeCategory(id: String): CategoryRemoval = withContext(ioDispatcher) {
        val category = dbHelper.categoryQueries.getCategoryById(id).executeAsOneOrNull()?.toDomain()
            ?: return@withContext CategoryRemoval.Deleted

        if (category.isProtected) return@withContext CategoryRemoval.Protected

        val used = countTransactionsIn(id)

        // A category with history is hidden, never dropped: the transactions
        // filed under it keep pointing at this id, and deleting the row would
        // leave months of the ledger labelled "Uncategorised".
        if (used > 0 || category.isBuiltIn) {
            dbHelper.categoryQueries.setCategoryArchived(archived = 1L, id = id)
            CategoryRemoval.Archived(used)
        } else {
            dbHelper.categoryQueries.deleteCategory(id)
            CategoryRemoval.Deleted
        }
    }

    override suspend fun countTransactionsIn(id: String): Int = withContext(ioDispatcher) {
        dbHelper.transactionQueries.countTransactionsInCategory(id).executeAsOne().toInt()
    }

    /**
     * A readable id derived from the name, with a timestamp suffix.
     *
     * Readable because these ids end up in exported CSVs and in the transaction
     * rows themselves; suffixed because two categories may legitimately be
     * named the same thing on opposite sides of the ledger, and the id is the
     * primary key.
     */
    private fun generateId(name: String): String {
        val slug = name.trim().lowercase()
            .map { if (it.isLetterOrDigit()) it else '_' }
            .joinToString("")
            .trim('_')
            .take(24)
            .ifEmpty { "category" }

        return slug + "_" + getCurrentDateString().filter { it.isDigit() }.takeLast(8)
    }
}
