package com.monebie.app

import com.monebie.app.core.Money
import com.monebie.app.domain.calculation.FinancialCalculator
import com.monebie.app.domain.model.Debt
import com.monebie.app.domain.model.DebtStatus
import com.monebie.app.domain.model.PaymentFrequency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the derived parts of a debt: the remaining balance, and the status and
 * next-due-date rules that depend on today.
 *
 * These are pure functions taking today as a parameter, so the assertions here
 * are stable rather than passing only on the day they were written.
 */
class DebtCalculationTest {

    private val today = "2026-09-04"

    private fun debt(
        original: Long,
        paid: Long = 0L,
        dueDate: String? = null,
        frequency: PaymentFrequency = PaymentFrequency.MONTHLY,
        installment: Long? = null
    ) = Debt(
        id = 1L,
        name = "Test debt",
        lender = "Test lender",
        originalAmount = Money.fromMajor(original),
        amountPaid = Money.fromMajor(paid),
        startDate = "2026-01-01",
        dueDate = dueDate,
        paymentFrequency = frequency,
        installmentAmount = installment?.let { Money.fromMajor(it) }
    )

    @Test
    fun remainingIsOriginalMinusPaid() {
        assertEquals(Money.fromMajor(700L), debt(original = 1000, paid = 300).remainingAmount)
    }

    @Test
    fun overpaymentNeverProducesANegativeBalance() {
        val overpaid = debt(original = 1000, paid = 1200)
        assertTrue(overpaid.remainingAmount.isZero)
        assertTrue(overpaid.isSettled)
        assertEquals(1f, overpaid.progress)
    }

    @Test
    fun statusFollowsPaymentsAndDueDate() {
        assertEquals(
            DebtStatus.ACTIVE,
            FinancialCalculator.debtStatus(debt(original = 1000), today)
        )
        assertEquals(
            DebtStatus.PARTIALLY_PAID,
            FinancialCalculator.debtStatus(debt(original = 1000, paid = 300), today)
        )
        assertEquals(
            DebtStatus.PAID,
            FinancialCalculator.debtStatus(debt(original = 1000, paid = 1000), today)
        )
    }

    @Test
    fun aSettledDebtIsNeverReportedOverdue() {
        // Fully repaid but with a due date long past: paid must win, or the list
        // would nag about a debt that no longer exists.
        val settled = debt(original = 1000, paid = 1000, dueDate = "2026-01-15")
        assertEquals(DebtStatus.PAID, FinancialCalculator.debtStatus(settled, today))
    }

    @Test
    fun aPastDueDateOnAnUnpaidDebtIsOverdue() {
        val overdue = debt(
            original = 1000,
            dueDate = "2026-08-20",
            frequency = PaymentFrequency.ONE_TIME
        )
        assertEquals(DebtStatus.OVERDUE, FinancialCalculator.debtStatus(overdue, today))
    }

    @Test
    fun aRecurringDueDateRollsForwardInsteadOfStayingStuckInThePast() {
        // Monthly debt whose first due date was months ago: the next payment is
        // the next occurrence, not the original date.
        val recurring = debt(original = 1000, dueDate = "2026-02-10")
        val next = FinancialCalculator.nextDebtDueDate(recurring, today)
        assertEquals("2026-09-10", next)
    }

    @Test
    fun aOneTimeDueDateDoesNotRollForward() {
        val once = debt(
            original = 1000,
            dueDate = "2026-02-10",
            frequency = PaymentFrequency.ONE_TIME
        )
        assertEquals("2026-02-10", FinancialCalculator.nextDebtDueDate(once, today))
    }

    @Test
    fun nextPaymentIsTheInstallmentButNeverMoreThanIsLeft() {
        val withRoom = debt(original = 1000, paid = 0, installment = 250)
        assertEquals(Money.fromMajor(250L), withRoom.nextPaymentAmount)

        // Final installment: only the remainder is due, not a full instalment.
        val almostDone = debt(original = 1000, paid = 900, installment = 250)
        assertEquals(Money.fromMajor(100L), almostDone.nextPaymentAmount)
    }

    @Test
    fun outstandingTotalIgnoresSettledDebts() {
        val debts = listOf(
            debt(original = 1000, paid = 300),
            debt(original = 500, paid = 500),
            debt(original = 250)
        )
        assertEquals(Money.fromMajor(950L), FinancialCalculator.totalDebtOutstanding(debts))
    }
}
