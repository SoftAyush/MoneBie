package com.monebie.app

import com.monebie.app.core.Money
import com.monebie.app.core.total
import com.monebie.app.domain.calculation.FinancialCalculator
import com.monebie.app.domain.model.AccountDelta
import com.monebie.app.domain.model.Transaction
import com.monebie.app.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the balance-posting rules, which are the one place a bug silently
 * corrupts data: a wrong delta leaves an account balance that no transaction
 * history explains, and nothing surfaces the discrepancy.
 */
class LedgerPostingTest {

    private fun transaction(
        id: Long = 1L,
        type: TransactionType,
        major: Long,
        accountId: Long = 1L,
        transferAccountId: Long? = null
    ) = Transaction(
        id = id,
        amount = Money.fromMajor(major),
        type = type,
        categoryId = "food",
        accountId = accountId,
        date = "2026-09-04 10:00:00",
        transferAccountId = transferAccountId
    )

    private fun netFor(accountId: Long, deltas: List<AccountDelta>): Money =
        deltas.filter { it.accountId == accountId }.map { it.delta }.total()

    @Test
    fun incomeCreditsItsAccount() {
        val deltas = FinancialCalculator.balanceDeltas(
            transaction(type = TransactionType.INCOME, major = 250)
        )
        assertEquals(Money.fromMajor(250L), netFor(1L, deltas))
    }

    @Test
    fun expenseDebitsItsAccount() {
        val deltas = FinancialCalculator.balanceDeltas(
            transaction(type = TransactionType.EXPENSE, major = 80)
        )
        assertEquals(Money.fromMajor(-80L), netFor(1L, deltas))
    }

    @Test
    fun transferMovesBetweenAccountsAndIsNetNeutral() {
        val deltas = FinancialCalculator.balanceDeltas(
            transaction(
                type = TransactionType.TRANSFER,
                major = 500,
                accountId = 1L,
                transferAccountId = 2L
            )
        )

        assertEquals(Money.fromMajor(-500L), netFor(1L, deltas))
        assertEquals(Money.fromMajor(500L), netFor(2L, deltas))
        // Moving your own money must not change what you are worth.
        assertTrue(deltas.map { it.delta }.total().isZero)
    }

    @Test
    fun deletingATransactionUndoesItExactly() {
        listOf(
            transaction(type = TransactionType.INCOME, major = 250),
            transaction(type = TransactionType.EXPENSE, major = 80),
            transaction(
                type = TransactionType.TRANSFER,
                major = 500,
                accountId = 1L,
                transferAccountId = 2L
            )
        ).forEach { entry ->
            val applied = FinancialCalculator.balanceDeltas(entry)
            val reversed = FinancialCalculator.reversingDeltas(entry)

            listOf(1L, 2L).forEach { accountId ->
                val net = netFor(accountId, applied) + netFor(accountId, reversed)
                assertTrue(net.isZero, "posting then reversing left a residue on account $accountId")
            }
        }
    }

    @Test
    fun editingSettlesToTheNewValueEvenWhenTypeAndAccountBothChange() {
        // An expense of 80 on account 1, edited into income of 250 on account 2.
        val before = transaction(type = TransactionType.EXPENSE, major = 80, accountId = 1L)
        val after = transaction(type = TransactionType.INCOME, major = 250, accountId = 2L)

        val deltas = FinancialCalculator.reversingDeltas(before) +
            FinancialCalculator.balanceDeltas(after)

        // Account 1 gets its 80 back; account 2 receives the new income.
        assertEquals(Money.fromMajor(80L), netFor(1L, deltas))
        assertEquals(Money.fromMajor(250L), netFor(2L, deltas))
    }

    @Test
    fun amountsParseWithoutFloatingPointDrift() {
        assertEquals(Money(123456L), Money.parse("1,234.56"))
        assertEquals(Money(1000L), Money.parse("10"))
        assertEquals(Money(1050L), Money.parse("10.5"))
        // Repeated addition of a value that has no exact binary representation.
        assertEquals(Money(1000L), List(10) { Money(100L) }.total())
    }
}
