package com.monebie.app.domain.usecase.recurring

import com.monebie.app.core.getCurrentDay
import com.monebie.app.domain.model.PaymentFrequency
import com.monebie.app.domain.model.RecurringPostingResult
import com.monebie.app.domain.model.RecurringTransaction
import com.monebie.app.domain.model.RecurringTransactionDraft
import com.monebie.app.domain.model.TransactionType
import com.monebie.app.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow

class GetRecurringTransactionsUseCase(private val repository: RecurringTransactionRepository) {
    operator fun invoke(): Flow<List<RecurringTransaction>> = repository.observeRecurring()
}

class GetRecurringTransactionUseCase(private val repository: RecurringTransactionRepository) {
    operator fun invoke(id: Long): Flow<RecurringTransaction?> = repository.observeRecurring(id)
    suspend fun once(id: Long): RecurringTransaction? = repository.getRecurring(id)
}

/** Creates or replaces a schedule after checking it can actually run. */
class SaveRecurringTransactionUseCase(private val repository: RecurringTransactionRepository) {

    sealed interface Result {
        data class Saved(val id: Long) : Result
        data class Invalid(val message: String) : Result
    }

    suspend operator fun invoke(existingId: Long?, draft: RecurringTransactionDraft): Result {
        if (draft.name.isBlank()) return Result.Invalid("Give this a name")
        if (!draft.amount.isPositive) return Result.Invalid("Amount must be greater than zero")
        if (draft.frequency == PaymentFrequency.ONE_TIME) {
            return Result.Invalid("A recurring transaction needs a repeat cycle")
        }
        if (draft.type == TransactionType.TRANSFER) {
            if (draft.transferAccountId == null) return Result.Invalid("Choose an account to move it to")
            if (draft.transferAccountId == draft.accountId) {
                return Result.Invalid("A transfer needs two different accounts")
            }
        }
        val end = draft.endDate
        if (end != null && end < draft.startDate) {
            return Result.Invalid("The end date is before the start date")
        }

        if (existingId == null) return Result.Saved(repository.createRecurring(draft))
        repository.updateRecurring(existingId, draft)
        return Result.Saved(existingId)
    }
}

class DeleteRecurringTransactionUseCase(private val repository: RecurringTransactionRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteRecurring(id)
}

class SetRecurringPausedUseCase(private val repository: RecurringTransactionRepository) {
    suspend operator fun invoke(id: Long, paused: Boolean) = repository.setPaused(id, paused)
}

/**
 * Posts everything that has fallen due.
 *
 * Called on launch, from the daily worker and when the schedule list opens;
 * safe to call as often as wanted because a schedule that is not due does
 * nothing.
 */
class PostDueRecurringUseCase(
    private val repository: RecurringTransactionRepository,
    private val todayProvider: () -> String = ::getCurrentDay
) {
    suspend operator fun invoke(): RecurringPostingResult = repository.postDue(todayProvider())
}

/** Posts one schedule's next occurrence right now, dated today. */
class PostRecurringNowUseCase(
    private val repository: RecurringTransactionRepository,
    private val todayProvider: () -> String = ::getCurrentDay
) {
    suspend operator fun invoke(id: Long): Long? = repository.postNow(id, todayProvider())
}

/** Moves a schedule past its next occurrence without recording it. */
class SkipRecurringOccurrenceUseCase(private val repository: RecurringTransactionRepository) {
    suspend operator fun invoke(id: Long) = repository.skipNext(id)
}
