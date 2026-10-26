package com.monebie.app.domain.repository

import com.monebie.app.domain.model.Currency
import kotlinx.coroutines.flow.Flow

interface CurrencyRepository {
    suspend fun insertCurrency(code: String, symbol: String, name: String, flag: String, popular: Boolean)
    fun getAllCurrencies(): Flow<List<Currency>>
}
