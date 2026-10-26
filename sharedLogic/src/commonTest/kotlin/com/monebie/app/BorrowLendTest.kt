package com.monebie.app

import com.monebie.app.core.Money
import com.monebie.app.domain.calculation.FinancialCalculator
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.AccountType
import com.monebie.app.domain.model.BorrowLendRecord
import com.monebie.app.domain.model.LoanDirection
import com.monebie.app.domain.model.LoanStatus
import com.monebie.app.domain.model.Transaction
import com.monebie.app.domain.model.TransactionSource
import com.monebie.app.domain.model.TransactionSourceType
import com.monebie.app.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the two things this module has to get right: which direction money
 * goes, and the fact that moving it does not make the user richer or poorer.
 */
class BorrowLendTest {

    private val today = "2026-09-04"

    private fun record(
        direction: LoanDirection,
        principal: Long,
        settled: Long = 0L,
        dueDate: String? = null
    ) = BorrowLendRecord(
        id = 1L,
        personName = "Test person",
        direction = direction,
        principalAmount = Money.fromMajor(principal),
        amountSettled = Money.fromMajor(settled),
        date = "2026-08-01",
        dueDate = dueDate
    )

    private fun account(type: AccountType, balanceMajor: Long) = Account(
        id = 1L,
        name = "Test account",
        type = type,
        balance = Money.fromMajor(balanceMajor),
        currencyCode = "USD",
        createdAt = "2026-01-01 00:00:00"
    )

    private fun loanTransaction(
        type: TransactionType,
        major: Long,
        sourceType: TransactionSourceType
    ) = Transaction(
        id = 1L,
        amount = Money.fromMajor(major),
        type = type,
        categoryId = "lending",
        accountId = 1L,
        date = "2026-09-04 10:00:00",
        source = TransactionSource(type = sourceType, referenceId = 1L)
    )

    // -------------------------------------------------------------------------
    // Direction
    // -------------------------------------------------------------------------

    @Test
    fun lendingIsAnAssetAndBorrowingIsALiability() {
        val records = listOf(
            record(LoanDirection.LENT, principal = 1500, settled = 500),
            record(LoanDirection.BORROWED, principal = 2000, settled = 750)
        )

        assertEquals(Money.fromMajor(1000L), FinancialCalculator.totalLent(records))
        assertEquals(Money.fromMajor(1250L), FinancialCalculator.totalBorrowed(records))
    }

    @Test
    fun settledRecordsDropOutOfBothTotals() {
        val records = listOf(
            record(LoanDirection.LENT, principal = 300, settled = 300),
            record(LoanDirection.BORROWED, principal = 450, settled = 450)
        )

        assertTrue(FinancialCalculator.totalLent(records).isZero)
        assertTrue(FinancialCalculator.totalBorrowed(records).isZero)
    }

    @Test
    fun moneyLentCountsAsAnAssetSoNetPositionSurvivesLending() {
        // Lending 500 from a 2,000 bank balance: the cash leaves the account and
        // becomes a receivable. Net worth must not change.
        val before = FinancialCalculator.netPosition(
            accounts = listOf(account(AccountType.BANK, 2_000)),
            debts = emptyList(),
            emis = emptyList(),
            records = emptyList()
        )

        val after = FinancialCalculator.netPosition(
            accounts = listOf(account(AccountType.BANK, 1_500)),
            debts = emptyList(),
            emis = emptyList(),
            records = listOf(record(LoanDirection.LENT, principal = 500))
        )

        assertEquals(before.net, after.net)
        assertEquals(Money.fromMajor(500L), after.assets.moneyLentOut)
    }

    @Test
    fun borrowingRaisesCashAndLiabilityTogether() {
        // Borrowing 500 into a 2,000 account leaves net worth unchanged: more
        // cash, and exactly as much owed.
        val before = FinancialCalculator.netPosition(
            accounts = listOf(account(AccountType.BANK, 2_000)),
            debts = emptyList(),
            emis = emptyList(),
            records = emptyList()
        )

        val after = FinancialCalculator.netPosition(
            accounts = listOf(account(AccountType.BANK, 2_500)),
            debts = emptyList(),
            emis = emptyList(),
            records = listOf(record(LoanDirection.BORROWED, principal = 500))
        )

        assertEquals(before.net, after.net)
        assertEquals(Money.fromMajor(500L), after.liabilities.moneyBorrowed)
    }

    // -------------------------------------------------------------------------
    // Status
    // -------------------------------------------------------------------------

    @Test
    fun statusFollowsSettlementAndDueDate() {
        assertEquals(
            LoanStatus.ACTIVE,
            FinancialCalculator.loanStatus(record(LoanDirection.LENT, 1000), today)
        )
        assertEquals(
            LoanStatus.PARTIALLY_SETTLED,
            FinancialCalculator.loanStatus(record(LoanDirection.LENT, 1000, settled = 400), today)
        )
        assertEquals(
            LoanStatus.OVERDUE,
            FinancialCalculator.loanStatus(
                record(LoanDirection.BORROWED, 1000, dueDate = "2026-08-20"),
                today
            )
        )
    }

    @Test
    fun aSettledRecordIsNeverReportedOverdue() {
        val settled = record(LoanDirection.LENT, 1000, settled = 1000, dueDate = "2026-07-01")
        assertEquals(LoanStatus.SETTLED, FinancialCalculator.loanStatus(settled, today))
    }

    // -------------------------------------------------------------------------
    // Cash flow
    // -------------------------------------------------------------------------

    @Test
    fun loanMovementsAreExcludedFromIncomeAndExpenseTotals() {
        val summary = FinancialCalculator.summarise(
            listOf(
                loanTransaction(TransactionType.INCOME, 2_000, TransactionSourceType.BORROW_RECEIVED),
                loanTransaction(TransactionType.EXPENSE, 1_500, TransactionSourceType.LEND_GIVEN),
                loanTransaction(TransactionType.EXPENSE, 750, TransactionSourceType.BORROW_REPAYMENT),
                loanTransaction(TransactionType.INCOME, 500, TransactionSourceType.LEND_REPAYMENT)
            )
        )

        // Borrowing is not earnings and lending is not spending.
        assertTrue(summary.income.isZero)
        assertTrue(summary.expense.isZero)
    }

    @Test
    fun aCompleteLendThenRepayRoundTripShowsNoProfit() {
        val summary = FinancialCalculator.summarise(
            listOf(
                loanTransaction(TransactionType.EXPENSE, 1_000, TransactionSourceType.LEND_GIVEN),
                loanTransaction(TransactionType.INCOME, 1_000, TransactionSourceType.LEND_REPAYMENT)
            )
        )
        assertTrue(summary.net.isZero, "getting your own money back is not income")
    }

    @Test
    fun ordinaryAndDebtTransactionsStillCount() {
        val summary = FinancialCalculator.summarise(
            listOf(
                Transaction(
                    id = 1L,
                    amount = Money.fromMajor(3_000L),
                    type = TransactionType.INCOME,
                    categoryId = "salary",
                    accountId = 1L,
                    date = "2026-09-04 09:00:00"
                ),
                // Servicing a debt is real spending and is deliberately included.
                loanTransaction(TransactionType.EXPENSE, 500, TransactionSourceType.DEBT_PAYMENT),
                loanTransaction(TransactionType.EXPENSE, 200, TransactionSourceType.EMI_PAYMENT)
            )
        )

        assertEquals(Money.fromMajor(3_000L), summary.income)
        assertEquals(Money.fromMajor(700L), summary.expense)
    }
}
