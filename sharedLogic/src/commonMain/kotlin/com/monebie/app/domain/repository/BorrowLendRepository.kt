package com.monebie.app.domain.repository

import com.monebie.app.core.Money
import com.monebie.app.domain.model.BorrowLendDraft
import com.monebie.app.domain.model.BorrowLendRecord
import com.monebie.app.domain.model.LoanRepayment
import com.monebie.app.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

/**
 * Money between the user and other people, in both directions.
 *
 * Unlike debts and EMIs, the principal itself moves through a real account here
 * - lending cash means the cash has gone. Creating a record with an account
 * therefore posts that movement, and the four possible directions of money are
 * handled by the implementation rather than the caller.
 */
interface BorrowLendRepository {

    fun observeRecords(): Flow<List<BorrowLendRecord>>

    fun observeRecord(id: Long): Flow<BorrowLendRecord?>

    fun observeRepayments(recordId: Long): Flow<List<LoanRepayment>>

    suspend fun getRecord(id: Long): BorrowLendRecord?

    /**
     * Creates the record and, when the draft names an account, posts the
     * principal movement so the balance reflects that the money really changed
     * hands.
     */
    suspend fun createRecord(draft: BorrowLendDraft): Long

    /**
     * Updates the descriptive fields.
     *
     * Direction, amount and account are fixed once created: changing them would
     * invalidate the principal movement already posted and every repayment
     * recorded against it. Delete and re-create instead.
     */
    suspend fun updateRecord(id: Long, draft: BorrowLendDraft)

    /**
     * Removes the record, its repayment history, and every transaction the app
     * posted for it - principal included.
     *
     * This differs from debts and EMIs on purpose. There, the payments were
     * genuine spending that happened regardless of whether the debt is still
     * tracked. Here the whole arrangement is being said never to have existed,
     * so the money must return to where it started or the balance would be left
     * permanently wrong.
     */
    suspend fun deleteRecord(id: Long)

    /**
     * Records a repayment in whichever direction settles this record.
     *
     * Repaying a borrowing takes money out; being repaid on a lending brings it
     * in. Returns the new repayment id.
     */
    suspend fun recordRepayment(
        recordId: Long,
        amount: Money,
        day: String,
        accountId: Long?,
        paymentMethod: PaymentMethod,
        note: String?
    ): Long

    /** Deletes a repayment and reverses the transaction and balance it created. */
    suspend fun deleteRepayment(repaymentId: Long)
}
