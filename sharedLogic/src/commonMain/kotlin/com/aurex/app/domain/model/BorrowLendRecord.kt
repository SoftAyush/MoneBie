package com.aurex.app.domain.model

import com.aurex.app.core.Money

/**
 * Which way money moved between the user and another person.
 *
 * Borrowing and lending are presented as two separate concepts in the UI, but
 * they share one model: identical fields, identical repayment mechanics, and
 * opposite signs on the balance sheet. Keeping them unified means the
 * settlement and progress maths exists exactly once.
 */
enum class LoanDirection {
    /** The user received money and owes it back - a liability. */
    BORROWED,

    /** The user gave money out and expects it back - an asset. */
    LENT
}

enum class LoanStatus { ACTIVE, PARTIALLY_SETTLED, SETTLED, OVERDUE }

val LoanStatus.displayName: String
    get() = when (this) {
        LoanStatus.ACTIVE -> "Active"
        LoanStatus.PARTIALLY_SETTLED -> "Partially Settled"
        LoanStatus.SETTLED -> "Settled"
        LoanStatus.OVERDUE -> "Overdue"
    }

data class BorrowLendRecord(
    val id: Long,
    val personName: String,
    val direction: LoanDirection,
    val principalAmount: Money,
    /** Sum of this record's LoanRepayment history. */
    val amountSettled: Money = Money.ZERO,
    /** Borrow date, or lending date. yyyy-MM-dd */
    val date: String,
    /** Due date, or expected return date. yyyy-MM-dd */
    val dueDate: String? = null,
    val notes: String? = null,
    /** Account the principal moved through, when it moved through one at all. */
    val accountId: Long? = null,
    /**
     * The transaction posted for the principal itself.
     *
     * Null when the money moved outside any tracked account - a cash loan the
     * user never banked. The record still exists; no balance was touched.
     */
    val principalTransactionId: Long? = null
) {
    val remainingAmount: Money get() = (principalAmount - amountSettled).coerceAtLeastZero()

    val isSettled: Boolean get() = amountSettled >= principalAmount

    val progress: Float get() = amountSettled.ratioOf(principalAmount)
}

/** One repayment in either direction, retained as permanent history. */
data class LoanRepayment(
    val id: Long,
    val recordId: Long,
    val amount: Money,
    /** yyyy-MM-dd */
    val date: String,
    val accountId: Long? = null,
    val transactionId: Long? = null,
    val note: String? = null
)

/**
 * The editable fields of a borrowing or a lending.
 *
 * Excludes the settled amount, which is the sum of the repayment history.
 */
data class BorrowLendDraft(
    val personName: String,
    val direction: LoanDirection,
    val principalAmount: Money,
    /** yyyy-MM-dd */
    val date: String,
    val dueDate: String? = null,
    val notes: String? = null,
    /**
     * Account the principal moves through.
     *
     * When set, creating the record posts the movement so the balance reflects
     * that the cash has actually gone (or arrived). When null the record is
     * history only - the right answer for money that changed hands outside any
     * account the app tracks.
     */
    val accountId: Long? = null
)

fun BorrowLendRecord.toDraft(): BorrowLendDraft = BorrowLendDraft(
    personName = personName,
    direction = direction,
    principalAmount = principalAmount,
    date = date,
    dueDate = dueDate,
    notes = notes,
    accountId = accountId
)

/** Wording for the direction, used wherever a record is described. */
val LoanDirection.displayName: String
    get() = when (this) {
        LoanDirection.BORROWED -> "Borrowed"
        LoanDirection.LENT -> "Lent"
    }

/** Short label for the person's side of the arrangement. */
val LoanDirection.counterpartyLabel: String
    get() = when (this) {
        LoanDirection.BORROWED -> "You owe"
        LoanDirection.LENT -> "Owes you"
    }
