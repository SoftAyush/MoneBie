package com.monebie.app.domain.model

/**
 * Optional narrowing applied to a month of transactions.
 *
 * Filtering happens in memory over the already-loaded month window rather than
 * as SQL: the window is bounded, and pushing three optional predicates into the
 * query would mean either a combinatorial set of statements or string-built SQL.
 *
 * Payment method is deliberately not among them. It is derived from the account
 * a transaction was recorded against, so filtering by it only ever restated the
 * account filter sitting directly above it.
 */
data class TransactionFilter(
    val type: TransactionType? = null,
    val accountId: Long? = null,
    val categoryId: String? = null
) {
    val activeCount: Int
        get() = listOfNotNull(type, accountId, categoryId).size

    val isActive: Boolean get() = activeCount > 0

    fun matches(transaction: Transaction): Boolean {
        if (type != null && transaction.type != type) return false
        if (categoryId != null && transaction.categoryId != categoryId) return false
        if (accountId != null) {
            // A transfer touches two accounts, and belongs in the list for both.
            val touchesAccount = transaction.accountId == accountId ||
                transaction.transferAccountId == accountId
            if (!touchesAccount) return false
        }
        return true
    }

    fun cleared(): TransactionFilter = TransactionFilter()
}
