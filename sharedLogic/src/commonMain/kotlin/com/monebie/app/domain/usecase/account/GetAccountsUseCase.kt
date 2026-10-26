package com.monebie.app.domain.usecase.account

import com.monebie.app.domain.model.Account
import com.monebie.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

class GetAccountsUseCase(private val accountRepository: AccountRepository) {
    operator fun invoke(): Flow<List<Account>> = accountRepository.observeActiveAccounts()
}
