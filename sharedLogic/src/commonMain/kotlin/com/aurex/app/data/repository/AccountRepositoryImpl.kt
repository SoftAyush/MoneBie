package com.aurex.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.aurex.app.core.Money
import com.aurex.app.core.getCurrentDateString
import com.aurex.app.data.database.DatabaseHelper
import com.aurex.app.data.mapper.toDomain
import com.aurex.app.domain.model.Account
import com.aurex.app.domain.model.AccountType
import com.aurex.app.domain.repository.AccountRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AccountRepositoryImpl(
    private val dbHelper: DatabaseHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : AccountRepository {

    override fun observeAccounts(): Flow<List<Account>> =
        dbHelper.accountQueries.selectAllAccounts()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeActiveAccounts(): Flow<List<Account>> =
        dbHelper.accountQueries.selectActiveAccounts()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeActiveAccountCount(): Flow<Long> =
        dbHelper.accountQueries.countActiveAccounts()
            .asFlow()
            .mapToOne(ioDispatcher)

    override suspend fun getAccount(id: Long): Account? = withContext(ioDispatcher) {
        dbHelper.accountQueries.getAccountById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun createAccount(
        name: String,
        type: AccountType,
        openingBalance: Money,
        currencyCode: String
    ): Long = withContext(ioDispatcher) {
        // Insert and id read share a transaction so a concurrent insert cannot
        // make last_insert_rowid() return someone else's row.
        dbHelper.accountQueries.transactionWithResult {
            dbHelper.accountQueries.insertAccount(
                name = name,
                type = type.name,
                balance_minor = openingBalance.minorUnits,
                currency_code = currencyCode,
                created_at = getCurrentDateString()
            )
            dbHelper.accountQueries.lastInsertedAccountId().executeAsOne()
        }
    }

    override suspend fun renameAccount(id: Long, name: String, type: AccountType) {
        withContext(ioDispatcher) {
            dbHelper.accountQueries.updateAccount(name = name, type = type.name, id = id)
        }
    }

    override suspend fun adjustBalance(id: Long, delta: Money) {
        withContext(ioDispatcher) {
            dbHelper.accountQueries.adjustAccountBalance(delta = delta.minorUnits, id = id)
        }
    }

    override suspend fun archiveAccount(id: Long) {
        withContext(ioDispatcher) {
            dbHelper.accountQueries.archiveAccount(id)
        }
    }
}
