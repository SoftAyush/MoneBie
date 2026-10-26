package com.aurex.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.aurex.app.core.Money
import com.aurex.app.core.getCurrentDateString
import com.aurex.app.core.timestampForDay
import com.aurex.app.data.database.DatabaseHelper
import com.aurex.app.data.mapper.toDomain
import com.aurex.app.domain.model.Debt
import com.aurex.app.domain.model.DebtDraft
import com.aurex.app.domain.model.DebtPayment
import com.aurex.app.domain.model.PaymentMethod
import com.aurex.app.domain.model.TransactionCategories
import com.aurex.app.domain.model.TransactionDraft
import com.aurex.app.domain.model.TransactionSource
import com.aurex.app.domain.model.TransactionSourceType
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.domain.repository.DebtRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DebtRepositoryImpl(
    private val dbHelper: DatabaseHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : DebtRepository {

    private val queries get() = dbHelper.debtQueries
    private val ledger = LedgerWriter(dbHelper)

    override fun observeDebts(): Flow<List<Debt>> =
        queries.selectAllDebts()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeDebt(id: Long): Flow<Debt?> =
        queries.selectDebtById(id)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }

    override fun observePayments(debtId: Long): Flow<List<DebtPayment>> =
        queries.selectPaymentsForDebt(debtId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getDebt(id: Long): Debt? = withContext(ioDispatcher) {
        queries.selectDebtById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun createDebt(draft: DebtDraft): Long = withContext(ioDispatcher) {
        queries.transactionWithResult {
            queries.insertDebt(
                name = draft.name.trim(),
                lender = draft.lender.trim(),
                original_amount_minor = draft.originalAmount.minorUnits,
                interest_rate = draft.interestRate,
                start_date = draft.startDate,
                due_date = draft.dueDate,
                payment_frequency = draft.paymentFrequency.name,
                installment_amount_minor = draft.installmentAmount?.minorUnits,
                notes = draft.notes,
                default_account_id = draft.defaultAccountId,
                created_at = getCurrentDateString()
            )
            queries.lastInsertedDebtId().executeAsOne()
        }
    }

    override suspend fun updateDebt(id: Long, draft: DebtDraft) {
        withContext(ioDispatcher) {
            queries.updateDebt(
                name = draft.name.trim(),
                lender = draft.lender.trim(),
                originalAmountMinor = draft.originalAmount.minorUnits,
                interestRate = draft.interestRate,
                startDate = draft.startDate,
                dueDate = draft.dueDate,
                paymentFrequency = draft.paymentFrequency.name,
                installmentAmountMinor = draft.installmentAmount?.minorUnits,
                notes = draft.notes,
                defaultAccountId = draft.defaultAccountId,
                id = id
            )
        }
    }

    override suspend fun deleteDebt(id: Long) {
        withContext(ioDispatcher) {
            dbHelper.database.transaction {
                /*
                 * Payment records go; the transactions they produced stay.
                 *
                 * Those transactions represent money that actually left an
                 * account. Reversing them here would credit the user back for
                 * spending that really happened, making every account balance
                 * and every past month's expense total wrong. They keep their
                 * DEBT_PAYMENT source, whose reference simply no longer
                 * resolves - every reader of that field tolerates a miss.
                 */
                queries.deletePaymentsForDebt(id)
                queries.deleteDebt(id)
            }
        }
    }

    override suspend fun recordPayment(
        debtId: Long,
        amount: Money,
        day: String,
        accountId: Long?,
        paymentMethod: PaymentMethod,
        note: String?
    ): Long = withContext(ioDispatcher) {
        dbHelper.database.transactionWithResult {
            val debt = queries.selectDebtById(debtId).executeAsOneOrNull()?.toDomain()

            /*
             * Order matters. The payment row is written first so its id can go
             * into the transaction's source, then the transaction id is written
             * back onto the payment. Both directions of the link exist, and
             * because this all sits in one database transaction there is no
             * moment where a payment exists without its transaction.
             */
            queries.insertDebtPayment(
                debt_id = debtId,
                amount_minor = amount.minorUnits,
                date = day,
                account_id = accountId,
                transaction_id = null,
                note = note
            )
            val paymentId = queries.lastInsertedDebtPaymentId().executeAsOne()

            if (accountId != null) {
                val transactionId = ledger.post(
                    TransactionDraft(
                        amount = amount,
                        type = TransactionType.EXPENSE,
                        categoryId = DEBT_PAYMENT_CATEGORY,
                        accountId = accountId,
                        date = timestampForDay(day),
                        note = note ?: debt?.let { "Payment for " + it.name },
                        paymentMethod = paymentMethod,
                        source = TransactionSource(
                            type = TransactionSourceType.DEBT_PAYMENT,
                            referenceId = debtId,
                            paymentId = paymentId
                        )
                    )
                )
                queries.linkDebtPaymentTransaction(transactionId = transactionId, id = paymentId)
            }

            paymentId
        }
    }

    override suspend fun deletePayment(paymentId: Long) {
        withContext(ioDispatcher) {
            dbHelper.database.transaction {
                val payment = queries.getDebtPaymentById(paymentId)
                    .executeAsOneOrNull()?.toDomain() ?: return@transaction

                // Unlike deleting a whole debt, this says the payment never
                // happened - so the money goes back to the account it came from.
                payment.transactionId?.let { ledger.reverse(it) }
                queries.deleteDebtPayment(paymentId)
            }
        }
    }

    private companion object {
        val DEBT_PAYMENT_CATEGORY = TransactionCategories.find("debt_payment").id
    }
}
