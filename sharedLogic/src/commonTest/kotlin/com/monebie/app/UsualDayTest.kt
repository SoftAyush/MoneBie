package com.aurex.app

import com.aurex.app.core.Money
import com.aurex.app.domain.model.DailyTotal
import com.aurex.app.domain.model.DashboardSnapshot
import com.aurex.app.domain.model.NetPosition
import com.aurex.app.domain.model.PeriodSummary
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers what the dashboard calls "a usual day".
 *
 * The figure exists only to be compared against today, so the two ways it can
 * quietly go wrong are both worth pinning: including today drags the average
 * towards the very number being judged, and an empty window divides by zero.
 */
class UsualDayTest {

    private val today = "2026-09-06"

    private fun day(day: String, expense: Long) = DailyTotal(
        day = day,
        income = Money.ZERO,
        expense = Money.fromMajor(expense)
    )

    private fun snapshot(trend: List<DailyTotal>) = DashboardSnapshot(
        userName = "Test",
        currencySymbol = "$",
        today = today,
        accounts = emptyList(),
        totalBalance = Money.ZERO,
        todaySummary = PeriodSummary(),
        monthSummary = PeriodSummary(),
        weeklyTrend = trend,
        netPosition = NetPosition(),
        totalDebtOwed = Money.ZERO,
        totalMoneyBorrowed = Money.ZERO,
        totalMoneyLent = Money.ZERO,
        remainingEmiAmount = Money.ZERO,
        emiDueThisMonth = Money.ZERO,
        upcomingPayments = emptyList(),
        recentTransactions = emptyList()
    )

    @Test
    fun `averages the days before today`() {
        val trend = listOf(
            day("2026-09-04", 100L),
            day("2026-09-05", 200L),
            day(today, 900L)
        )
        // (100 + 200) / 2, with today's 900 kept out of its own comparison.
        assertEquals(Money.fromMajor(150L), snapshot(trend).usualDailyExpense)
    }

    @Test
    fun `a day with no spending still counts towards the average`() {
        // Dropping the quiet days would report the average of the days money
        // moved, which is a different and much larger number.
        val trend = listOf(
            day("2026-09-04", 300L),
            day("2026-09-05", 0L),
            day(today, 50L)
        )
        assertEquals(Money.fromMajor(150L), snapshot(trend).usualDailyExpense)
    }

    @Test
    fun `today alone has nothing to compare with`() {
        assertEquals(Money.ZERO, snapshot(listOf(day(today, 900L))).usualDailyExpense)
        assertEquals(Money.ZERO, snapshot(emptyList()).usualDailyExpense)
    }

    @Test
    fun `the average keeps its minor units`() {
        // 250 over three days is 83.33, and the third of a rupee that gets
        // dropped must not become a third of a hundred rupees.
        val trend = listOf(
            day("2026-09-03", 100L),
            day("2026-09-04", 100L),
            day("2026-09-05", 50L),
            day(today, 0L)
        )
        assertEquals(Money(8333L), snapshot(trend).usualDailyExpense)
    }
}
