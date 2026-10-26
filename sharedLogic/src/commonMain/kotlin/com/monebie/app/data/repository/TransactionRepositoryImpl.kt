package com.aurex.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.aurex.app.data.database.DatabaseHelper
import com.aurex.app.data.mapper.toDomain
import com.aurex.app.domain.model.Transaction
import com.aurex.app.domain.model.TransactionDraft
import com.aurex.app.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * SQLDelight-backed ledger for manually entered transactions.
 *
 * Writes delegate to [LedgerWriter] inside a database transaction, so the row
 * and the balance change land together or not at all.
 */
class TransactionRepositoryImpl(
    private val dbHelper: DatabaseHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : TransactionRepository {

    private val queries get() = dbHelper.transactionQueries
    private val ledger = LedgerWriter(dbHelper)

    override fun observeTransactions(): Flow<List<Transaction>> =
        queries.selectAllTransactions()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeRecentTransactions(limit: Int): Flow<List<Transaction>> =
        queries.selectRecentTransactions(limit.toLong())
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeTransactionsBetween(
        startInclusive: String,
        endExclusive: String
    ): Flow<List<Transaction>> =
        queries.selectTransactionsBetween(startInclusive, endExclusive)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getTransaction(id: Long): Transaction? = withContext(ioDispatcher) {
        queries.getTransactionById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getLatestTransaction(): Transaction? = withContext(ioDispatcher) {
        queries.selectLatestTransaction().executeAsOneOrNull()?.toDomain()
    }

    override suspend fun postTransaction(draft: TransactionDraft): Long = withContext(ioDispatcher) {
        dbHelper.database.transactionWithResult { ledger.post(draft) }
    }

    override suspend fun postTransactions(drafts: List<TransactionDraft>): Int =
        withContext(ioDispatcher) {
            dbHelper.database.transactionWithResult {
                drafts.forEach { ledger.post(it) }
                drafts.size
            }
        }

    override suspend fun updateTransaction(id: Long, draft: TransactionDraft) {
        withContext(ioDispatcher) {
            dbHelper.database.transaction {
                // A transaction created by a debt payment (and later an EMI
                // installment) is owned by that record. Editing it here would
                // leave the payment history saying one thing and the ledger
                // another, so it is managed from the module that created it.
                if (ledger.find(id)?.source?.isSystemGenerated == true) return@transaction
                ledger.replace(id, draft)
            }
        }
    }

    override suspend fun deleteTransaction(id: Long) {
        withContext(ioDispatcher) {
            dbHelper.database.transaction {
                if (ledger.find(id)?.source?.isSystemGenerated == true) return@transaction
                ledger.reverse(id)
            }
        }
    }
}
