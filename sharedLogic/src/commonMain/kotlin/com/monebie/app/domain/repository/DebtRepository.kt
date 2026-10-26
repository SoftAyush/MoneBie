package com.monebie.app.domain.repository

import com.monebie.app.core.Money
import com.monebie.app.domain.model.Debt
import com.monebie.app.domain.model.DebtDraft
import com.monebie.app.domain.model.DebtPayment
import com.monebie.app.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

/**
 * Money the user owes to people, banks and institutions.
 *
 * Recording a payment is the module's one non-trivial write, and it is
 * deliberately a single call: it creates the payment record, posts the matching
 * transaction, and moves the account balance in one atomic step. There is no
 * way for a caller to do half of it.
 */
interface DebtRepository {

    fun observeDebts(): Flow<List<Debt>>

    fun observeDebt(id: Long): Flow<Debt?>

    fun observePayments(debtId: Long): Flow<List<DebtPayment>>

    suspend fun getDebt(id: Long): Debt?

    suspend fun createDebt(draft: DebtDraft): Long

    suspend fun updateDebt(id: Long, draft: DebtDraft)

    /**
     * Removes the debt and its payment records.
     *
     * The transactions those payments produced are kept: that money genuinely
     * left the account, and erasing it would make the account balance wrong.
     */
    suspend fun deleteDebt(id: Long)

    /**
     * Records a repayment.
     *
     * When [accountId] is set the payment is posted as an expense from that
     * account, so the balance moves and the payment appears in transaction
     * history linked back to this debt. With no account it is recorded as
     * history only - useful for payments the user made outside the app.
     *
     * Returns the new payment id.
     */
    suspend fun recordPayment(
        debtId: Long,
        amount: Money,
        day: String,
        accountId: Long?,
        paymentMethod: PaymentMethod,
        note: String?
    ): Long

    /** Deletes a payment and reverses the transaction and balance it created. */
    suspend fun deletePayment(paymentId: Long)
}
