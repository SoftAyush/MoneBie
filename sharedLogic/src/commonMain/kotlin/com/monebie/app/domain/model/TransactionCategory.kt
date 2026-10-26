package com.monebie.app.domain.model

/**
 * Categories are referenced by a stable string id rather than an enum ordinal,
 * which is what lets a user-created category sit beside the built-in ones in
 * the same column of the same table with no data migration.
 *
 * [isProtected] marks the categories the app itself posts to - debt payments,
 * EMI installments, money lent and borrowed, transfers. A repository writes
 * those ids directly, so they can be renamed but never removed or moved to the
 * other side of the ledger.
 */
data class TransactionCategory(
    val id: String,
    val name: String,
    val iconKey: String,
    val appliesTo: TransactionType,
    val isBuiltIn: Boolean = true,
    val isProtected: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
) {
    /** Renaming and re-iconing is always allowed; existence is not always the user's to decide. */
    val canBeRemoved: Boolean get() = !isProtected && !isBuiltIn

    /** A built-in that is not load-bearing can be hidden rather than deleted. */
    val canBeArchived: Boolean get() = !isProtected
}

/**
 * The built-in set.
 *
 * These are seeded into the Category table - by Category.sq on a fresh install
 * and by 7.sqm on an existing one - and the table is the source of truth from
 * then on, because a user may have renamed or hidden any of them. What is left
 * here is the seed itself and a last-resort resolver for an id whose row has
 * gone missing.
 */
object TransactionCategories {

    val income: List<TransactionCategory> = listOf(
        built("salary", "Salary", "salary", TransactionType.INCOME, 10),
        built("business", "Business", "business", TransactionType.INCOME, 20),
        built("freelance", "Freelance", "freelance", TransactionType.INCOME, 30),
        built("investment", "Investment", "investment", TransactionType.INCOME, 40),
        built("rental", "Rental", "rental", TransactionType.INCOME, 50),
        built("gift_in", "Gift", "gift", TransactionType.INCOME, 60),
        built("refund", "Refund", "refund", TransactionType.INCOME, 70),
        built("borrowed", "Borrowed", "lending", TransactionType.INCOME, 80, protected = true),
        built("loan_returned", "Loan Returned", "lending", TransactionType.INCOME, 90, protected = true),
        built("other_income", "Other", "other", TransactionType.INCOME, 100)
    )

    val expense: List<TransactionCategory> = listOf(
        built("food", "Food & Drink", "food", TransactionType.EXPENSE, 10),
        built("groceries", "Groceries", "groceries", TransactionType.EXPENSE, 20),
        built("transport", "Transport", "transport", TransactionType.EXPENSE, 30),
        built("housing", "Rent & Housing", "housing", TransactionType.EXPENSE, 40),
        built("utilities", "Utilities", "utilities", TransactionType.EXPENSE, 50),
        built("health", "Health", "health", TransactionType.EXPENSE, 60),
        built("education", "Education", "education", TransactionType.EXPENSE, 70),
        built("shopping", "Shopping", "shopping", TransactionType.EXPENSE, 80),
        built("entertainment", "Entertainment", "entertainment", TransactionType.EXPENSE, 90),
        built("subscriptions", "Subscriptions", "subscriptions", TransactionType.EXPENSE, 100),
        built("debt_payment", "Debt Payment", "debt", TransactionType.EXPENSE, 110, protected = true),
        built("emi_payment", "EMI Payment", "emi", TransactionType.EXPENSE, 120, protected = true),
        built("lending", "Money Lent", "lending", TransactionType.EXPENSE, 130, protected = true),
        built("loan_repayment", "Loan Repayment", "lending", TransactionType.EXPENSE, 140, protected = true),
        built("other_expense", "Other", "other", TransactionType.EXPENSE, 150)
    )

    val transfer = built("transfer", "Transfer", "transfer", TransactionType.TRANSFER, 10, protected = true)

    /** Everything seeded, in the order the table holds it. */
    val all: List<TransactionCategory> = income + expense + listOf(transfer)

    private val byId: Map<String, TransactionCategory> = all.associateBy { it.id }

    fun forType(type: TransactionType): List<TransactionCategory> = when (type) {
        TransactionType.INCOME -> income
        TransactionType.EXPENSE -> expense
        TransactionType.TRANSFER -> listOf(transfer)
    }

    /**
     * Last resort for an id with no row behind it.
     *
     * Screens read categories from the repository; this exists so a deleted or
     * not-yet-loaded id renders as something readable instead of crashing a
     * list or printing a raw key at the user.
     */
    fun find(id: String): TransactionCategory =
        byId[id] ?: TransactionCategory(
            id = id,
            name = "Uncategorised",
            iconKey = "other",
            appliesTo = TransactionType.EXPENSE,
            isBuiltIn = false
        )

    private fun built(
        id: String,
        name: String,
        iconKey: String,
        appliesTo: TransactionType,
        sortOrder: Int,
        protected: Boolean = false
    ) = TransactionCategory(
        id = id,
        name = name,
        iconKey = iconKey,
        appliesTo = appliesTo,
        isBuiltIn = true,
        isProtected = protected,
        sortOrder = sortOrder
    )
}

/**
 * Resolves category ids to their current definition.
 *
 * Passed into anything that has to label a transaction - the insights
 * breakdown, the CSV export, a row in a list - so those all read the user's
 * renamed categories rather than the seed the app shipped with.
 */
class CategoryLookup(categories: List<TransactionCategory>) {

    private val byId: Map<String, TransactionCategory> = categories.associateBy { it.id }

    operator fun get(id: String): TransactionCategory =
        byId[id] ?: TransactionCategories.find(id)

    companion object {
        /** The seed set, for callers with nothing loaded yet. */
        val Seeded = CategoryLookup(TransactionCategories.all)
    }
}
