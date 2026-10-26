package com.monebie.app

import com.monebie.app.core.Money
import com.monebie.app.domain.calculation.FinancialCalculator
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.AccountType
import com.monebie.app.domain.model.DueBucket
import com.monebie.app.domain.model.PeriodChange
import com.monebie.app.domain.model.Transaction
import com.monebie.app.domain.model.TransactionSource
import com.monebie.app.domain.model.TransactionSourceType
import com.monebie.app.domain.model.TransactionType
import com.monebie.app.domain.model.UpcomingPayment
import com.monebie.app.domain.model.UpcomingPaymentKind
import com.monebie.app.domain.usecase.export.TransactionCsv
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the analysis and export logic added in the final phase: the category
 * split, the elapsed-day rate, urgency bucketing, and a CSV that a spreadsheet
 * can actually read back.
 */
class InsightsAndExportTest {

    private fun transaction(
        id: Long,
        major: Long,
        type: TransactionType = TransactionType.EXPENSE,
        categoryId: String = "food",
        day: String = "2026-09-04",
        accountId: Long = 1L,
        note: String? = null,
        sourceType: TransactionSourceType = TransactionSourceType.MANUAL
    ) = Transaction(
        id = id,
        amount = Money.fromMajor(major),
        type = type,
        categoryId = categoryId,
        accountId = accountId,
        date = "$day 10:00:00",
        note = note,
        source = TransactionSource(type = sourceType)
    )

    // -------------------------------------------------------------------------
    // Category analysis
    // -------------------------------------------------------------------------

    @Test
    fun categorySharesAddUpToTheWholeAndAreOrderedLargestFirst() {
        val breakdown = FinancialCalculator.categoryBreakdown(
            listOf(
                transaction(1, 300, categoryId = "food"),
                transaction(2, 100, categoryId = "food"),
                transaction(3, 600, categoryId = "housing")
            ),
            TransactionType.EXPENSE
        )

        assertEquals(listOf("housing", "food"), breakdown.map { it.categoryId })
        assertEquals(Money.fromMajor(600L), breakdown.first().amount)
        assertEquals(2, breakdown.last().transactionCount)
        assertEquals(1f, breakdown.sumOf { it.share.toDouble() }.toFloat())
    }

    @Test
    fun loanMovementsDoNotAppearAsSpendingCategories() {
        // Money lent out is not spending, so it must not show up as a category
        // or the shares would no longer describe the expense total above them.
        val breakdown = FinancialCalculator.categoryBreakdown(
            listOf(
                transaction(1, 200, categoryId = "food"),
                transaction(
                    2, 5_000,
                    categoryId = "lending",
                    sourceType = TransactionSourceType.LEND_GIVEN
                )
            ),
            TransactionType.EXPENSE
        )

        assertEquals(listOf("food"), breakdown.map { it.categoryId })
        assertEquals(1f, breakdown.single().share)
    }

    @Test
    fun anEmptyPeriodProducesNoCategoriesRatherThanDividingByZero() {
        assertTrue(
            FinancialCalculator.categoryBreakdown(emptyList(), TransactionType.EXPENSE).isEmpty()
        )
    }

    // -------------------------------------------------------------------------
    // Daily rate
    // -------------------------------------------------------------------------

    @Test
    fun theCurrentMonthCountsOnlyTheDaysThatHaveHappened() {
        // 4 September: four days elapsed, not thirty.
        assertEquals(4, FinancialCalculator.elapsedDaysInMonth("2026-09", "2026-09-04"))
    }

    @Test
    fun aFinishedMonthCountsInFull() {
        assertEquals(31, FinancialCalculator.elapsedDaysInMonth("2026-08", "2026-09-04"))
        assertEquals(28, FinancialCalculator.elapsedDaysInMonth("2026-02", "2026-09-04"))
    }

    @Test
    fun aFutureMonthHasNoElapsedDays() {
        assertEquals(0, FinancialCalculator.elapsedDaysInMonth("2026-12", "2026-09-04"))
    }

    @Test
    fun theDailyRateUsesElapsedDaysSoAPartMonthIsNotFlattered() {
        // 400 spent over 4 elapsed days is 100 a day. Dividing by the month's
        // full 30 days would report 13 and hide the overspending.
        val rate = FinancialCalculator.dailyAverageExpense(Money.fromMajor(400L), days = 4)
        assertEquals(Money.fromMajor(100L), rate)

        assertTrue(FinancialCalculator.dailyAverageExpense(Money.fromMajor(400L), days = 0).isZero)
    }

