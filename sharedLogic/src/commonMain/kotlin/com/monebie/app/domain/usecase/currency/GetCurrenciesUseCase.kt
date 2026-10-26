package com.monebie.app.domain.usecase.currency

import com.monebie.app.domain.model.Currency
import com.monebie.app.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.Flow

class GetCurrenciesUseCase(private val currencyRepository: CurrencyRepository) {
    operator fun invoke(): Flow<List<Currency>> {
        return currencyRepository.getAllCurrencies()
    }
}
