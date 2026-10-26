package com.monebie.app

import com.monebie.app.core.Money
import com.monebie.app.domain.calculation.EmiSchedule
import com.monebie.app.domain.calculation.FinancialCalculator
import com.monebie.app.domain.model.Emi
import com.monebie.app.domain.model.EmiStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the schedule an EMI derives from its terms, and the status rules that
 * depend on today.
 *
 * Everything the spec calls "automatically calculated" is asserted here, since
 * a wrong end date or next-due date is invisible until a payment is missed.
 */
class EmiScheduleTest {

    private val today = "2026-09-04"

    private fun emi(
        startDate: String = "2026-01-01",
        tenureYears: Double = 1.0,
        monthlyMajor: Long = 100,
        totalPayableMajor: Long = 1200,
        installmentsPaid: Int = 0,
        amountPaidMajor: Long = 0
    ) = Emi(
        id = 1L,
        name = "Test EMI",
        provider = "Test provider",
        principalAmount = Money.fromMajor(1000L),
        totalPayableAmount = Money.fromMajor(totalPayableMajor),
        monthlyAmount = Money.fromMajor(monthlyMajor),
        startDate = startDate,
        tenureYears = tenureYears,
        installmentsPaid = installmentsPaid,
        amountPaid = Money.fromMajor(amountPaidMajor)
    )

    @Test
    fun installmentCountAndEndDateComeFromTheTenure() {
        val fiveYear = emi(startDate = "2026-01-01", tenureYears = 5.0)
        assertEquals(60, fiveYear.totalInstallments)
        assertEquals("2031-01-01", fiveYear.endDate)

        // Half years are legitimate tenures and must not truncate to whole years.
        val eighteenMonths = emi(startDate = "2026-01-01", tenureYears = 1.5)
        assertEquals(18, eighteenMonths.totalInstallments)
        assertEquals("2027-07-01", eighteenMonths.endDate)
    }

    @Test
    fun theFirstInstallmentFallsOneMonthAfterTheStart() {
        val fresh = emi(startDate = "2026-01-01", tenureYears = 1.0)
        assertEquals("2026-02-01", fresh.nextDueDate)
        assertEquals(1, fresh.nextInstallmentNumber)
    }

    @Test
    fun theScheduleAdvancesWithEachRecordedPayment() {
        val partway = emi(startDate = "2026-01-01", tenureYears = 1.0, installmentsPaid = 7)
        assertEquals("2026-09-01", partway.nextDueDate)
        assertEquals(8, partway.nextInstallmentNumber)
        assertEquals(5, partway.remainingInstallments)
    }

    @Test
    fun aFinishedEmiHasNoNextDueDate() {
        val done = emi(tenureYears = 1.0, installmentsPaid = 12, amountPaidMajor = 1200)
        assertTrue(done.isCompleted)
        assertNull(done.nextDueDate)
        assertNull(done.nextInstallmentNumber)
        assertTrue(done.remainingAmount.isZero)
        assertEquals(1f, done.progress)
    }

    @Test
    fun theFinalInstallmentCollectsOnlyWhatIsLeft() {
        // Total payable 1200 with 11 of 100 paid leaves 100; a normal monthly
        // amount of 150 must not overshoot the total.
        val nearlyDone = emi(
            tenureYears = 1.0,
            monthlyMajor = 150,
            totalPayableMajor = 1200,
            installmentsPaid = 11,
            amountPaidMajor = 1150
        )
        assertEquals(Money.fromMajor(50L), nearlyDone.remainingAmount)
        assertEquals(Money.fromMajor(50L), nearlyDone.nextInstallmentAmount)
    }

    @Test
    fun statusReflectsWhereTheScheduleStands() {
        // Starts in the future: nothing is due yet.
        assertEquals(
            EmiStatus.UPCOMING,
            FinancialCalculator.emiStatus(emi(startDate = "2026-12-01"), today)
        )
        // Next installment is still ahead.
        assertEquals(
            EmiStatus.ACTIVE,
            FinancialCalculator.emiStatus(
                emi(startDate = "2026-01-01", tenureYears = 5.0, installmentsPaid = 8),
                today
            )
        )
        // Next installment was due in February and never paid.
        assertEquals(
            EmiStatus.OVERDUE,
            FinancialCalculator.emiStatus(
                emi(startDate = "2026-01-01", tenureYears = 5.0, installmentsPaid = 1),
                today
            )
        )
        assertEquals(
            EmiStatus.COMPLETED,
            FinancialCalculator.emiStatus(
                emi(tenureYears = 1.0, installmentsPaid = 12, amountPaidMajor = 1200),
                today
            )
        )
    }

    @Test
    fun anInterestFreePlanSplitsThePrincipalEvenly() {
        val monthly = EmiSchedule.monthlyInstallment(
            principal = Money.fromMajor(1200L),
            annualInterestRate = 0.0,
            tenureYears = 1.0
        )
        assertEquals(Money.fromMajor(100L), monthly)
    }

    @Test
    fun anInterestBearingPlanCostsMoreThanThePrincipal() {
        val principal = Money.fromMajor(100_000L)
        val monthly = EmiSchedule.monthlyInstallment(
            principal = principal,
            annualInterestRate = 12.0,
            tenureYears = 1.0
        )
        val total = EmiSchedule.totalPayable(monthly, 1.0)

        assertTrue(total > principal, "interest should make the total exceed the principal")
        // 100,000 at 12% over 12 months is 8,884.88 a month by the standard formula.
        assertEquals(888_488L, monthly.minorUnits)
    }

    @Test
    fun suggestionsNeedEnoughInputToBeMeaningful() {
        assertNull(EmiSchedule.suggest(null, 10.0, 1.0))
        assertNull(EmiSchedule.suggest(Money.ZERO, 10.0, 1.0))
        assertNull(EmiSchedule.suggest(Money.fromMajor(1000L), 10.0, 0.0))

        val suggestion = EmiSchedule.suggest(Money.fromMajor(1200L), 0.0, 1.0)
        assertEquals(12, suggestion?.installments)
        assertEquals(Money.fromMajor(100L), suggestion?.monthlyAmount)
        assertEquals(Money.fromMajor(1200L), suggestion?.totalPayable)
    }

    @Test
    fun monthlyDueTotalsOnlyCountInstallmentsFallingInThatMonth() {
        val emis = listOf(
            // Next due 2026-09-01.
            emi(startDate = "2026-01-01", tenureYears = 5.0, monthlyMajor = 200, installmentsPaid = 7),
            // Next due 2026-11-01, outside the month.
            emi(startDate = "2026-01-01", tenureYears = 5.0, monthlyMajor = 500, installmentsPaid = 9),
            // Finished, so nothing is due.
            emi(tenureYears = 1.0, installmentsPaid = 12, amountPaidMajor = 1200)
        )
        assertEquals(Money.fromMajor(200L), FinancialCalculator.emiDueInMonth(emis, "2026-09"))
    }
}
