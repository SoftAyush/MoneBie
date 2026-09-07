package com.aurex.app

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.aurex.app.data.database.DatabaseHelper
import com.aurex.app.data.repository.BackupRepositoryImpl
import com.aurex.app.db.AurexDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Holds the backup to the one promise that matters: what comes back is what
 * went in.
 *
 * The interesting failure is not a lost row, which is obvious the moment anyone
 * looks. It is a row that comes back attached to the wrong thing. Every table
 * here points at another by rowid - a transaction at its account, a payment at
 * its debt, a repayment at the transaction that recorded it - and AUTOINCREMENT
 * hands out fresh ids on insert. A restore that let it do so would produce a
 * database full of plausible rows describing somebody else's finances, and
 * nothing about it would look wrong until a balance was checked by hand.
 *
 * So the ids in the fixture below are deliberately sparse and out of order.
 * Sequential ids would round-trip correctly even through an implementation that
 * simply renumbered everything.
 */
class BackupRoundTripTest {

    private fun freshDatabase(): SqlDriver =
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { AurexDatabase.Schema.create(it).value }

    /** A small but complete ledger, using ids no counter would produce by chance. */
    private fun DatabaseHelper.seedLedger() {
        userQueries.backupInsertUser(
            name = "Ayush",
            date = "2026-01-04",
            currency_code = "NPR",
            theme_mode = "DARK"
        )

        accountQueries.backupInsertAccount(
            id = 7L, name = "Everyday", type = "BANK", balance_minor = 125_00L,
            currency_code = "NPR", is_archived = 0L, created_at = "2026-01-04"
        )
        accountQueries.backupInsertAccount(
            id = 41L, name = "Old wallet", type = "CASH", balance_minor = -3_50L,
            currency_code = "NPR", is_archived = 1L, created_at = "2026-01-05"
        )

        debtQueries.backupInsertDebt(
            id = 12L, name = "Car loan", lender = "Bank", original_amount_minor = 900_00L,
            interest_rate = 7.5, start_date = "2026-02-01", due_date = "2027-02-01",
            payment_frequency = "MONTHLY", installment_amount_minor = 75_00L,
            notes = null, default_account_id = 7L, created_at = "2026-02-01"
        )

        emiQueries.backupInsertEmi(
            id = 30L, name = "Laptop", provider = "Store",
            principal_amount_minor = 600_00L, total_payable_amount_minor = 660_00L,
            monthly_amount_minor = 55_00L, interest_rate = null, start_date = "2026-03-01",
            tenure_years = 1.0, payment_account_id = 7L, notes = "12 months",
            created_at = "2026-03-01"
        )

        borrowLendQueries.backupInsertLoanRecord(
            id = 55L, person_name = "Sita", direction = "LENT",
            principal_amount_minor = 200_00L, date = "2026-04-01", due_date = null,
            notes = null, account_id = 7L, principal_transaction_id = 101L,
            created_at = "2026-04-01"
        )

        // Ids chosen so that nothing lines up: the transaction settling debt 12
        // is id 103, and it is the third row rather than the first.
        transactionQueries.backupInsertTransaction(
            id = 101L, amount_minor = 200_00L, type = "EXPENSE", category_id = "other",
            account_id = 7L, transfer_account_id = null, date = "2026-04-01 09:00:00",
            note = "Lent to Sita", payment_method = "CASH", source_type = "BORROW_LEND",
            source_reference_id = 55L, source_payment_id = null
        )
        transactionQueries.backupInsertTransaction(
            id = 102L, amount_minor = 40_00L, type = "TRANSFER", category_id = "other",
            account_id = 7L, transfer_account_id = 41L, date = "2026-04-02 10:00:00",
            note = null, payment_method = "BANK", source_type = "MANUAL",
            source_reference_id = null, source_payment_id = null
        )
        transactionQueries.backupInsertTransaction(
            id = 103L, amount_minor = 75_00L, type = "EXPENSE", category_id = "other",
            account_id = 7L, transfer_account_id = null, date = "2026-04-03 11:00:00",
            note = "Car loan", payment_method = "BANK", source_type = "DEBT",
            source_reference_id = 12L, source_payment_id = 71L
        )

        debtQueries.backupInsertDebtPayment(
            id = 71L, debt_id = 12L, amount_minor = 75_00L, date = "2026-04-03",
            account_id = 7L, transaction_id = 103L, note = null
        )
        emiQueries.backupInsertEmiPayment(
            id = 88L, emi_id = 30L, installment_number = 1L, amount_minor = 55_00L,
            due_date = "2026-04-01", paid_date = "2026-04-01", account_id = 7L,
            transaction_id = null
        )
        borrowLendQueries.backupInsertLoanRepayment(
            id = 99L, record_id = 55L, amount_minor = 50_00L, date = "2026-05-01",
            account_id = 7L, transaction_id = null, note = "part repaid"
        )
    }

