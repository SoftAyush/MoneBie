package com.aurex.app.domain.repository

import com.aurex.app.core.Money
import com.aurex.app.domain.model.Emi
import com.aurex.app.domain.model.EmiDraft
import com.aurex.app.domain.model.EmiPayment
import com.aurex.app.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

/**
 * Fixed-installment obligations.
 *
 * Marking an installment paid is one atomic call that advances the schedule,
 * writes the payment record, posts the transaction and moves the account
 * balance. Nothing else advances an EMI - there is no "set installments paid".
 */
interface EmiRepository {

    fun observeEmis(): Flow<List<Emi>>

    fun observeEmi(id: Long): Flow<Emi?>

    fun observePayments(emiId: Long): Flow<List<EmiPayment>>

    suspend fun getEmi(id: Long): Emi?

    suspend fun createEmi(draft: EmiDraft): Long

    suspend fun updateEmi(id: Long, draft: EmiDraft)

    /**
     * Removes the EMI and its installment records.
     *
     * The transactions those installments produced are kept: that money left
     * the account for real, and removing it would make the balance wrong.
     */
    suspend fun deleteEmi(id: Long)

    /**
     * Marks the next installment paid.
     *
     * The installment number and due date come from the schedule, not from the
     * caller, so an installment can never be recorded out of order. Returns the
     * new payment id, or null when the EMI is already complete.
     */
    suspend fun markInstallmentPaid(
        emiId: Long,
        amount: Money,
        paidDay: String,
        accountId: Long?,
        paymentMethod: PaymentMethod
    ): Long?

    /**
     * Undoes the most recently paid installment.
     *
     * Only the latest can be undone: removing one from the middle would leave a
     * gap, and the next due date is derived from how many payments exist, so a
     * gap would point the schedule at a month that was already settled.
     */
    suspend fun undoLastInstallment(emiId: Long)
}
