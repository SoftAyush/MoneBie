package com.aurex.app.core

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.localeWithLocaleIdentifier
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.timeIntervalSinceDate

private const val TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss"
private const val DAY_PATTERN = "yyyy-MM-dd"

// en_US_POSIX keeps the fixed patterns above stable regardless of the device locale.
private fun formatter(pattern: String) = NSDateFormatter().apply {
    dateFormat = pattern
    locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
}

private fun parseDay(day: String): NSDate? = formatter(DAY_PATTERN).dateFromString(day)

actual fun getCurrentDateString(): String = formatter(TIMESTAMP_PATTERN).stringFromDate(NSDate())

actual fun getCurrentDay(): String = formatter(DAY_PATTERN).stringFromDate(NSDate())

actual fun addMonths(day: String, months: Int): String {
    val date = parseDay(day) ?: return day
    val components = NSDateComponents().apply { setMonth(months.toLong()) }
    val shifted = NSCalendar.currentCalendar.dateByAddingComponents(components, date, 0u) ?: return day
    return formatter(DAY_PATTERN).stringFromDate(shifted)
}

actual fun addDays(day: String, days: Int): String {
    val date = parseDay(day) ?: return day
    val components = NSDateComponents().apply { setDay(days.toLong()) }
    val shifted = NSCalendar.currentCalendar.dateByAddingComponents(components, date, 0u) ?: return day
    return formatter(DAY_PATTERN).stringFromDate(shifted)
}

actual fun daysBetween(startDay: String, endDay: String): Long {
    val start = parseDay(startDay) ?: return 0L
    val end = parseDay(endDay) ?: return 0L
    return (end.timeIntervalSinceDate(start) / 86_400.0).toLong()
}

actual fun formatDisplayDate(day: String): String {
    val date = parseDay(day) ?: return day
    return formatter("d MMM yyyy").stringFromDate(date)
}

actual fun formatMonthYear(day: String): String {
    val date = parseDay(day) ?: return day
    return formatter("MMMM yyyy").stringFromDate(date)
}

actual fun formatWeekday(day: String): String {
    val date = parseDay(day) ?: return day
    return formatter("EEE").stringFromDate(date)
}

actual fun getCurrentHour(): Int =
    NSCalendar.currentCalendar.components(NSCalendarUnitHour, NSDate()).hour.toInt()

actual fun epochMillisToDay(millis: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(millis / 1000.0)
    return formatter(DAY_PATTERN).stringFromDate(date)
}

actual fun dayToEpochMillis(day: String): Long {
    val date = parseDay(day) ?: NSDate()
    return (date.timeIntervalSince1970 * 1000.0).toLong()
}
