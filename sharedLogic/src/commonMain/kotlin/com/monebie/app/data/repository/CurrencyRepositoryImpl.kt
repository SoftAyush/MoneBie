package com.aurex.app.data.repository

import com.aurex.app.data.database.DatabaseHelper
import com.aurex.app.data.mapper.toDomain
import com.aurex.app.domain.model.Currency
import com.aurex.app.domain.repository.CurrencyRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CurrencyRepositoryImpl(
    private val dbHelper: DatabaseHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : CurrencyRepository {

    override suspend fun insertCurrency(
        code: String,
        symbol: String,
        name: String,
        flag: String,
        popular: Boolean
    ) {
        withContext(ioDispatcher) {
            dbHelper.currencyQueries.insertCurrency(
                code    = code,
                symbol  = symbol,
                name    = name,
                flag    = flag,
                popular = if (popular) 1L else 0L  // SQLite boolean as INTEGER
            )
        }
    }

    override fun getAllCurrencies(): Flow<List<Currency>> {
        return dbHelper.currencyQueries.selectAllCurrencies()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toDomain() } }
    }
}
