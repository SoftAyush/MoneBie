package com.aurex.app.domain.repository

import com.aurex.app.domain.model.Currency
import kotlinx.coroutines.flow.Flow

interface CurrencyRepository {
    suspend fun insertCurrency(code: String, symbol: String, name: String, flag: String, popular: Boolean)
    fun getAllCurrencies(): Flow<List<Currency>>
}
