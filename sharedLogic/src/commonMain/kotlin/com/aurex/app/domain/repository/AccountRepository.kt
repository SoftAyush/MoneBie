package com.aurex.app.domain.repository

import com.aurex.app.core.Money
import com.aurex.app.domain.model.Account
import com.aurex.app.domain.model.AccountType
import kotlinx.coroutines.flow.Flow

interface AccountRepository {

    fun observeAccounts(): Flow<List<Account>>

    fun observeActiveAccounts(): Flow<List<Account>>

    fun observeActiveAccountCount(): Flow<Long>

    suspend fun getAccount(id: Long): Account?

    /** Returns the id of the created account so callers can post an opening balance against it. */
    suspend fun createAccount(
        name: String,
        type: AccountType,
        openingBalance: Money,
        currencyCode: String
    ): Long

    suspend fun renameAccount(id: Long, name: String, type: AccountType)

    /**
     * Applies a signed delta to a balance.
     *
     * Every module posts balance changes through this one entry point, which is
     * what keeps account balances consistent with transaction, debt-payment and
     * EMI-payment history.
     */
    suspend fun adjustBalance(id: Long, delta: Money)

    suspend fun archiveAccount(id: Long)
}
