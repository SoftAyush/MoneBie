package com.aurex.app.domain.usecase.account

import com.aurex.app.domain.model.Account
import com.aurex.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

class GetAccountsUseCase(private val accountRepository: AccountRepository) {
    operator fun invoke(): Flow<List<Account>> = accountRepository.observeActiveAccounts()
}
