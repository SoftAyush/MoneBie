package com.aurex.app.domain.model

import com.aurex.app.core.Money

/**
 * What produced a transaction.
 *
 * This is the backbone of the traceability rule: a debt payment, an EMI
 * installment and a loan repayment all also exist as a transaction, and this
 * reference is how the transaction points back at the record it settled.
 * Modelled as three flat fields so it maps to three SQLite columns with no
 * polymorphic serialisation.
 */
enum class TransactionSourceType {
    MANUAL,
    DEBT_PAYMENT,
    EMI_PAYMENT,

    /** Money arriving because the user borrowed it from someone. */
    BORROW_RECEIVED,

    /** Money leaving to repay something the user borrowed. */
    BORROW_REPAYMENT,

    /** Money leaving because the user lent it to someone. */
    LEND_GIVEN,

    /** Money arriving because someone repaid what they were lent. */
    LEND_REPAYMENT,

    ACCOUNT_OPENING,
    TRANSFER,

    /**
     * Posted on the user's behalf from a recurring schedule. Owned by the user
     * rather than by the schedule: unlike a debt or EMI payment there is no
     * payment history to keep in step, so the row stays editable.
     */
    RECURRING
}

data class TransactionSource(
    val type: TransactionSourceType = TransactionSourceType.MANUAL,
    /** Id of the debt / EMI / borrow-lend record this settled. */
    val referenceId: Long? = null,
    /** Id of the individual payment row inside that record's history. */
    val paymentId: Long? = null
) {
    /**
     * Whether a module other than manual entry owns this row.
     *
     * A recurring occurrence is excluded: it was posted automatically, but
     * nothing else in the app depends on it staying as posted, so the user may
     * correct or remove it like anything they typed themselves.
     */
    val isSystemGenerated: Boolean
        get() = type != TransactionSourceType.MANUAL && type != TransactionSourceType.RECURRING

    /** Whether a recurring schedule produced this row. */
    val isRecurring: Boolean get() = type == TransactionSourceType.RECURRING

    /**
     * Whether this transaction moves money between the user and a person
     * without earning or spending anything.
     *
     * Lending cash turns an asset into a receivable; being repaid turns it
     * back. Borrowing raises cash and a liability together; repaying lowers
     * both. None of the four is income or expenditure, so they are excluded
     * from income/expense totals - see FinancialCalculator.summarise. They
     * still move account balances, because the cash genuinely moved.
     */
    val isLoanPrincipalMovement: Boolean
        get() = type == TransactionSourceType.BORROW_RECEIVED ||
            type == TransactionSourceType.BORROW_REPAYMENT ||
            type == TransactionSourceType.LEND_GIVEN ||
            type == TransactionSourceType.LEND_REPAYMENT

    companion object {
        val Manual = TransactionSource()
    }
}

data class Transaction(
    val id: Long,
    val amount: Money,
    val type: TransactionType,
    val categoryId: String,
    val accountId: Long,
    /** Timestamp, yyyy-MM-dd HH:mm:ss. */
    val date: String,
    val note: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val source: TransactionSource = TransactionSource.Manual,
    /** Destination account for a TRANSFER. */
    val transferAccountId: Long? = null
) {
    val category: TransactionCategory get() = TransactionCategories.find(categoryId)

    /** Effect of this transaction on the owning account balance. */
    val signedAmount: Money get() = amount * type.balanceSign
}
