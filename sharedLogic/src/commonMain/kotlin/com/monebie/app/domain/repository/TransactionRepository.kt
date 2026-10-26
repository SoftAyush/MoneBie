package com.monebie.app.domain.repository

import com.monebie.app.domain.model.Transaction
import com.monebie.app.domain.model.TransactionDraft
import kotlinx.coroutines.flow.Flow

/**
 * The transaction ledger.
 *
 * Writes here are *postings*, not plain inserts: saving, editing or deleting a
 * transaction also moves the affected account balances, and the implementation
 * does both inside one database transaction. That is the contract - a caller
 * never adjusts a balance itself, so a balance cannot drift from the rows that
 * justify it.
 *
 * Later phases post through this same interface: a debt payment and an EMI
 * installment become drafts carrying a
 * [com.monebie.app.domain.model.TransactionSource] that points back at the record
 * they settled.
 */
interface TransactionRepository {

    fun observeTransactions(): Flow<List<Transaction>>

    fun observeRecentTransactions(limit: Int): Flow<List<Transaction>>

    /**
     * Transactions in the half-open window `[startInclusive, endExclusive)`,
     * both `yyyy-MM-dd`.
     *
     * Screens read a bounded window rather than the whole ledger, so cost stays
     * flat as history grows.
     */
    fun observeTransactionsBetween(startInclusive: String, endExclusive: String): Flow<List<Transaction>>

    suspend fun getTransaction(id: Long): Transaction?

    /** Most recent entry, used to seed the add screen's defaults. */
    suspend fun getLatestTransaction(): Transaction?

    /** Inserts [draft] and applies its balance effect atomically. Returns the new id. */
    suspend fun postTransaction(draft: TransactionDraft): Long

    /**
     * Posts every draft inside one database transaction. Returns how many
     * were written - all of them, or none if anything failed.
     *
     * For imports: a thousand rows posted one at a time could be interrupted
     * halfway and leave the user unable to tell which half arrived.
     */
    suspend fun postTransactions(drafts: List<TransactionDraft>): Int

    /** Replaces a transaction, reversing the old balance effect and applying the new one. */
    suspend fun updateTransaction(id: Long, draft: TransactionDraft)

    /** Removes a transaction and undoes its balance effect. */
    suspend fun deleteTransaction(id: Long)
}
