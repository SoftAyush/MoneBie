package com.monebie.app.core

/**
 * A day in the Bikram Sambat calendar, the one most people in Nepal date things by.
 *
 * Only ever a view: storage stays on the Gregorian `yyyy-MM-dd` day described in
 * DateUtils, so sorting, range queries and backups are untouched by which
 * calendar the user happens to read dates in.
 *
 * [month] is 1-based, Baisakh = 1.
 */
data class BsDate(val year: Int, val month: Int, val day: Int)

/**
 * Converts between Gregorian days and Bikram Sambat.
 *
 * BS month lengths are set by the almanac each year rather than by a rule, so
 * they come from a table: the one published with the `nepali-date-converter`
 * package (3.4.0), covering 2000-2090 BS (April 1943 to April 2034). Dates
 * outside that span have no BS form and callers fall back to AD.
 */
object NepaliCalendar {

    const val MIN_YEAR = 2000
    const val MAX_YEAR = 2090

    /** 1 Baisakh 2000 BS (14 Apr 1943), as a day count from 1970-01-01. */
    private val EPOCH_DAY = epochDay(1943, 4, 14)

    /** Weekday of 1 Baisakh 2000, 0 = Sunday. */
    private const val EPOCH_WEEKDAY = 3

    val monthNames = listOf(
        "Baisakh", "Jestha", "Asar", "Shrawan", "Bhadra", "Ashwin",
        "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra"
    )

    val monthNamesNepali = listOf(
        "बैशाख", "जेठ", "असार", "साउन", "भदौ", "असोज",
        "कार्तिक", "मंसिर", "पुस", "माघ", "फागुन", "चैत"
    )

    private val monthDays: Array<IntArray> = arrayOf(
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2000
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2001
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2002
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2003
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2004
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2005
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2006
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2007
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31), // 2008
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2009
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2010
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2011
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30), // 2012
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2013
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2014
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2015
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30), // 2016
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2017
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2018
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2019
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30), // 2020
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2021
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30), // 2022
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2023
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30), // 2024
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2025
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2026
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2027
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2028
        intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30), // 2029
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2030
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2031
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2032
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2033
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2034
        intArrayOf(30, 32, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31), // 2035
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2036
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2037
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2038
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30), // 2039
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2040
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2041
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2042
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30), // 2043
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2044
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2045
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2046
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30), // 2047
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2048
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30), // 2049
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2050
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30), // 2051
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2052
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30), // 2053
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2054
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2055
        intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30), // 2056
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2057
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2058
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2059
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2060
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2061
        intArrayOf(30, 32, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31), // 2062
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2063
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2064
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2065
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31), // 2066
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2067
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2068
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2069
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30), // 2070
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2071
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2072
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2073
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30), // 2074
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2075
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30), // 2076
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2077
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30), // 2078
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2079
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30), // 2080
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2081
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2082
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2083
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2084
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2085
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2086
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 30, 29, 30, 30, 30), // 2087
        intArrayOf(30, 31, 32, 32, 30, 31, 30, 30, 29, 30, 30, 30), // 2088
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 30, 30), // 2089
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 30, 30)  // 2090
    )

    fun isSupportedYear(year: Int): Boolean = year in MIN_YEAR..MAX_YEAR

    fun daysInMonth(year: Int, month: Int): Int = monthDays[year - MIN_YEAR][month - 1]

    /** The BS date for a `yyyy-MM-dd` day, or null when it falls outside the table. */
    fun fromAd(day: String): BsDate? {
        val parts = day.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return null
        var remaining = epochDay(parts[0], parts[1], parts[2]) - EPOCH_DAY
        if (remaining < 0) return null

        for (yearIndex in monthDays.indices) {
            val months = monthDays[yearIndex]
            val yearLength = months.sum()
            if (remaining >= yearLength) {
                remaining -= yearLength
                continue
            }
            for (monthIndex in months.indices) {
                if (remaining < months[monthIndex]) {
                    return BsDate(MIN_YEAR + yearIndex, monthIndex + 1, remaining.toInt() + 1)
                }
                remaining -= months[monthIndex]
            }
        }
        return null
    }

    /** The `yyyy-MM-dd` day for a BS date inside the table. */
    fun toAd(date: BsDate): String = dayFromEpochDay(EPOCH_DAY + daysSinceEpoch(date))

    /** Weekday of a BS date, 0 = Sunday, for laying out a month grid. */
    fun weekday(date: BsDate): Int = (EPOCH_WEEKDAY + daysSinceEpoch(date)) % 7

    private fun daysSinceEpoch(date: BsDate): Int {
        require(isSupportedYear(date.year)) { "BS year ${date.year} is outside $MIN_YEAR-$MAX_YEAR" }
        var days = 0
        for (yearIndex in 0 until date.year - MIN_YEAR) days += monthDays[yearIndex].sum()
        val months = monthDays[date.year - MIN_YEAR]
        for (monthIndex in 0 until date.month - 1) days += months[monthIndex]
        return days + date.day - 1
    }
}

/** `2026-10-14` -> `28 Ashwin 2083`, or null outside the supported BS years. */
fun formatNepaliDate(day: String): String? =
    NepaliCalendar.fromAd(day)?.let { "${it.day} ${NepaliCalendar.monthNames[it.month - 1]} ${it.year}" }

/*
 * Plain proleptic-Gregorian day arithmetic (Howard Hinnant's days_from_civil).
 * Done by hand rather than through the platform date helpers because those work
 * in the device's time zone, where a DST change - or Nepal's own move from
 * +5:30 to +5:45 in 1986 - shortens one "day" by enough to truncate a span
 * spanning decades to one day too few.
 */

private fun epochDay(year: Int, month: Int, day: Int): Long {
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * (month + (if (month > 2) -3 else 9)) + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097L + doe - 719468L
}

private fun dayFromEpochDay(epochDay: Long): String {
    val z = epochDay + 719468L
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = (z - era * 146097).toInt()
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = doy - (153 * mp + 2) / 5 + 1
    val month = if (mp < 10) mp + 3 else mp - 9
    val year = yoe + era.toInt() * 400 + if (month <= 2) 1 else 0
    return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}
