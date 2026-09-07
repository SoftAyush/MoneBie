package com.aurex.app.domain.usecase.borrowlend

import com.aurex.app.core.Money
import com.aurex.app.domain.model.BorrowLendDraft
import com.aurex.app.domain.model.BorrowLendRecord
import com.aurex.app.domain.model.LoanRepayment
import com.aurex.app.domain.model.PaymentMethod
import com.aurex.app.domain.repository.BorrowLendRepository
import kotlinx.coroutines.flow.Flow

class GetLoanRecordsUseCase(private val repository: BorrowLendRepository) {
    operator fun invoke(): Flow<List<BorrowLendRecord>> = repository.observeRecords()
}

class GetLoanRecordUseCase(private val repository: BorrowLendRepository) {
    operator fun invoke(id: Long): Flow<BorrowLendRecord?> = repository.observeRecord(id)

    suspend fun once(id: Long): BorrowLendRecord? = repository.getRecord(id)
}

class GetLoanRepaymentsUseCase(private val repository: BorrowLendRepository) {
    operator fun invoke(recordId: Long): Flow<List<LoanRepayment>> =
        repository.observeRepayments(recordId)
}

class SaveLoanRecordUseCase(private val repository: BorrowLendRepository) {

    suspend operator fun invoke(existingId: Long?, draft: BorrowLendDraft): Long {
        if (existingId == null) return repository.createRecord(draft)
        repository.updateRecord(existingId, draft)
        return existingId
    }
}

class DeleteLoanRecordUseCase(private val repository: BorrowLendRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteRecord(id)
}

/**
 * Records a repayment in whichever direction settles the record.
 *
 * Covers all four links the traceability rule requires: the record, the
 * repayment row, the transaction, and the account the money moved through.
 */
class RecordLoanRepaymentUseCase(private val repository: BorrowLendRepository) {

    suspend operator fun invoke(
        recordId: Long,
        amount: Money,
        day: String,
        accountId: Long?,
        paymentMethod: PaymentMethod,
        note: String?
    ): Long = repository.recordRepayment(
        recordId = recordId,
        amount = amount,
        day = day,
        accountId = accountId,
        paymentMethod = paymentMethod,
        note = note?.trim()?.takeIf { it.isNotEmpty() }
    )
}

class DeleteLoanRepaymentUseCase(private val repository: BorrowLendRepository) {
    suspend operator fun invoke(repaymentId: Long) = repository.deleteRepayment(repaymentId)
}
