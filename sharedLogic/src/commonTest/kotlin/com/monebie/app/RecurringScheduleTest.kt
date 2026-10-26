package com.aurex.app

import com.aurex.app.core.Money
import com.aurex.app.domain.calculation.RecurringSchedule
import com.aurex.app.domain.model.PaymentFrequency
import com.aurex.app.domain.model.RecurringTransaction
import com.aurex.app.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The arithmetic that decides when a recurring transaction lands.
 *
 * Month-end anchoring is the case worth guarding: a schedule created on the
 * 31st has to reach the 28th of February and then come back to the 31st, not
 * drift to the 28th of every month that follows.
 */
class RecurringScheduleTest {

    @Test
    fun monthlyStepsKeepTheAnchorDayThroughShortMonths() {
        assertEquals("2026-02-28", RecurringSchedule.next("2026-01-31", PaymentFrequency.MONTHLY, anchorDay = 31))
        assertEquals("2026-03-31", RecurringSchedule.next("2026-02-28", PaymentFrequency.MONTHLY, anchorDay = 31))
        assertEquals("2026-04-30", RecurringSchedule.next("2026-03-31", PaymentFrequency.MONTHLY, anchorDay = 31))
    }

    @Test
    fun monthlyStepsFromAnOrdinaryDayAreUnremarkable() {
        assertEquals("2026-10-15", RecurringSchedule.next("2026-09-15", PaymentFrequency.MONTHLY))
        assertEquals("2027-01-15", RecurringSchedule.next("2026-12-15", PaymentFrequency.MONTHLY))
    }

    @Test
    fun quarterlyAndYearlyStepByTheirMonthCount() {
        assertEquals("2026-12-01", RecurringSchedule.next("2026-09-01", PaymentFrequency.QUARTERLY))
        assertEquals("2027-09-01", RecurringSchedule.next("2026-09-01", PaymentFrequency.YEARLY))
        // 29 Feb anchors back to the 29th only in a leap year.
        assertEquals("2025-02-28", RecurringSchedule.next("2024-02-29", PaymentFrequency.YEARLY, anchorDay = 29))
    }

    @Test
    fun dayBasedCyclesAddTheirPeriod() {
        assertEquals("2026-09-08", RecurringSchedule.next("2026-09-01", PaymentFrequency.WEEKLY))
        assertEquals("2026-09-15", RecurringSchedule.next("2026-09-01", PaymentFrequency.BIWEEKLY))
    }

    @Test
    fun oneTimeDoesNotAdvance() {
        assertEquals("2026-09-01", RecurringSchedule.next("2026-09-01", PaymentFrequency.ONE_TIME))
    }

    @Test
    fun occurrencesDueCoverEverythingUpToToday() {
        val due = RecurringSchedule.occurrencesDue(
            nextDue = "2026-06-30",
            today = "2026-09-20",
            frequency = PaymentFrequency.MONTHLY,
            anchorDay = 30
        )
        assertEquals(listOf("2026-06-30", "2026-07-30", "2026-08-30"), due)
    }

    @Test
    fun occurrencesDueStopAtTheEndDate() {
        val due = RecurringSchedule.occurrencesDue(
            nextDue = "2026-06-01",
            today = "2026-09-20",
            frequency = PaymentFrequency.MONTHLY,
            anchorDay = 1,
            endDate = "2026-07-15"
        )
        assertEquals(listOf("2026-06-01", "2026-07-01"), due)
    }

    @Test
    fun occurrencesDueAreBoundedSoAnOldScheduleCannotFloodTheLedger() {
        val due = RecurringSchedule.occurrencesDue(
            nextDue = "2020-01-01",
            today = "2026-09-20",
            frequency = PaymentFrequency.WEEKLY,
            anchorDay = 1
        )
        assertEquals(RecurringSchedule.MAX_OCCURRENCES_PER_PASS, due.size)
    }

    @Test
    fun nothingIsDueForAFutureSchedule() {
        val due = RecurringSchedule.occurrencesDue(
            nextDue = "2026-10-01",
            today = "2026-09-20",
            frequency = PaymentFrequency.MONTHLY,
            anchorDay = 1
        )
        assertTrue(due.isEmpty())
    }

    @Test
    fun monthlyEquivalentNormalisesEveryCycle() {
        fun schedule(frequency: PaymentFrequency) = RecurringTransaction(
            id = 1L, name = "x", amount = Money.fromMajor(120L), type = TransactionType.EXPENSE,
            categoryId = "food", accountId = 1L, frequency = frequency,
            startDate = "2026-01-01", nextDueDate = "2026-01-01", createdAt = ""
        )
        assertEquals(Money.fromMajor(120L), schedule(PaymentFrequency.MONTHLY).monthlyEquivalent)
        assertEquals(Money.fromMajor(40L), schedule(PaymentFrequency.QUARTERLY).monthlyEquivalent)
        assertEquals(Money.fromMajor(10L), schedule(PaymentFrequency.YEARLY).monthlyEquivalent)
        assertEquals(Money.fromMajor(520L), schedule(PaymentFrequency.WEEKLY).monthlyEquivalent)
    }

    @Test
    fun aScheduleKnowsWhenItHasEnded() {
        val ended = RecurringTransaction(
            id = 1L, name = "x", amount = Money.fromMajor(1L), type = TransactionType.EXPENSE,
            categoryId = "food", accountId = 1L, frequency = PaymentFrequency.MONTHLY,
            startDate = "2026-01-01", nextDueDate = "2026-08-01", endDate = "2026-07-31", createdAt = ""
        )
        assertTrue(ended.hasEnded)
        assertFalse(ended.isActive)
        assertFalse(ended.isDue("2026-09-20"))
    }
}
