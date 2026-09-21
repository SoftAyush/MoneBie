package com.aurex.app

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.aurex.app.core.Money
import com.aurex.app.data.database.DatabaseHelper
import com.aurex.app.data.repository.RecurringTransactionRepositoryImpl
import com.aurex.app.data.repository.TransactionRepositoryImpl
import com.aurex.app.db.AurexDatabase
import com.aurex.app.domain.model.PaymentFrequency
import com.aurex.app.domain.model.PaymentMethod
import com.aurex.app.domain.model.RecurringTransactionDraft
import com.aurex.app.domain.model.TransactionSourceType
import com.aurex.app.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Posting recurring transactions through the real schema.
 *
 * The properties that matter: every due occurrence lands in the ledger exactly
 * once, the account balance moves with it, a second pass finds nothing to do,
 * and deleting the schedule leaves the history alone.
 */
class RecurringPostingTest {

    private fun freshDatabase(): DatabaseHelper {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
            AurexDatabase.Schema.create(it).value
        }
        return DatabaseHelper(driver).also {
            it.accountQueries.backupInsertAccount(
                id = 1L, name = "Everyday", type = "BANK", balance_minor = 1_000_00L,
                currency_code = "NPR", is_archived = 0L, created_at = "2026-01-01"
            )
        }
    }

    private fun rent(startDate: String, endDate: String? = null) = RecurringTransactionDraft(
        name = "Rent",
        amount = Money.fromMajor(100L),
        type = TransactionType.EXPENSE,
        categoryId = "housing",
        accountId = 1L,
        paymentMethod = PaymentMethod.BANK_TRANSFER,
        frequency = PaymentFrequency.MONTHLY,
        startDate = startDate,
        endDate = endDate
    )

    private fun DatabaseHelper.balance(): Long =
        accountQueries.getAccountById(1L).executeAsOne().balance_minor

    @Test
    fun `every occurrence up to today is posted once and the balance follows`() = runTest {
        val db = freshDatabase()
        val recurring = RecurringTransactionRepositoryImpl(db)
        val transactions = TransactionRepositoryImpl(db)

        val id = recurring.createRecurring(rent(startDate = "2026-06-30"))

        val result = recurring.postDue(today = "2026-09-20")

        assertEquals(3, result.postedCount)
        assertEquals(listOf("Rent"), result.postedNames)

        val ledger = transactions.observeTransactions().first()
        assertEquals(3, ledger.size)
        assertEquals(
            listOf("2026-08-30", "2026-07-30", "2026-06-30"),
            ledger.map { it.date.take(10) },
            "newest first, on the anchored day of each month"
        )
        assertTrue(ledger.all { it.source.type == TransactionSourceType.RECURRING && it.source.referenceId == id })
        assertEquals(1_000_00L - 300_00L, db.balance())

        // The schedule now points past what was posted.
        assertEquals("2026-09-30", recurring.getRecurring(id)!!.nextDueDate)

        // And a second pass on the same day is a no-op.
        assertEquals(0, recurring.postDue(today = "2026-09-20").postedCount)
        assertEquals(3, transactions.observeTransactions().first().size)
    }

    @Test
    fun `a paused schedule posts nothing and an ended one stops at its end`() = runTest {
        val db = freshDatabase()
        val recurring = RecurringTransactionRepositoryImpl(db)

        val paused = recurring.createRecurring(rent(startDate = "2026-01-01"))
        recurring.setPaused(paused, true)

        recurring.createRecurring(rent(startDate = "2026-07-01", endDate = "2026-08-15"))

        val result = recurring.postDue(today = "2026-09-20")

        assertEquals(2, result.postedCount, "July and August only; September is past the end")
        assertEquals("2026-01-01", recurring.getRecurring(paused)!!.nextDueDate)
    }

    @Test
    fun `skipping moves the schedule on without touching the ledger`() = runTest {
        val db = freshDatabase()
        val recurring = RecurringTransactionRepositoryImpl(db)
        val transactions = TransactionRepositoryImpl(db)

        val id = recurring.createRecurring(rent(startDate = "2026-09-30"))
        recurring.skipNext(id)

        assertEquals("2026-10-30", recurring.getRecurring(id)!!.nextDueDate)
        assertTrue(transactions.observeTransactions().first().isEmpty())
        assertEquals(1_000_00L, db.balance())
    }

    @Test
    fun `posting now is dated today and advances the schedule`() = runTest {
        val db = freshDatabase()
        val recurring = RecurringTransactionRepositoryImpl(db)
        val transactions = TransactionRepositoryImpl(db)

        val id = recurring.createRecurring(rent(startDate = "2026-09-30"))
        recurring.postNow(id, today = "2026-09-20")

        val posted = transactions.observeTransactions().first().single()
        assertEquals("2026-09-20", posted.date.take(10))
        assertEquals("2026-10-30", recurring.getRecurring(id)!!.nextDueDate)
        assertEquals(1_000_00L - 100_00L, db.balance())
    }

    @Test
    fun `deleting the schedule leaves what it posted in place`() = runTest {
        val db = freshDatabase()
        val recurring = RecurringTransactionRepositoryImpl(db)
        val transactions = TransactionRepositoryImpl(db)

        val id = recurring.createRecurring(rent(startDate = "2026-08-01"))
        recurring.postDue(today = "2026-09-20")
        recurring.deleteRecurring(id)

        assertEquals(2, transactions.observeTransactions().first().size)
        assertEquals(1_000_00L - 200_00L, db.balance())
        assertEquals(null, recurring.getRecurring(id))
    }

    @Test
    fun `a posted occurrence stays editable by the user`() = runTest {
        val db = freshDatabase()
        val recurring = RecurringTransactionRepositoryImpl(db)
        val transactions = TransactionRepositoryImpl(db)

        recurring.createRecurring(rent(startDate = "2026-09-01"))
        recurring.postDue(today = "2026-09-20")

        val posted = transactions.observeTransactions().first().single()
        // Unlike a debt or EMI payment, nothing else depends on this row, so
        // the ordinary delete path must not refuse it.
        transactions.deleteTransaction(posted.id)

        assertTrue(transactions.observeTransactions().first().isEmpty())
        assertEquals(1_000_00L, db.balance())
    }

    @Test
    fun `editing the amount does not re-post months already recorded`() = runTest {
        val db = freshDatabase()
        val recurring = RecurringTransactionRepositoryImpl(db)
        val transactions = TransactionRepositoryImpl(db)

        val id = recurring.createRecurring(rent(startDate = "2026-07-01"))
        recurring.postDue(today = "2026-09-20")
        assertEquals(3, transactions.observeTransactions().first().size)

        recurring.updateRecurring(id, rent(startDate = "2026-07-01").copy(amount = Money.fromMajor(120L)))
        assertEquals("2026-10-01", recurring.getRecurring(id)!!.nextDueDate)
        assertEquals(0, recurring.postDue(today = "2026-09-20").postedCount)
    }
}
