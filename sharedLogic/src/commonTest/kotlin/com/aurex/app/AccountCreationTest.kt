package com.aurex.app

import com.aurex.app.core.Money
import com.aurex.app.domain.model.Account
import com.aurex.app.domain.model.AccountType
import com.aurex.app.domain.model.isAsset
import com.aurex.app.domain.model.openingBalanceFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the sign an opening balance is stored with.
 *
 * Balances live on one signed scale, and which end of it an account starts on
 * is decided once, at creation, from the type. Getting it wrong is invisible
 * afterwards - the figure looks right, it is simply on the wrong side of the
 * net position - so it is worth pinning here rather than trusting the entry
 * screen to keep asking the right question.
 */
class AccountCreationTest {

    private val assetTypes =
        listOf(AccountType.CASH, AccountType.BANK, AccountType.DIGITAL_WALLET)

    private val liabilityTypes =
        listOf(AccountType.CREDIT_CARD, AccountType.LOAN, AccountType.EMI)

    private fun card(balance: Money) = Account(
        id = 1L,
        name = "Visa",
        type = AccountType.CREDIT_CARD,
        balance = balance,
        currencyCode = "NPR",
        isArchived = false,
        createdAt = "2026-01-01 00:00:00"
    )

    @Test
    fun `an asset account keeps the figure as typed`() {
        val entered = Money.fromMajor(12_000L)
        assetTypes.forEach { type ->
            assertEquals(
                entered,
                openingBalanceFor(type, entered),
                "$type should hold what was typed"
            )
        }
    }

    @Test
    fun `a liability account stores what is owed as negative`() {
        // The entry field only produces digits, so a card opened at 12,000 used
        // to arrive as +12,000 and read as twelve thousand available.
        val entered = Money.fromMajor(12_000L)
        liabilityTypes.forEach { type ->
            assertEquals(
                Money.fromMajor(-12_000L),
                openingBalanceFor(type, entered),
                "$type should owe what was typed"
            )
        }
    }

    @Test
    fun `every account type is covered by exactly one of the two rules`() {
        // A new type added to the enum has to be classified deliberately rather
        // than falling into whichever branch happens to be the else.
        assertEquals(
            AccountType.entries.size,
            assetTypes.size + liabilityTypes.size,
            "an account type is unaccounted for"
        )
        assetTypes.forEach { assertTrue(it.isAsset, "$it should be an asset") }
        liabilityTypes.forEach { assertFalse(it.isAsset, "$it should be a liability") }
    }

    @Test
    fun `what a card owes reaches the balance sheet`() {
        val stored = openingBalanceFor(AccountType.CREDIT_CARD, Money.fromMajor(12_000L))

        // The point of the sign: `outstanding` is what every liability total in
        // the app is built from, and it only reads a negative balance.
        assertEquals(Money.fromMajor(12_000L), card(stored).outstanding)
        assertFalse(card(stored).isAssetAccount)
    }

    @Test
    fun `a card opened at nothing owes nothing`() {
        // Zero has no sign to get wrong, but negating it must not leave a
        // balance that reads as a liability of nothing at all.
        val stored = openingBalanceFor(AccountType.CREDIT_CARD, Money.ZERO)
        assertEquals(Money.ZERO, stored)
        assertEquals(Money.ZERO, card(stored).outstanding)
    }
}
