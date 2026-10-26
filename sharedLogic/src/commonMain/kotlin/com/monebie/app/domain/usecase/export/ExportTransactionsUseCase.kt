package com.monebie.app.domain.usecase.export

import com.monebie.app.domain.model.CategoryLookup
import com.monebie.app.domain.repository.AccountRepository
import com.monebie.app.domain.repository.CategoryRepository
import com.monebie.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first

/**
 * Exports the full transaction history.
 *
 * Loads the records and hands them to [TransactionCsv], which owns the format.
 * Accounts and categories come along so ids can be resolved to names - an
 * export of account numbers and category keys would be unreadable.
 */
class ExportTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) {

    suspend operator fun invoke(): ExportedFile {
        val transactions = transactionRepository.observeTransactions().first()
        // Archived accounts and hidden categories included on purpose: old
        // transactions still point at them, and resolving to "Unknown" would
        // lose real information.
        val accounts = accountRepository.observeAccounts().first()
        val categories = categoryRepository.observeCategories().first()
        return TransactionCsv.build(transactions, accounts, CategoryLookup(categories))
    }
}
