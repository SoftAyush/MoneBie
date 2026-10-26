package com.aurex.app.domain.usecase.debt

import com.aurex.app.core.Money
import com.aurex.app.domain.model.Debt
import com.aurex.app.domain.model.DebtDraft
import com.aurex.app.domain.model.DebtPayment
import com.aurex.app.domain.model.PaymentMethod
import com.aurex.app.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow

class GetDebtsUseCase(private val repository: DebtRepository) {
    operator fun invoke(): Flow<List<Debt>> = repository.observeDebts()
}

class GetDebtUseCase(private val repository: DebtRepository) {
    operator fun invoke(id: Long): Flow<Debt?> = repository.observeDebt(id)

    suspend fun once(id: Long): Debt? = repository.getDebt(id)
}

class GetDebtPaymentsUseCase(private val repository: DebtRepository) {
    operator fun invoke(debtId: Long): Flow<List<DebtPayment>> = repository.observePayments(debtId)
}

class SaveDebtUseCase(private val repository: DebtRepository) {

    suspend operator fun invoke(existingId: Long?, draft: DebtDraft): Long {
        if (existingId == null) return repository.createDebt(draft)
        repository.updateDebt(existingId, draft)
        return existingId
    }
}

class DeleteDebtUseCase(private val repository: DebtRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteDebt(id)
}

/**
 * Records a repayment against a debt.
 *
 * One call covers all four links the traceability rule requires: the debt, the
 * payment record, the transaction, and the account the money left.
 */
class RecordDebtPaymentUseCase(private val repository: DebtRepository) {

    suspend operator fun invoke(
        debtId: Long,
        amount: Money,
        day: String,
        accountId: Long?,
        paymentMethod: PaymentMethod,
        note: String?
    ): Long = repository.recordPayment(
        debtId = debtId,
        amount = amount,
        day = day,
        accountId = accountId,
        paymentMethod = paymentMethod,
        note = note?.trim()?.takeIf { it.isNotEmpty() }
    )
}

class DeleteDebtPaymentUseCase(private val repository: DebtRepository) {
    suspend operator fun invoke(paymentId: Long) = repository.deletePayment(paymentId)
}
