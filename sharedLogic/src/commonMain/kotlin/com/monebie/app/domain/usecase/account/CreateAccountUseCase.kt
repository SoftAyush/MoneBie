package com.monebie.app.domain.usecase.account

import com.monebie.app.core.Money
import com.monebie.app.domain.model.AccountType
import com.monebie.app.domain.repository.AccountRepository

/**
 * Creates a financial account.
 *
 * The opening balance is written as the account balance rather than posted as
 * an income transaction: it is a starting position, not earnings, and counting
 * it as income would distort every income total from day one.
 */
class CreateAccountUseCase(private val accountRepository: AccountRepository) {

    suspend operator fun invoke(
        name: String,
        type: AccountType,
        openingBalance: Money,
        currencyCode: String
    ): Long = accountRepository.createAccount(
        name = name.trim(),
        type = type,
        openingBalance = openingBalance,
        currencyCode = currencyCode
    )
}
