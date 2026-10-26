package com.monebie.app.data.repository

import com.monebie.app.data.database.DatabaseHelper
import com.monebie.app.data.mapper.toDomain
import com.monebie.app.domain.calculation.FinancialCalculator
import com.monebie.app.domain.model.AccountDelta
import com.monebie.app.domain.model.Transaction
import com.monebie.app.domain.model.TransactionDraft

/**
 * The one place a transaction row and an account balance change together.
 *
 * Every module that moves money - manual entry, debt payments, and later EMI
 * installments and loan repayments - posts through this. Keeping it in a single
 * class is what stops each module growing its own slightly different version of
 * "insert a row and adjust a balance", which is how the two drift apart.
 *
 * **Every method must be called from inside a `database.transaction { }` block.**
 * They are not suspend functions and take no locks of their own: the caller owns
 * the transaction boundary, so a debt payment can write its payment row and its
 * transaction row atomically as one unit.
 */
internal class LedgerWriter(private val dbHelper: DatabaseHelper) {

    private val transactions get() = dbHelper.transactionQueries

    /** Inserts [draft] and applies its balance effect. Returns the new transaction id. */
    fun post(draft: TransactionDraft): Long {
        transactions.insertTransaction(
            amount_minor = draft.amount.minorUnits,
            type = draft.type.name,
            category_id = draft.categoryId,
            account_id = draft.accountId,
            transfer_account_id = draft.transferAccountId,
            date = draft.date,
            note = draft.note,
            payment_method = draft.paymentMethod.name,
            source_type = draft.source.type.name,
            source_reference_id = draft.source.referenceId,
            source_payment_id = draft.source.paymentId
        )
        val id = transactions.lastInsertedTransactionId().executeAsOne()
        applyDeltas(FinancialCalculator.balanceDeltas(draft))
        return id
    }

    /** Removes a transaction and undoes its balance effect. No-op if it is already gone. */
    fun reverse(transactionId: Long) {
        val existing = find(transactionId) ?: return
        transactions.deleteTransaction(transactionId)
        applyDeltas(FinancialCalculator.reversingDeltas(existing))
    }

    /**
     * Replaces a transaction in place.
     *
     * Undoes the old effect before applying the new one, so an edit that changes
     * amount, type and account all at once still settles to the right balances.
     */
    fun replace(id: Long, draft: TransactionDraft) {
        val existing = find(id) ?: return
        applyDeltas(FinancialCalculator.reversingDeltas(existing))

        transactions.updateTransaction(
            amountMinor = draft.amount.minorUnits,
            type = draft.type.name,
            categoryId = draft.categoryId,
            accountId = draft.accountId,
            transferAccountId = draft.transferAccountId,
            date = draft.date,
            note = draft.note,
            paymentMethod = draft.paymentMethod.name,
            id = id
        )

        applyDeltas(FinancialCalculator.balanceDeltas(draft))
    }

    fun find(transactionId: Long): Transaction? =
        transactions.getTransactionById(transactionId).executeAsOneOrNull()?.toDomain()

    /**
     * Balances move by relative delta, never by writing an absolute figure, so
     * two postings landing close together cannot overwrite each other.
     */
    private fun applyDeltas(deltas: List<AccountDelta>) {
        deltas.forEach { (accountId, delta) ->
            dbHelper.accountQueries.adjustAccountBalance(delta = delta.minorUnits, id = accountId)
        }
    }
}