    @Test
    fun `a snapshot restored into a fresh database reproduces it exactly`() = runTest {
        val source = DatabaseHelper(freshDatabase()).also { it.seedLedger() }
        val snapshot = BackupRepositoryImpl(source).snapshot()

        val target = DatabaseHelper(freshDatabase())
        BackupRepositoryImpl(target).restore(snapshot)

        // Compared as whole snapshots rather than field by field, so a column
        // added later that nobody remembers to carry fails this test too.
        assertEquals(
            snapshot.copy(createdAt = ""),
            BackupRepositoryImpl(target).snapshot().copy(createdAt = "")
        )
    }

    @Test
    fun `rows keep the ids their siblings point at`() = runTest {
        val source = DatabaseHelper(freshDatabase()).also { it.seedLedger() }
        val target = DatabaseHelper(freshDatabase())
        BackupRepositoryImpl(target).restore(BackupRepositoryImpl(source).snapshot())

        val restored = BackupRepositoryImpl(target).snapshot()

        val debtPayment = restored.debtPayments.single()
        assertEquals(71L, debtPayment.id)
        assertEquals(12L, debtPayment.debtId)
        assertEquals(103L, debtPayment.transactionId)

        // The link the other way: the transaction still names the payment that
        // produced it, which is what makes the debt payment undoable.
        val settling = restored.transactions.single { it.id == 103L }
        assertEquals("DEBT", settling.sourceType)
        assertEquals(12L, settling.sourceReferenceId)
        assertEquals(71L, settling.sourcePaymentId)

        val transfer = restored.transactions.single { it.id == 102L }
        assertEquals(7L, transfer.accountId)
        assertEquals(41L, transfer.transferAccountId)

        assertEquals(55L, restored.loanRepayments.single().recordId)
        assertEquals(30L, restored.emiPayments.single().emiId)
    }

    @Test
    fun `restoring replaces what was there rather than merging into it`() = runTest {
        val source = DatabaseHelper(freshDatabase()).also { it.seedLedger() }
        val snapshot = BackupRepositoryImpl(source).snapshot()

        val target = DatabaseHelper(freshDatabase())
        target.accountQueries.backupInsertAccount(
            id = 999L, name = "Only on this phone", type = "CASH", balance_minor = 5_00L,
            currency_code = "NPR", is_archived = 0L, created_at = "2026-06-01"
        )

        BackupRepositoryImpl(target).restore(snapshot)

        val accounts = BackupRepositoryImpl(target).snapshot().accounts
        assertTrue(accounts.none { it.id == 999L }, "a merge would have kept the local account")
        assertEquals(setOf(7L, 41L), accounts.map { it.id }.toSet())
    }

    @Test
    fun `the archived flag and the theme survive the trip`() = runTest {
        val source = DatabaseHelper(freshDatabase()).also { it.seedLedger() }
        val target = DatabaseHelper(freshDatabase())
        BackupRepositoryImpl(target).restore(BackupRepositoryImpl(source).snapshot())

        val restored = BackupRepositoryImpl(target).snapshot()

        // insertUser omits theme_mode and defaults it to SYSTEM; the backup
        // insert exists precisely so a restore does not silently do that.
        assertEquals("DARK", assertNotNull(restored.user).themeMode)
        assertEquals(1L, restored.accounts.single { it.id == 41L }.isArchived)
        assertEquals(-3_50L, restored.accounts.single { it.id == 41L }.balanceMinor)
    }

    @Test
    fun `the currency reference list is left alone by a restore`() = runTest {
        val source = DatabaseHelper(freshDatabase()).also { it.seedLedger() }
        val target = DatabaseHelper(freshDatabase())

        val before = target.currencyQueries.selectAllCurrencies().executeAsList()
        BackupRepositoryImpl(target).restore(BackupRepositoryImpl(source).snapshot())

        assertEquals(before, target.currencyQueries.selectAllCurrencies().executeAsList())
        assertTrue(before.isNotEmpty(), "the schema should have seeded currencies")
    }
}
