package com.aurex.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.aurex.app.core.Money
import com.aurex.app.core.getCurrentDateString
import com.aurex.app.core.timestampForDay
import com.aurex.app.data.database.DatabaseHelper
import com.aurex.app.data.mapper.toDomain
import com.aurex.app.domain.model.BorrowLendDraft
import com.aurex.app.domain.model.BorrowLendRecord
import com.aurex.app.domain.model.LoanDirection
import com.aurex.app.domain.model.LoanRepayment
import com.aurex.app.domain.model.PaymentMethod
import com.aurex.app.domain.model.TransactionDraft
import com.aurex.app.domain.model.TransactionSource
import com.aurex.app.domain.model.TransactionSourceType
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.domain.repository.BorrowLendRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BorrowLendRepositoryImpl(
    private val dbHelper: DatabaseHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : BorrowLendRepository {

    private val queries get() = dbHelper.borrowLendQueries
    private val ledger = LedgerWriter(dbHelper)

    override fun observeRecords(): Flow<List<BorrowLendRecord>> =
        queries.selectAllLoanRecords()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeRecord(id: Long): Flow<BorrowLendRecord?> =
        queries.selectLoanRecordById(id)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }

    override fun observeRepayments(recordId: Long): Flow<List<LoanRepayment>> =
        queries.selectRepaymentsForRecord(recordId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getRecord(id: Long): BorrowLendRecord? = withContext(ioDispatcher) {
        queries.selectLoanRecordById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun createRecord(draft: BorrowLendDraft): Long = withContext(ioDispatcher) {
        dbHelper.database.transactionWithResult {
            queries.insertLoanRecord(
                person_name = draft.personName.trim(),
                direction = draft.direction.name,
                principal_amount_minor = draft.principalAmount.minorUnits,
                date = draft.date,
                due_date = draft.dueDate,
                notes = draft.notes,
                account_id = draft.accountId,
                principal_transaction_id = null,
                created_at = getCurrentDateString()
            )
            val recordId = queries.lastInsertedLoanRecordId().executeAsOne()

            /*
             * Posting the principal is what keeps the net position honest.
             *
             * Lend 1,500 from the bank and the cash has genuinely left: without
             * this, the bank balance would still show it while the record also
             * counts it as money owed to the user - the same 1,500 twice.
             * Borrowing is the mirror image: the cash arrives and the liability
             * appears alongside it, so the net position is unchanged.
             */
            if (draft.accountId != null) {
                val transactionId = ledger.post(
                    principalDraft(
                        direction = draft.direction,
                        personName = draft.personName.trim(),
                        amount = draft.principalAmount,
                        day = draft.date,
                        accountId = draft.accountId,
                        recordId = recordId
                    )
                )
                queries.linkLoanPrincipalTransaction(transactionId = transactionId, id = recordId)
            }

            recordId
        }
    }

    override suspend fun updateRecord(id: Long, draft: BorrowLendDraft) {
        withContext(ioDispatcher) {
            // Direction, amount and account are intentionally not updated here;
            // see BorrowLendRepository for why.
            queries.updateLoanRecord(
                personName = draft.personName.trim(),
                direction = draft.direction.name,
                principalAmountMinor = draft.principalAmount.minorUnits,
                date = draft.date,
                dueDate = draft.dueDate,
                notes = draft.notes,
                id = id
            )
        }
    }

    override suspend fun deleteRecord(id: Long) {
        withContext(ioDispatcher) {
            dbHelper.database.transaction {
                val record = queries.selectLoanRecordById(id)
                    .executeAsOneOrNull()?.toDomain() ?: return@transaction

                // Every transaction this arrangement produced is undone, so the
                // accounts return to where they would have been without it.
                queries.selectRepaymentTransactionIds(id).executeAsList()
                    .filterNotNull()
                    .forEach { ledger.reverse(it) }
                record.principalTransactionId?.let { ledger.reverse(it) }

                queries.deleteRepaymentsForRecord(id)
                queries.deleteLoanRecord(id)
            }
        }
    }

    override suspend fun recordRepayment(
        recordId: Long,
        amount: Money,
        day: String,
        accountId: Long?,
        paymentMethod: PaymentMethod,
        note: String?
    ): Long = withContext(ioDispatcher) {
        dbHelper.database.transactionWithResult {
            val record = queries.selectLoanRecordById(recordId).executeAsOneOrNull()?.toDomain()

            queries.insertLoanRepayment(
                record_id = recordId,
                amount_minor = amount.minorUnits,
                date = day,
                account_id = accountId,
                transaction_id = null,
                note = note
            )
            val repaymentId = queries.lastInsertedLoanRepaymentId().executeAsOne()

            if (accountId != null && record != null) {
                val transactionId = ledger.post(
                    repaymentDraft(
                        direction = record.direction,
                        personName = record.personName,
                        amount = amount,
                        day = day,
                        accountId = accountId,
                        recordId = recordId,
                        repaymentId = repaymentId,
                        paymentMethod = paymentMethod,
                        note = note
                    )
                )
                queries.linkLoanRepaymentTransaction(transactionId = transactionId, id = repaymentId)
            }

            repaymentId
        }
    }

    override suspend fun deleteRepayment(repaymentId: Long) {
        withContext(ioDispatcher) {
            dbHelper.database.transaction {
                val repayment = queries.getLoanRepaymentById(repaymentId)
                    .executeAsOneOrNull()?.toDomain() ?: return@transaction

                repayment.transactionId?.let { ledger.reverse(it) }
                queries.deleteLoanRepayment(repaymentId)
            }
        }
    }

    /**
     * The principal movement.
     *
     * Borrowing brings money in; lending sends it out. This is the one place
     * that mapping is written down.
     */
    private fun principalDraft(
        direction: LoanDirection,
        personName: String,
        amount: Money,
        day: String,
        accountId: Long,
        recordId: Long
    ): TransactionDraft {
        val borrowed = direction == LoanDirection.BORROWED
        return TransactionDraft(
            amount = amount,
            type = if (borrowed) TransactionType.INCOME else TransactionType.EXPENSE,
            categoryId = if (borrowed) BORROWED_CATEGORY else LENT_CATEGORY,
            accountId = accountId,
            date = timestampForDay(day),
            note = if (borrowed) "Borrowed from $personName" else "Lent to $personName",
            paymentMethod = PaymentMethod.OTHER,
            source = TransactionSource(
                type = if (borrowed) {
                    TransactionSourceType.BORROW_RECEIVED
                } else {
                    TransactionSourceType.LEND_GIVEN
                },
                referenceId = recordId
            )
        )
    }

    /** A repayment moves money the opposite way to the principal that created it. */
    private fun repaymentDraft(
        direction: LoanDirection,
        personName: String,
        amount: Money,
        day: String,
        accountId: Long,
        recordId: Long,
        repaymentId: Long,
        paymentMethod: PaymentMethod,
        note: String?
    ): TransactionDraft {
        val borrowed = direction == LoanDirection.BORROWED
        return TransactionDraft(
            amount = amount,
            type = if (borrowed) TransactionType.EXPENSE else TransactionType.INCOME,
            categoryId = if (borrowed) REPAYMENT_CATEGORY else RETURNED_CATEGORY,
            accountId = accountId,
            date = timestampForDay(day),
            note = note ?: if (borrowed) "Repaid $personName" else "$personName repaid you",
            paymentMethod = paymentMethod,
            source = TransactionSource(
                type = if (borrowed) {
                    TransactionSourceType.BORROW_REPAYMENT
                } else {
                    TransactionSourceType.LEND_REPAYMENT
                },
                referenceId = recordId,
                paymentId = repaymentId
            )
        )
    }

    private companion object {
        const val BORROWED_CATEGORY = "borrowed"
        const val LENT_CATEGORY = "lending"
        const val REPAYMENT_CATEGORY = "loan_repayment"
        const val RETURNED_CATEGORY = "loan_returned"
    }
}
