package com.monebie.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.monebie.app.core.Money
import com.monebie.app.core.getCurrentDateString
import com.monebie.app.core.timestampForDay
import com.monebie.app.data.database.DatabaseHelper
import com.monebie.app.data.mapper.toDomain
import com.monebie.app.domain.model.Emi
import com.monebie.app.domain.model.EmiDraft
import com.monebie.app.domain.model.EmiPayment
import com.monebie.app.domain.model.PaymentMethod
import com.monebie.app.domain.model.TransactionCategories
import com.monebie.app.domain.model.TransactionDraft
import com.monebie.app.domain.model.TransactionSource
import com.monebie.app.domain.model.TransactionSourceType
import com.monebie.app.domain.model.TransactionType
import com.monebie.app.domain.repository.EmiRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class EmiRepositoryImpl(
    private val dbHelper: DatabaseHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : EmiRepository {

    private val queries get() = dbHelper.emiQueries
    private val ledger = LedgerWriter(dbHelper)

    override fun observeEmis(): Flow<List<Emi>> =
        queries.selectAllEmis()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeEmi(id: Long): Flow<Emi?> =
        queries.selectEmiById(id)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }

    override fun observePayments(emiId: Long): Flow<List<EmiPayment>> =
        queries.selectPaymentsForEmi(emiId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getEmi(id: Long): Emi? = withContext(ioDispatcher) {
        queries.selectEmiById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun createEmi(draft: EmiDraft): Long = withContext(ioDispatcher) {
        queries.transactionWithResult {
            queries.insertEmi(
                name = draft.name.trim(),
                provider = draft.provider.trim(),
                principal_amount_minor = draft.principalAmount.minorUnits,
                total_payable_amount_minor = draft.totalPayableAmount.minorUnits,
                monthly_amount_minor = draft.monthlyAmount.minorUnits,
                interest_rate = draft.interestRate,
                start_date = draft.startDate,
                tenure_years = draft.tenureYears,
                payment_account_id = draft.paymentAccountId,
                notes = draft.notes,
                created_at = getCurrentDateString()
            )
            queries.lastInsertedEmiId().executeAsOne()
        }
    }

    override suspend fun updateEmi(id: Long, draft: EmiDraft) {
        withContext(ioDispatcher) {
            queries.updateEmi(
                name = draft.name.trim(),
                provider = draft.provider.trim(),
                principalAmountMinor = draft.principalAmount.minorUnits,
                totalPayableAmountMinor = draft.totalPayableAmount.minorUnits,
                monthlyAmountMinor = draft.monthlyAmount.minorUnits,
                interestRate = draft.interestRate,
                startDate = draft.startDate,
                tenureYears = draft.tenureYears,
                paymentAccountId = draft.paymentAccountId,
                notes = draft.notes,
                id = id
            )
        }
    }

    override suspend fun deleteEmi(id: Long) {
        withContext(ioDispatcher) {
            dbHelper.database.transaction {
                // Installment records go; the transactions they produced stay,
                // for the same reason as debts - that money really did leave the
                // account, and reversing it would corrupt every balance.
                queries.deletePaymentsForEmi(id)
                queries.deleteEmi(id)
            }
        }
    }

    override suspend fun markInstallmentPaid(
        emiId: Long,
        amount: Money,
        paidDay: String,
        accountId: Long?,
        paymentMethod: PaymentMethod
    ): Long? = withContext(ioDispatcher) {
        dbHelper.database.transactionWithResult {
            val emi = queries.selectEmiById(emiId).executeAsOneOrNull()?.toDomain()
                ?: return@transactionWithResult null

            /*
             * The installment number and its due date come from the schedule,
             * read inside this transaction. The caller cannot pass them in, so
             * two taps in quick succession cannot both claim installment 15.
             */
            val installmentNumber = emi.nextInstallmentNumber
                ?: return@transactionWithResult null
            val dueDate = emi.nextDueDate ?: return@transactionWithResult null

            queries.insertEmiPayment(
                emi_id = emiId,
                installment_number = installmentNumber.toLong(),
                amount_minor = amount.minorUnits,
                due_date = dueDate,
                paid_date = paidDay,
                account_id = accountId,
                transaction_id = null
            )
            val paymentId = queries.lastInsertedEmiPaymentId().executeAsOne()

            if (accountId != null) {
                val transactionId = ledger.post(
                    TransactionDraft(
                        amount = amount,
                        type = TransactionType.EXPENSE,
                        categoryId = EMI_PAYMENT_CATEGORY,
                        accountId = accountId,
                        date = timestampForDay(paidDay),
                        note = emi.name + " - installment " + installmentNumber +
                            " of " + emi.totalInstallments,
                        paymentMethod = paymentMethod,
                        source = TransactionSource(
                            type = TransactionSourceType.EMI_PAYMENT,
                            referenceId = emiId,
                            paymentId = paymentId
                        )
                    )
                )
                queries.linkEmiPaymentTransaction(transactionId = transactionId, id = paymentId)
            }

            paymentId
        }
    }

    override suspend fun undoLastInstallment(emiId: Long) {
        withContext(ioDispatcher) {
            dbHelper.database.transaction {
                val latest = queries.latestPaymentForEmi(emiId)
                    .executeAsOneOrNull()?.toDomain() ?: return@transaction

                // Undoing says the installment never happened, so the money goes
                // back to the account it was taken from.
                latest.transactionId?.let { ledger.reverse(it) }
                queries.deleteEmiPayment(latest.id)
            }
        }
    }

    private companion object {
        val EMI_PAYMENT_CATEGORY = TransactionCategories.find("emi_payment").id
    }
}
