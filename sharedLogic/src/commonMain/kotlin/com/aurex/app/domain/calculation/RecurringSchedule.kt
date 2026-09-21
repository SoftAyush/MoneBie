package com.aurex.app.domain.calculation

import com.aurex.app.core.addDays
import com.aurex.app.core.addMonths
import com.aurex.app.domain.model.PaymentFrequency
import com.aurex.app.domain.model.periodDays
import com.aurex.app.domain.model.periodMonths

/**
 * Where a recurring schedule lands next.
 *
 * Month-based cycles step from the *anchor* day rather than from the previous
 * occurrence: a rent due on the 31st steps to 28 Feb and then back to 31 Mar,
 * where stepping from the clamped 28th would drift the schedule to the 28th
 * of every month thereafter. Day-based cycles have no such problem and simply
 * add their period.
 */
object RecurringSchedule {

    /**
     * The occurrence after [current].
     *
     * [anchorDay] is the day-of-month the schedule was created on, `1..31`,
     * used to undo the clamping a short month applied.
     */
    fun next(current: String, frequency: PaymentFrequency, anchorDay: Int = dayOfMonth(current)): String {
        frequency.periodDays?.let { return addDays(current, it) }
        val months = frequency.periodMonths ?: return current
        return stepMonths(current, months, anchorDay)
    }

    /**
     * Every occurrence from [nextDue] up to and including [today], oldest
     * first, stopping at [endDate] when there is one.
     *
     * Bounded so a schedule paused for years cannot flood the ledger when it
     * is switched back on: anything past [limit] occurrences is left for the
     * next pass.
     */
    fun occurrencesDue(
        nextDue: String,
        today: String,
        frequency: PaymentFrequency,
        anchorDay: Int,
        endDate: String? = null,
        limit: Int = MAX_OCCURRENCES_PER_PASS
    ): List<String> {
        val due = mutableListOf<String>()
        var cursor = nextDue
        while (cursor <= today && due.size < limit) {
            if (endDate != null && cursor > endDate) break
            due += cursor
            val following = next(cursor, frequency, anchorDay)
            // A one-time or unknown cycle does not advance; posting it once is
            // all that can be done, so stop rather than loop on the same day.
            if (following == cursor) break
            cursor = following
        }
        return due
    }

    /** The day-of-month part of a `yyyy-MM-dd` day, `1..31`. */
    fun dayOfMonth(day: String): Int = day.drop(8).take(2).toIntOrNull() ?: 1

    /**
     * Adds whole months, landing on [anchorDay] where the month has it and on
     * the last day where it does not.
     */
    private fun stepMonths(day: String, months: Int, anchorDay: Int): String {
        // Step from the 1st so the intermediate arithmetic never clamps, then
        // put the anchor day back and let addDays(0) validate nothing - the
        // clamp to the month's length is done by hand below.
        val firstOfTarget = addMonths(day.take(7) + "-01", months)
        val lastDayOfTarget = dayOfMonth(addDays(addMonths(firstOfTarget, 1), -1))
        val landing = anchorDay.coerceIn(1, lastDayOfTarget)
        return firstOfTarget.take(8) + landing.toString().padStart(2, '0')
    }

    const val MAX_OCCURRENCES_PER_PASS = 36
}
