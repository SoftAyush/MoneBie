package com.aurex.app.domain.usecase.account

import com.aurex.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Whether account setup has been completed.
 *
 * Backed by a real COUNT so the app shell routes on persisted state instead of
 * a placeholder, and so a user who quits mid-setup returns to the right screen.
 */
class HasAccountUseCase(private val accountRepository: AccountRepository) {
    operator fun invoke(): Flow<Boolean> =
        accountRepository.observeActiveAccountCount().map { it > 0L }
}
