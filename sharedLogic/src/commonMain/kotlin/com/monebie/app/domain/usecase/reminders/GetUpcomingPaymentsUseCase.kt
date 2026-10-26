package com.monebie.app.domain.usecase.reminders

import com.monebie.app.core.Money
import com.monebie.app.core.getCurrentDay
import com.monebie.app.core.totalOf
import com.monebie.app.domain.calculation.FinancialCalculator
import com.monebie.app.domain.model.BorrowLendRecord
import com.monebie.app.domain.model.Debt
import com.monebie.app.domain.model.DueBucket
import com.monebie.app.domain.model.Emi
import com.monebie.app.domain.model.UpcomingPayment
import com.monebie.app.domain.repository.BorrowLendRepository
import com.monebie.app.domain.repository.DebtRepository
import com.monebie.app.domain.repository.EmiRepository
import com.monebie.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Everything with a due date, grouped by how soon it needs attention.
 *
 * The reminder centre: one place that answers "what needs paying, and what
 * have I already missed", drawn from debts, EMIs and person-to-person loans at
 * once rather than three separate screens.
 */
data class ReminderBoard(
    val isLoading: Boolean = true,
    val currencySymbol: String = "$",
    val buckets: Map<DueBucket, List<UpcomingPayment>> = emptyMap(),
    /** Money going out that is already late. */
    val overdueTotal: Money = Money.ZERO,
    /** Money going out within the next seven days. */
    val dueThisWeekTotal: Money = Money.ZERO,
    val overdueCount: Int = 0
) {
    val isEmpty: Boolean get() = !isLoading && buckets.isEmpty()

    val hasOverdue: Boolean get() = overdueCount > 0
}

class GetUpcomingPaymentsUseCase(
    private val debtRepository: DebtRepository,
    private val emiRepository: EmiRepository,
    private val borrowLendRepository: BorrowLendRepository,
    private val userRepository: UserRepository,
    private val todayProvider: () -> String = ::getCurrentDay
) {

    private data class Obligations(
        val debts: List<Debt>,
        val emis: List<Emi>,
        val records: List<BorrowLendRecord>
    )

    operator fun invoke(): Flow<ReminderBoard> {
        val obligations = combine(
            debtRepository.observeDebts(),
            emiRepository.observeEmis(),
            borrowLendRepository.observeRecords()
        ) { debts, emis, records -> Obligations(debts, emis, records) }

        return combine(
            obligations,
            userRepository.getSelectedCurrencySymbol()
        ) { obligation, symbol ->
            val today = todayProvider()

            val payments = FinancialCalculator.upcomingPayments(
                debts = obligation.debts,
                emis = obligation.emis,
                records = obligation.records,
                today = today
            )

            val buckets = FinancialCalculator.bucketByUrgency(payments)

            // Totals cover money the user has to find. What someone owes *them*
            // is listed but not summed here - it is not an outgoing, and adding
            // it would make the amount to budget for look larger than it is.
            val outgoing = payments.filterNot { it.isReceivable }

            ReminderBoard(
                isLoading = false,
                currencySymbol = symbol ?: DEFAULT_CURRENCY_SYMBOL,
                buckets = buckets,
                overdueTotal = outgoing.filter { it.isOverdue }.totalOf { it.amount },
                dueThisWeekTotal = outgoing
                    .filter { !it.isOverdue && it.daysUntilDue <= DAYS_IN_WEEK }
                    .totalOf { it.amount },
                overdueCount = payments.count { it.isOverdue }
            )
        }
    }

    private companion object {
        const val DEFAULT_CURRENCY_SYMBOL = "$"
        const val DAYS_IN_WEEK = 7L
    }
}
