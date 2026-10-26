package com.aurex.app.domain.usecase.overview

import com.aurex.app.core.getCurrentDay
import com.aurex.app.domain.calculation.PositionComposer
import com.aurex.app.domain.model.BorrowLendRecord
import com.aurex.app.domain.model.Debt
import com.aurex.app.domain.model.Emi
import com.aurex.app.domain.model.FinancialOverview
import com.aurex.app.domain.repository.AccountRepository
import com.aurex.app.domain.repository.BorrowLendRepository
import com.aurex.app.domain.repository.DebtRepository
import com.aurex.app.domain.repository.EmiRepository
import com.aurex.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Assembles the full financial position from every module.
 *
 * Deliberately does not read transactions: the position is a balance sheet, and
 * every transaction's effect already reached it by moving an account balance.
 * Reading them again would risk counting the same money twice.
 */
class GetFinancialOverviewUseCase(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val debtRepository: DebtRepository,
    private val emiRepository: EmiRepository,
    private val borrowLendRepository: BorrowLendRepository,
    /** Injectable so an overview is reproducible in tests. */
    private val todayProvider: () -> String = ::getCurrentDay
) {

    private data class Obligations(
        val debts: List<Debt>,
        val emis: List<Emi>,
        val records: List<BorrowLendRecord>
    )

    operator fun invoke(): Flow<FinancialOverview> {
        // Grouped because combine tops out at five typed sources.
        val obligations = combine(
            debtRepository.observeDebts(),
            emiRepository.observeEmis(),
            borrowLendRepository.observeRecords()
        ) { debts, emis, records -> Obligations(debts, emis, records) }

        return combine(
            accountRepository.observeActiveAccounts(),
            obligations,
            userRepository.getSelectedCurrencySymbol()
        ) { accounts, obligation, symbol ->
            PositionComposer.compose(
                accounts = accounts,
                debts = obligation.debts,
                emis = obligation.emis,
                records = obligation.records,
                currencySymbol = symbol ?: DEFAULT_CURRENCY_SYMBOL,
                today = todayProvider()
            )
        }
    }

    private companion object {
        const val DEFAULT_CURRENCY_SYMBOL = "$"
    }
}
