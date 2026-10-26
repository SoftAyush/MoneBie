package com.monebie.app.domain.usecase.emi

import com.monebie.app.core.Money
import com.monebie.app.domain.model.Emi
import com.monebie.app.domain.model.EmiDraft
import com.monebie.app.domain.model.EmiPayment
import com.monebie.app.domain.model.PaymentMethod
import com.monebie.app.domain.repository.EmiRepository
import kotlinx.coroutines.flow.Flow

class GetEmisUseCase(private val repository: EmiRepository) {
    operator fun invoke(): Flow<List<Emi>> = repository.observeEmis()
}

class GetEmiUseCase(private val repository: EmiRepository) {
    operator fun invoke(id: Long): Flow<Emi?> = repository.observeEmi(id)

    suspend fun once(id: Long): Emi? = repository.getEmi(id)
}

class GetEmiPaymentsUseCase(private val repository: EmiRepository) {
    operator fun invoke(emiId: Long): Flow<List<EmiPayment>> = repository.observePayments(emiId)
}

class SaveEmiUseCase(private val repository: EmiRepository) {

    suspend operator fun invoke(existingId: Long?, draft: EmiDraft): Long {
        if (existingId == null) return repository.createEmi(draft)
        repository.updateEmi(existingId, draft)
        return existingId
    }
}

class DeleteEmiUseCase(private val repository: EmiRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteEmi(id)
}

/**
 * Marks the next installment paid.
 *
 * Covers everything the spec asks of this action in one step: the payment
 * record, the reduced remaining installments and balance, the deduction from
 * the account, the advanced next-payment date, and the preserved history.
 */
class MarkInstallmentPaidUseCase(private val repository: EmiRepository) {

    suspend operator fun invoke(
        emiId: Long,
        amount: Money,
        paidDay: String,
        accountId: Long?,
        paymentMethod: PaymentMethod
    ): Long? = repository.markInstallmentPaid(
        emiId = emiId,
        amount = amount,
        paidDay = paidDay,
        accountId = accountId,
        paymentMethod = paymentMethod
    )
}

class UndoLastInstallmentUseCase(private val repository: EmiRepository) {
    suspend operator fun invoke(emiId: Long) = repository.undoLastInstallment(emiId)
}
