package com.monebie.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The calendar the user reads dates and months in. Storage is always AD. */
enum class CalendarSystem { AD, BS }

/**
 * The calendar the whole app works in, chosen in Settings.
 *
 * It decides how dates are written and where a month starts and ends, so a
 * Bikram Sambat user's "this month" runs 1 Ashwin to the end of Ashwin for the
 * dashboard, budgets, insights and the transaction list alike. Stored dates do
 * not change: switching back and forth only regroups the same days.
 *
 * Held here rather than passed around so the plain date helpers in DateUtils
 * can follow it; the platform persists the choice and calls [set] at start-up.
 * Flows that bucket by month observe [system] so a switch recomputes them.
 */
object AppCalendar {

    private val _system = MutableStateFlow(CalendarSystem.AD)
    val system: StateFlow<CalendarSystem> = _system.asStateFlow()

    val current: CalendarSystem get() = _system.value

    fun set(system: CalendarSystem) {
        _system.value = system
    }
}

/*
 * Month keys.
 *
 * A month is named by a sortable key: `yyyy-MM` for AD and `BS-yyyy-MM` for
 * BS. The prefix makes a key self-describing, so a key made before the user
 * switched calendars still resolves to the month it meant instead of being
 * read in the other calendar. Keys of the same calendar sort chronologically.
 */

private const val BS_PREFIX = "BS-"

private fun isBsKey(month: String): Boolean = month.startsWith(BS_PREFIX)

private fun bsKey(year: Int, month: Int): String =
    "$BS_PREFIX$year-${month.toString().padStart(2, '0')}"

/** Year and 1-based month of a BS key. */
private fun parseBsKey(month: String): Pair<Int, Int> {
    val parts = month.removePrefix(BS_PREFIX).split("-")
    return parts[0].toInt() to parts[1].toInt()
}

/**
 * The month key of a timestamp or day in [system].
 *
 * Days outside the BS table (before 1943 or after early 2034) keep their AD
 * month, since they have no BS form to group by.
 */
fun monthKeyOf(timestamp: String, system: CalendarSystem): String {
    if (system == CalendarSystem.BS) {
        NepaliCalendar.fromAd(dayOf(timestamp))?.let { return bsKey(it.year, it.month) }
    }
    return timestamp.take(7)
}

internal fun firstDayOfMonthKey(month: String): String {
    if (!isBsKey(month)) return "$month-01"
    val (year, number) = parseBsKey(month)
    return NepaliCalendar.toAd(BsDate(year, number, 1))
}

internal fun shiftMonthKey(month: String, delta: Int): String {
    if (!isBsKey(month)) return addMonths("$month-01", delta).take(7)
    val (year, number) = parseBsKey(month)
    // Clamped to the table, which spans 2000-2090 BS and so is never reached
    // by real paging through a ledger.
    val index = (year * 12 + number - 1 + delta)
        .coerceIn(NepaliCalendar.MIN_YEAR * 12, NepaliCalendar.MAX_YEAR * 12 + 11)
    return bsKey(index / 12, index % 12 + 1)
}

internal fun formatMonthKey(month: String): String {
    if (!isBsKey(month)) return formatGregorianMonthYear("$month-01")
    val (year, number) = parseBsKey(month)
    return "${NepaliCalendar.monthNames[number - 1]} $year"
}
