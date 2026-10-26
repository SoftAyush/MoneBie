package com.monebie.app.domain.model

import com.monebie.app.core.Money
import com.monebie.app.core.total

/**
 * Everything the user owns, itemised.
 *
 * Money lent to other people is an asset even though it is not sitting in an
 * account, which is why the net position cannot be read off account balances
 * alone.
 */
data class AssetBreakdown(
    val cash: Money = Money.ZERO,
    val bank: Money = Money.ZERO,
    val wallet: Money = Money.ZERO,
    /**
     * Credit balances on accounts that normally hold a liability.
     *
     * An overpaid credit card is money the user can draw on. Without this
     * bucket it counted as neither an asset nor a liability and simply vanished
     * from the net position.
     */
    val otherAssets: Money = Money.ZERO,
    val moneyLentOut: Money = Money.ZERO
) {
    /**
     * Spendable money sitting in accounts.
     *
     * Deliberately excludes [otherAssets] so this always equals the "Total
     * balance" figure on the dashboard, which is computed from asset-type
     * accounts alone.
     */
    val liquid: Money get() = listOf(cash, bank, wallet).total()

    val total: Money get() = listOf(liquid, otherAssets, moneyLentOut).total()
}

/**
 * Everything the user owes, itemised.
 *
 * Kept separate per source so the dashboard can explain a net position instead
 * of just asserting one.
 */
data class LiabilityBreakdown(
    val debts: Money = Money.ZERO,
    val emiOutstanding: Money = Money.ZERO,
    val moneyBorrowed: Money = Money.ZERO,
    val creditCardOutstanding: Money = Money.ZERO
) {
    val total: Money get() = listOf(debts, emiOutstanding, moneyBorrowed, creditCardOutstanding).total()
}

/** Total Assets - Total Liabilities. */
data class NetPosition(
    val assets: AssetBreakdown = AssetBreakdown(),
    val liabilities: LiabilityBreakdown = LiabilityBreakdown()
) {
    val net: Money get() = assets.total - liabilities.total

    val isPositive: Boolean get() = net.minorUnits >= 0L

    /**
     * Share of assets consumed by liabilities, 0f..1f. Above 1f the user is
     * insolvent on paper, so it is clamped for display and the sign of [net]
     * carries the real story.
     */
    val liabilityRatio: Float get() = liabilities.total.ratioOf(assets.total)
}

/** Income and expense totals over any window - a day, a month, a custom range. */
data class PeriodSummary(
    val income: Money = Money.ZERO,
    val expense: Money = Money.ZERO
) {
    val net: Money get() = income - expense

    val hasActivity: Boolean get() = !income.isZero || !expense.isZero

    /** Expense as a share of income, for the month bar. Zero income means no meaningful ratio. */
    val expenseRatio: Float get() = expense.ratioOf(income)
}

/** One day of expense totals, for the dashboard trend chart. */
data class DailyTotal(
    /** yyyy-MM-dd */
    val day: String,
    val income: Money,
    val expense: Money
)

/** One calendar day of transactions with its own subtotals, for grouped lists. */
data class DayGroup(
    /** yyyy-MM-dd */
    val day: String,
    val transactions: List<Transaction>,
    val summary: PeriodSummary
)
