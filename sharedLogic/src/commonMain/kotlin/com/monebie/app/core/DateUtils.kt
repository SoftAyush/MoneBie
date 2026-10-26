package com.monebie.app.core

/**
 * Dates are stored as sortable ISO-like text, matching the convention already
 * used by the `User` table:
 *  - timestamps: `yyyy-MM-dd HH:mm:ss`
 *  - calendar days: `yyyy-MM-dd`
 *
 * Text keeps every value lexicographically sortable and range-queryable in
 * SQLite (`WHERE date >= ? AND date < ?`) without a per-platform date type.
 */

/** Current timestamp, `yyyy-MM-dd HH:mm:ss`. */
expect fun getCurrentDateString(): String

/** Today as a calendar day, `yyyy-MM-dd`. */
expect fun getCurrentDay(): String

/** Shifts a `yyyy-MM-dd` day by [months], clamping to the last valid day of the target month. */
expect fun addMonths(day: String, months: Int): String

/** Shifts a `yyyy-MM-dd` day by [days]. */
expect fun addDays(day: String, days: Int): String

/** Whole days from [startDay] to [endDay]; negative when [endDay] is in the past. */
expect fun daysBetween(startDay: String, endDay: String): Long

/** `2026-09-04` -> `4 Sep 2026`, always AD whatever [AppCalendar] says. */
expect fun formatGregorianDate(day: String): String

/** `2026-09-04` -> `September 2026`, always AD whatever [AppCalendar] says. */
expect fun formatGregorianMonthYear(day: String): String

/**
 * A day as the user reads dates: `4 Sep 2026`, or `19 Bhadra 2083` in BS.
 * Days outside the BS table fall back to AD.
 */
fun formatDisplayDate(day: String): String =
    if (AppCalendar.current == CalendarSystem.BS) {
        formatNepaliDate(day) ?: formatGregorianDate(day)
    } else {
        formatGregorianDate(day)
    }

/** The month containing [day] as the user reads months: `September 2026` or `Bhadra 2083`. */
fun formatMonthYear(day: String): String = formatMonth(monthOf(day))

/** `2026-09-04` -> `Thu`. */
expect fun formatWeekday(day: String): String

/** Hour of day in `0..23`, used only to pick the dashboard greeting. */
expect fun getCurrentHour(): Int

/** The calendar day part of a timestamp. */
fun dayOf(timestamp: String): String = timestamp.take(10)

/**
 * The key of the month containing a timestamp or day, in the user's calendar:
 * `2026-09` for AD, `BS-2083-05` for BS. See AppCalendar for the format.
 */
fun monthOf(timestamp: String): String = monthKeyOf(timestamp, AppCalendar.current)

/** First day of the month containing [day], as a `yyyy-MM-dd` AD day. */
fun startOfMonth(day: String): String = firstDayOfMonth(monthOf(day))

/** Whether [timestamp] falls on the calendar day [day]. */
fun isOnDay(timestamp: String, day: String): Boolean = dayOf(timestamp) == day

/** Whether [timestamp] falls inside the month keyed [month], in that month's own calendar. */
fun isInMonth(timestamp: String, month: String): Boolean {
    val day = dayOf(timestamp)
    return day >= firstDayOfMonth(month) && day < firstDayAfterMonth(month)
}

/** Human relative label for a due date, e.g. `Overdue by 3d`, `Today`, `In 12d`. */
fun relativeDueLabel(daysUntilDue: Long): String = when {
    daysUntilDue < -1L -> "Overdue by ${-daysUntilDue}d"
    daysUntilDue == -1L -> "Overdue by 1d"
    daysUntilDue == 0L -> "Due today"
    daysUntilDue == 1L -> "Tomorrow"
    daysUntilDue <= 30L -> "In ${daysUntilDue}d"
    else -> "In ${daysUntilDue / 30L}mo"
}

/** Converts a picker's epoch-millis selection to a `yyyy-MM-dd` day. */
expect fun epochMillisToDay(millis: Long): String

/** Converts a `yyyy-MM-dd` day to epoch millis, for seeding a date picker. */
expect fun dayToEpochMillis(day: String): Long

/** First day of a month key, as a `yyyy-MM-dd` AD day. */
fun firstDayOfMonth(month: String): String = firstDayOfMonthKey(month)

/**
 * The day after a month key ends, as a `yyyy-MM-dd` AD day: the exclusive end
 * for a `date >= start AND date < end` range query over the month.
 */
fun firstDayAfterMonth(month: String): String = firstDayOfMonth(shiftMonth(month, 1))

/** Shifts a month key by [delta] months, staying in the key's calendar. */
fun shiftMonth(month: String, delta: Int): String = shiftMonthKey(month, delta)

/** A month key for display: `September 2026` or `Bhadra 2083`. */
fun formatMonth(month: String): String = formatMonthKey(month)

/**
 * Builds the stored timestamp for a transaction dated [day].
 *
 * Entries dated today keep the real clock time so same-day transactions stay in
 * the order they were recorded; back-dated entries land at midday, which sorts
 * them predictably without pretending to a precision the user never gave.
 */
fun timestampForDay(day: String): String =
    if (day == getCurrentDay()) getCurrentDateString() else "$day 12:00:00"
