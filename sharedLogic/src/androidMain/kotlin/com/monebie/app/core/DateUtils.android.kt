package com.aurex.app.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss"
private const val DAY_PATTERN = "yyyy-MM-dd"

private fun formatter(pattern: String) = SimpleDateFormat(pattern, Locale.US)

private fun parseDay(day: String): Date? = runCatching { formatter(DAY_PATTERN).parse(day) }.getOrNull()

actual fun getCurrentDateString(): String = formatter(TIMESTAMP_PATTERN).format(Date())

actual fun getCurrentDay(): String = formatter(DAY_PATTERN).format(Date())

actual fun addMonths(day: String, months: Int): String {
    val date = parseDay(day) ?: return day
    val calendar = Calendar.getInstance().apply {
        time = date
        add(Calendar.MONTH, months)
    }
    return formatter(DAY_PATTERN).format(calendar.time)
}

actual fun addDays(day: String, days: Int): String {
    val date = parseDay(day) ?: return day
    val calendar = Calendar.getInstance().apply {
        time = date
        add(Calendar.DAY_OF_YEAR, days)
    }
    return formatter(DAY_PATTERN).format(calendar.time)
}

actual fun daysBetween(startDay: String, endDay: String): Long {
    val start = parseDay(startDay) ?: return 0L
    val end = parseDay(endDay) ?: return 0L
    return TimeUnit.MILLISECONDS.toDays(end.time - start.time)
}

actual fun formatDisplayDate(day: String): String {
    val date = parseDay(day) ?: return day
    return formatter("d MMM yyyy").format(date)
}

actual fun formatMonthYear(day: String): String {
    val date = parseDay(day) ?: return day
    return formatter("MMMM yyyy").format(date)
}

actual fun formatWeekday(day: String): String {
    val date = parseDay(day) ?: return day
    return formatter("EEE").format(date)
}

actual fun getCurrentHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

actual fun epochMillisToDay(millis: Long): String = formatter(DAY_PATTERN).format(Date(millis))

actual fun dayToEpochMillis(day: String): Long = parseDay(day)?.time ?: System.currentTimeMillis()
