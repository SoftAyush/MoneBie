package com.aurex.app.core

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

/** `2026-09-04` -> `4 Sep 2026`. */
expect fun formatDisplayDate(day: String): String

/** `2026-09-04` -> `September 2026`. */
expect fun formatMonthYear(day: String): String

/** `2026-09-04` -> `Thu`. */
expect fun formatWeekday(day: String): String

/** Hour of day in `0..23`, used only to pick the dashboard greeting. */
expect fun getCurrentHour(): Int

/** The calendar day part of a timestamp. */
fun dayOf(timestamp: String): String = timestamp.take(10)

/** The `yyyy-MM` month part of a timestamp or day. */
fun monthOf(timestamp: String): String = timestamp.take(7)

/** First day of the month containing [day]. */
fun startOfMonth(day: String): String = "${monthOf(day)}-01"

/** Whether [timestamp] falls on the calendar day [day]. */
fun isOnDay(timestamp: String, day: String): Boolean = dayOf(timestamp) == day

/** Whether [timestamp] falls inside the `yyyy-MM` month. */
fun isInMonth(timestamp: String, month: String): Boolean = monthOf(timestamp) == month

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

/** First day of a `yyyy-MM` month. */
fun firstDayOfMonth(month: String): String = "$month-01"

/** Shifts a `yyyy-MM` month by [delta] months. */
fun shiftMonth(month: String, delta: Int): String = monthOf(addMonths(firstDayOfMonth(month), delta))

/**
 * Builds the stored timestamp for a transaction dated [day].
 *
 * Entries dated today keep the real clock time so same-day transactions stay in
 * the order they were recorded; back-dated entries land at midday, which sorts
 * them predictably without pretending to a precision the user never gave.
 */
fun timestampForDay(day: String): String =
    if (day == getCurrentDay()) getCurrentDateString() else "$day 12:00:00"
