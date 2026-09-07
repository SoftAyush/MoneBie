package com.aurex.app

import com.aurex.app.core.Money
import com.aurex.app.domain.calculation.FinancialCalculator
import com.aurex.app.domain.calculation.PositionComposer
import com.aurex.app.domain.model.Account
import com.aurex.app.domain.model.AccountType
import com.aurex.app.domain.model.BorrowLendRecord
import com.aurex.app.domain.model.Debt
import com.aurex.app.domain.model.Emi
import com.aurex.app.domain.model.LoanDirection
import com.aurex.app.domain.model.PaymentFrequency
import com.aurex.app.domain.model.PositionGroupKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the balance sheet: that every recorded amount lands on exactly one
 * side of it, and that the itemised groups add up to the totals shown above
 * them.
 */
class FinancialPositionTest {

    private val today = "2026-09-04"

    private fun account(
        id: Long,
        type: AccountType,
        balanceMajor: Long,
        archived: Boolean = false
    ) = Account(
        id = id,
        name = "Account $id",
        type = type,
        balance = Money.fromMajor(balanceMajor),
        currencyCode = "USD",
        isArchived = archived,
        createdAt = "2026-01-01 00:00:00"
    )

    private fun debt(id: Long, original: Long, paid: Long = 0L, dueDate: String? = null) = Debt(
        id = id,
        name = "Debt $id",
        lender = "Lender",
        originalAmount = Money.fromMajor(original),
        amountPaid = Money.fromMajor(paid),
        startDate = "2026-01-01",
        dueDate = dueDate,
        paymentFrequency = PaymentFrequency.MONTHLY,
        installmentAmount = Money.fromMajor(100L)
    )

    private fun emi(id: Long, totalPayable: Long, paid: Long, installmentsPaid: Int) = Emi(
        id = id,
        name = "EMI $id",
        provider = "Provider",
        principalAmount = Money.fromMajor(totalPayable),
        totalPayableAmount = Money.fromMajor(totalPayable),
        monthlyAmount = Money.fromMajor(100L),
        startDate = "2026-01-01",
        tenureYears = 5.0,
        installmentsPaid = installmentsPaid,
        amountPaid = Money.fromMajor(paid)
    )

    private fun loan(id: Long, direction: LoanDirection, principal: Long) = BorrowLendRecord(
        id = id,
        personName = "Person $id",
        direction = direction,
        principalAmount = Money.fromMajor(principal),
        date = "2026-08-01"
    )

    // -------------------------------------------------------------------------
    // Nothing falls through the gap
    // -------------------------------------------------------------------------

    @Test
    fun anOverpaidCardIsAnAssetRatherThanVanishing() {
        // A credit card in credit is money the user can draw on. It used to be
        // neither an asset nor a liability and disappeared from the net figure.
        val position = FinancialCalculator.netPosition(
            accounts = listOf(account(1, AccountType.CREDIT_CARD, balanceMajor = 250)),
            debts = emptyList(),
            emis = emptyList(),
            records = emptyList()
        )

        assertEquals(Money.fromMajor(250L), position.assets.otherAssets)
        assertEquals(Money.fromMajor(250L), position.net)
        assertTrue(position.liabilities.total.isZero)
    }

    @Test
    fun aCardInDebtIsALiabilityAndNotAnAsset() {
        val position = FinancialCalculator.netPosition(
            accounts = listOf(account(1, AccountType.CREDIT_CARD, balanceMajor = -400)),
            debts = emptyList(),
            emis = emptyList(),
            records = emptyList()
        )

        assertTrue(position.assets.total.isZero)
        assertEquals(Money.fromMajor(400L), position.liabilities.creditCardOutstanding)
        assertEquals(Money.fromMajor(-400L), position.net)
    }

    @Test
    fun creditBalancesStayOutOfTheLiquidFigure() {
        // Liquid must keep matching the dashboard's "Total balance", which is
        // computed from asset-type accounts alone.
        val accounts = listOf(
            account(1, AccountType.BANK, balanceMajor = 1_000),
            account(2, AccountType.CREDIT_CARD, balanceMajor = 250)
        )
        val position = FinancialCalculator.netPosition(accounts, emptyList(), emptyList(), emptyList())

        assertEquals(FinancialCalculator.totalBalance(accounts), position.assets.liquid)
        assertEquals(Money.fromMajor(1_250L), position.assets.total)
    }

    @Test
    fun archivedAccountsAreLeftOutOfBothSides() {
        val position = FinancialCalculator.netPosition(
            accounts = listOf(
                account(1, AccountType.BANK, balanceMajor = 1_000),
                account(2, AccountType.BANK, balanceMajor = 5_000, archived = true),
                account(3, AccountType.CREDIT_CARD, balanceMajor = -900, archived = true)
            ),
            debts = emptyList(),
            emis = emptyList(),
            records = emptyList()
        )

        assertEquals(Money.fromMajor(1_000L), position.assets.total)
        assertTrue(position.liabilities.total.isZero)
    }

