package com.aurex.app.domain.repository

import com.aurex.app.domain.model.RecurringPostingResult
import com.aurex.app.domain.model.RecurringTransaction
import com.aurex.app.domain.model.RecurringTransactionDraft
import kotlinx.coroutines.flow.Flow

/**
 * Recurring schedules and the posting of their occurrences.
 *
 * Posting lives here rather than in a use case because it has to write the
 * ledger row and advance the schedule's next due date inside one database
 * transaction. Done as two calls, a crash between them would either post the
 * same occurrence again tomorrow or skip it forever.
 */
interface RecurringTransactionRepository {

    fun observeRecurring(): Flow<List<RecurringTransaction>>

    fun observeRecurring(id: Long): Flow<RecurringTransaction?>

    suspend fun getRecurring(id: Long): RecurringTransaction?

    /** Returns the new id. The next due date starts at the draft's start date. */
    suspend fun createRecurring(draft: RecurringTransactionDraft): Long

    /**
     * Replaces the template.
     *
     * Occurrences already posted are left as they were - they describe money
     * that moved at the amount it moved at. The next due date is moved to the
     * new start date only when the start date itself changed.
     */
    suspend fun updateRecurring(id: Long, draft: RecurringTransactionDraft)

    suspend fun setPaused(id: Long, paused: Boolean)

    /** Removes the template. Posted occurrences stay in the ledger. */
    suspend fun deleteRecurring(id: Long)

    /**
     * Posts every occurrence due on or before [today], across every active
     * schedule, and advances each schedule past what was posted.
     */
    suspend fun postDue(today: String): RecurringPostingResult

    /** Posts the next occurrence of one schedule now, regardless of its date. */
    suspend fun postNow(id: Long, today: String): Long?

    /** Advances one schedule past its next occurrence without posting it. */
    suspend fun skipNext(id: Long)
}
