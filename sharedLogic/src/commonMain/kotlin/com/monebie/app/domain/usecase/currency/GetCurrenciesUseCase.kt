package com.aurex.app.domain.usecase.currency

import com.aurex.app.domain.model.Currency
import com.aurex.app.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.Flow

class GetCurrenciesUseCase(private val currencyRepository: CurrencyRepository) {
    operator fun invoke(): Flow<List<Currency>> {
        return currencyRepository.getAllCurrencies()
    }
}
