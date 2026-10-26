package com.monebie.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.monebie.app.core.getCurrentDateString
import com.monebie.app.core.timestampForDay
import com.monebie.app.data.database.DatabaseHelper
import com.monebie.app.data.mapper.toDomain
import com.monebie.app.domain.calculation.RecurringSchedule
import com.monebie.app.domain.model.RecurringPostingResult
import com.monebie.app.domain.model.RecurringTransaction
import com.monebie.app.domain.model.RecurringTransactionDraft
import com.monebie.app.domain.model.TransactionDraft
import com.monebie.app.domain.model.TransactionSource
import com.monebie.app.domain.model.TransactionSourceType
import com.monebie.app.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RecurringTransactionRepositoryImpl(
    private val dbHelper: DatabaseHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : RecurringTransactionRepository {

    private val queries get() = dbHelper.recurringQueries
    private val ledger = LedgerWriter(dbHelper)

    override fun observeRecurring(): Flow<List<RecurringTransaction>> =
        queries.selectAllRecurring()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeRecurring(id: Long): Flow<RecurringTransaction?> =
        queries.getRecurringById(id)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }

    override suspend fun getRecurring(id: Long): RecurringTransaction? = withContext(ioDispatcher) {
        queries.getRecurringById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun createRecurring(draft: RecurringTransactionDraft): Long =
        withContext(ioDispatcher) {
            queries.transactionWithResult {
                queries.insertRecurring(
                    name = draft.name.trim(),
                    amount_minor = draft.amount.minorUnits,
                    type = draft.type.name,
                    category_id = draft.categoryId,
                    account_id = draft.accountId,
                    transfer_account_id = draft.transferAccountId,
                    payment_method = draft.paymentMethod.name,
                    frequency = draft.frequency.name,
                    start_date = draft.startDate,
                    next_due_date = draft.startDate,
                    end_date = draft.endDate,
                    note = draft.note,
                    created_at = getCurrentDateString()
                )
                queries.lastInsertedRecurringId().executeAsOne()
            }
        }

    override suspend fun updateRecurring(id: Long, draft: RecurringTransactionDraft) {
        withContext(ioDispatcher) {
            dbHelper.database.transaction {
                val existing = queries.getRecurringById(id).executeAsOneOrNull() ?: return@transaction
                // A changed start date restarts the schedule from there; an
                // unchanged one keeps its place so editing the amount of a
                // schedule three months in does not re-post those months.
                val nextDue = if (existing.start_date == draft.startDate) {
                    existing.next_due_date
                } else {
                    draft.startDate
                }
                queries.updateRecurring(
                    name = draft.name.trim(),
                    amountMinor = draft.amount.minorUnits,
                    type = draft.type.name,
                    categoryId = draft.categoryId,
                    accountId = draft.accountId,
                    transferAccountId = draft.transferAccountId,
                    paymentMethod = draft.paymentMethod.name,
                    frequency = draft.frequency.name,
                    startDate = draft.startDate,
                    nextDueDate = nextDue,
                    endDate = draft.endDate,
                    note = draft.note,
                    id = id
                )
            }
        }
    }

    override suspend fun setPaused(id: Long, paused: Boolean) {
        withContext(ioDispatcher) {
            queries.setRecurringPaused(paused = if (paused) 1L else 0L, id = id)
        }
    }

    override suspend fun deleteRecurring(id: Long) {
        withContext(ioDispatcher) { queries.deleteRecurring(id) }
    }

    override suspend fun postDue(today: String): RecurringPostingResult = withContext(ioDispatcher) {
        dbHelper.database.transactionWithResult {
            var posted = 0
            val names = mutableListOf<String>()

            // Read inside the transaction, so two passes racing - the launch
            // check and the daily worker - cannot both see the same schedule as
            // due and post it twice.
            queries.selectDueRecurring(today).executeAsList().forEach { row ->
                val schedule = row.toDomain()
                val occurrences = RecurringSchedule.occurrencesDue(
                    nextDue = schedule.nextDueDate,
                    today = today,
                    frequency = schedule.frequency,
                    anchorDay = RecurringSchedule.dayOfMonth(schedule.startDate),
                    endDate = schedule.endDate
                )
                // Empty only for a schedule whose next date is already past
                // its end: nothing to post, and hasEnded reports it as done.
                if (occurrences.isEmpty()) return@forEach

                occurrences.forEach { day -> ledger.post(schedule.toDraft(day)) }
                posted += occurrences.size
                names += schedule.name

                val anchor = RecurringSchedule.dayOfMonth(schedule.startDate)
                queries.setRecurringNextDue(
                    nextDueDate = RecurringSchedule.next(occurrences.last(), schedule.frequency, anchor),
                    id = schedule.id
                )
            }

            RecurringPostingResult(postedCount = posted, postedNames = names)
        }
    }

    override suspend fun postNow(id: Long, today: String): Long? = withContext(ioDispatcher) {
        dbHelper.database.transactionWithResult {
            val schedule = queries.getRecurringById(id).executeAsOneOrNull()?.toDomain()
                ?: return@transactionWithResult null
            // Dated today rather than on the schedule's due date: the user is
            // saying the money moved now, ahead of or behind the plan.
            val transactionId = ledger.post(schedule.toDraft(today))
            advance(schedule)
            transactionId
        }
    }

    override suspend fun skipNext(id: Long) {
        withContext(ioDispatcher) {
            dbHelper.database.transaction {
                val schedule = queries.getRecurringById(id).executeAsOneOrNull()?.toDomain()
                    ?: return@transaction
                advance(schedule)
            }
        }
    }

    /** Moves a schedule one occurrence forward. Call inside a transaction. */
    private fun advance(schedule: RecurringTransaction) {
        val anchor = RecurringSchedule.dayOfMonth(schedule.startDate)
        queries.setRecurringNextDue(
            nextDueDate = RecurringSchedule.next(schedule.nextDueDate, schedule.frequency, anchor),
            id = schedule.id
        )
    }

    private fun RecurringTransaction.toDraft(day: String) = TransactionDraft(
        amount = amount,
        type = type,
        categoryId = categoryId,
        accountId = accountId,
        date = timestampForDay(day),
        note = note?.takeIf { it.isNotBlank() } ?: name,
        paymentMethod = paymentMethod,
        transferAccountId = transferAccountId,
        source = TransactionSource(
            type = TransactionSourceType.RECURRING,
            referenceId = id
        )
    )
}