    @Test
    fun theHeaviestDayIsTheOneWithTheMostSpending() {
        val heaviest = FinancialCalculator.heaviestSpendingDay(
            listOf(
                transaction(1, 50, day = "2026-09-01"),
                transaction(2, 400, day = "2026-09-02"),
                transaction(3, 120, day = "2026-09-03"),
                // Income must not make a day look heavy.
                transaction(4, 9_000, type = TransactionType.INCOME, day = "2026-09-01")
            )
        )

        assertEquals("2026-09-02", heaviest?.day)
        assertEquals(Money.fromMajor(400L), heaviest?.expense)
        assertNull(FinancialCalculator.heaviestSpendingDay(emptyList()))
    }

    // -------------------------------------------------------------------------
    // Period comparison
    // -------------------------------------------------------------------------

    @Test
    fun aChangeReportsDirectionAndSizeAgainstThePreviousPeriod() {
        val spentMore = PeriodChange(Money.fromMajor(1_200L), Money.fromMajor(1_000L))
        assertTrue(spentMore.increased)
        assertEquals(Money.fromMajor(200L), spentMore.difference)
        assertTrue(spentMore.hasComparison)

        // Nothing last month means there is no comparison to draw, rather than
        // an infinite increase.
        val noBaseline = PeriodChange(Money.fromMajor(500L), Money.ZERO)
        assertTrue(!noBaseline.hasComparison)
        assertEquals(0f, noBaseline.ratio)
    }

    // -------------------------------------------------------------------------
    // Urgency
    // -------------------------------------------------------------------------

    @Test
    fun paymentsAreBucketedByHowSoonTheyNeedAttention() {
        assertEquals(DueBucket.OVERDUE, DueBucket.forDaysUntilDue(-1))
        assertEquals(DueBucket.TODAY, DueBucket.forDaysUntilDue(0))
        assertEquals(DueBucket.THIS_WEEK, DueBucket.forDaysUntilDue(7))
        assertEquals(DueBucket.THIS_MONTH, DueBucket.forDaysUntilDue(8))
        assertEquals(DueBucket.LATER, DueBucket.forDaysUntilDue(32))
    }

    @Test
    fun bucketsComeBackInUrgencyOrder() {
        fun payment(id: String, days: Long) = UpcomingPayment(
            id = id,
            kind = UpcomingPaymentKind.DEBT,
            title = id,
            subtitle = "",
            amount = Money.fromMajor(100L),
            dueDate = "2026-09-04",
            daysUntilDue = days
        )

        val buckets = FinancialCalculator.bucketByUrgency(
            listOf(payment("later", 40), payment("overdue", -3), payment("week", 4))
        )

        assertEquals(
            listOf(DueBucket.OVERDUE, DueBucket.THIS_WEEK, DueBucket.LATER),
            buckets.keys.toList()
        )
    }

    // -------------------------------------------------------------------------
    // Export
    // -------------------------------------------------------------------------

    private val account = Account(
        id = 1L,
        name = "Main, Bank",
        type = AccountType.BANK,
        balance = Money.fromMajor(1_000L),
        currencyCode = "USD",
        createdAt = "2026-01-01 00:00:00"
    )

    @Test
    fun theCsvHasAHeaderAndOneRowPerTransaction() {
        val file = TransactionCsv.build(
            transactions = listOf(transaction(1, 250), transaction(2, 100)),
            accounts = listOf(account)
        )

        val lines = file.content.trim().lines()
        assertEquals(3, lines.size)
        assertTrue(lines.first().startsWith("Date,Type,Category"))
        assertEquals(2, file.rowCount)
        assertEquals("text/csv", file.mimeType)
    }

    @Test
    fun amountsAreWrittenAsPlainNumbersASpreadsheetCanParse() {
        val file = TransactionCsv.build(
            transactions = listOf(transaction(1, 1_234)),
            accounts = listOf(account)
        )

        // No currency symbol and no thousands separator, or the column stops
        // being numeric on import.
        assertTrue(file.content.contains("1234.00"), file.content)
    }

    @Test
    fun fieldsContainingCommasOrQuotesAreEscapedRatherThanShiftingColumns() {
        val file = TransactionCsv.build(
            transactions = listOf(
                transaction(1, 50, note = "Lunch, drinks and a \"tip\"")
            ),
            accounts = listOf(account)
        )

        val row = file.content.trim().lines().last()
        assertTrue(row.contains("\"Lunch, drinks and a \"\"tip\"\"\""), row)
        // The account name also contains a comma and must be quoted too.
        assertTrue(row.contains("\"Main, Bank\""), row)
    }

    @Test
    fun theSourceColumnMarksAutomaticallyPostedRows() {
        val file = TransactionCsv.build(
            transactions = listOf(
                transaction(1, 500, sourceType = TransactionSourceType.EMI_PAYMENT)
            ),
            accounts = listOf(account)
        )

        assertTrue(file.content.trim().lines().last().endsWith("EMI_PAYMENT"))
    }
}
