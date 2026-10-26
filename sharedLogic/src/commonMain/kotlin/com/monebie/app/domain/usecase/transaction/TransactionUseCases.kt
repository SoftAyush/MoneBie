package com.monebie.app.domain.usecase.transaction

import com.monebie.app.domain.model.PaymentMethod
import com.monebie.app.domain.model.Transaction
import com.monebie.app.domain.model.TransactionDraft
import com.monebie.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Transactions in the half-open day window `[startInclusive, endExclusive)`.
 *
 * Screens ask for a window rather than the whole ledger, so a user with years
 * of history pays the same cost as one with a week of it.
 */
class GetTransactionsInRangeUseCase(private val repository: TransactionRepository) {
    operator fun invoke(startInclusive: String, endExclusive: String): Flow<List<Transaction>> =
        repository.observeTransactionsBetween(startInclusive, endExclusive)
}

class GetRecentTransactionsUseCase(private val repository: TransactionRepository) {
    operator fun invoke(limit: Int): Flow<List<Transaction>> =
        repository.observeRecentTransactions(limit)
}

class GetTransactionUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(id: Long): Transaction? = repository.getTransaction(id)
}

/**
 * Saves a new transaction or replaces an existing one.
 *
 * Both paths go through the repository's atomic posting, so the account
 * balances move with the row in either case.
 */
class SaveTransactionUseCase(private val repository: TransactionRepository) {

    suspend operator fun invoke(existingId: Long?, draft: TransactionDraft): Long {
        if (existingId == null) return repository.postTransaction(draft)
        repository.updateTransaction(existingId, draft)
        return existingId
    }
}

class DeleteTransactionUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteTransaction(id)
}

/**
 * The account and payment method the user picked last time.
 *
 * Most people record from the same account most days, so seeding these turns a
 * repeat entry into amount plus category and nothing else.
 */
data class TransactionDefaults(
    val accountId: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH
)

class GetTransactionDefaultsUseCase(private val repository: TransactionRepository) {

    suspend operator fun invoke(): TransactionDefaults {
        val latest = repository.getLatestTransaction() ?: return TransactionDefaults()
        return TransactionDefaults(
            accountId = latest.accountId,
            paymentMethod = latest.paymentMethod
        )
    }
}