    // -------------------------------------------------------------------------
    // The parts add up to the whole
    // -------------------------------------------------------------------------

    @Test
    fun groupTotalsReconcileWithTheHeadlineTotals() {
        val overview = PositionComposer.compose(
            accounts = listOf(
                account(1, AccountType.CASH, 200),
                account(2, AccountType.BANK, 3_000),
                account(3, AccountType.DIGITAL_WALLET, 150),
                account(4, AccountType.CREDIT_CARD, -600)
            ),
            debts = listOf(debt(1, original = 5_000, paid = 1_000)),
            emis = listOf(emi(1, totalPayable = 6_000, paid = 2_000, installmentsPaid = 20)),
            records = listOf(
                loan(1, LoanDirection.LENT, 800),
                loan(2, LoanDirection.BORROWED, 400)
            ),
            currencySymbol = "$",
            today = today
        )

        // Every itemised group must sum back to the figure printed above it,
        // or the breakdown is telling a different story from the headline.
        assertEquals(overview.totalAssets, PositionComposer.sumOf(overview.assetGroups))
        assertEquals(overview.totalLiabilities, PositionComposer.sumOf(overview.liabilityGroups))

        // 200 + 3,000 + 150 + 800 lent = 4,150
        assertEquals(Money.fromMajor(4_150L), overview.totalAssets)
        // 4,000 debt + 4,000 EMI + 400 borrowed + 600 card = 9,000
        assertEquals(Money.fromMajor(9_000L), overview.totalLiabilities)
        assertEquals(Money.fromMajor(-4_850L), overview.net)
        assertFalse(overview.isPositive)
    }

    @Test
    fun emptyGroupsAreDroppedRatherThanShownAsZero() {
        val overview = PositionComposer.compose(
            accounts = listOf(account(1, AccountType.BANK, 1_000)),
            debts = emptyList(),
            emis = emptyList(),
            records = emptyList(),
            currencySymbol = "$",
            today = today
        )

        assertEquals(listOf(PositionGroupKey.BANK), overview.assetGroups.map { it.key })
        assertTrue(overview.liabilityGroups.isEmpty())
    }

    @Test
    fun settledRecordsDoNotAppearInTheBreakdown() {
        val overview = PositionComposer.compose(
            accounts = emptyList(),
            debts = listOf(debt(1, original = 1_000, paid = 1_000)),
            emis = emptyList(),
            records = emptyList(),
            currencySymbol = "$",
            today = today
        )

        assertTrue(overview.liabilityGroups.isEmpty())
        assertFalse(overview.hasData)
    }

    @Test
    fun groupLinesNameTheRecordsBehindTheTotal() {
        val overview = PositionComposer.compose(
            accounts = emptyList(),
            debts = listOf(
                debt(1, original = 1_000),
                debt(2, original = 4_000)
            ),
            emis = emptyList(),
            records = emptyList(),
            currencySymbol = "$",
            today = today
        )

        val debts = overview.liabilityGroups.single { it.key == PositionGroupKey.DEBTS }
        assertEquals(2, debts.lines.size)
        // Largest first, so the biggest contributor is visible without scrolling.
        assertEquals(Money.fromMajor(4_000L), debts.lines.first().amount)
        assertEquals(debts.total, Money(debts.lines.sumOf { it.amount.minorUnits }))
    }

    // -------------------------------------------------------------------------
    // Monthly obligations
    // -------------------------------------------------------------------------

    @Test
    fun obligationsCountOnlyWhatFallsDueThisMonth() {
        val overview = PositionComposer.compose(
            accounts = emptyList(),
            // Due 2026-09-15, inside the month.
            debts = listOf(debt(1, original = 5_000, dueDate = "2026-09-15")),
            // Started 2026-01-01 with 7 paid, so installment 8 is due 2026-09-01.
            emis = listOf(emi(1, totalPayable = 6_000, paid = 700, installmentsPaid = 7)),
            records = emptyList(),
            currencySymbol = "$",
            today = today
        )

        // 100 debt installment + 100 EMI installment.
        assertEquals(Money.fromMajor(200L), overview.obligationsThisMonth)
    }

    @Test
    fun aRatioOfZeroMeansNothingIsOwed() {
        val overview = PositionComposer.compose(
            accounts = listOf(account(1, AccountType.BANK, 2_000)),
            debts = emptyList(),
            emis = emptyList(),
            records = emptyList(),
            currencySymbol = "$",
            today = today
        )

        assertEquals(0f, overview.liabilityRatio)
        assertTrue(overview.isPositive)
        assertEquals(1f, overview.assetShareOfTotal)
    }
}
