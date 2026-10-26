package com.monebie.app

import com.monebie.app.core.AppCalendar
import com.monebie.app.core.BsDate
import com.monebie.app.core.CalendarSystem
import com.monebie.app.core.NepaliCalendar
import com.monebie.app.core.firstDayAfterMonth
import com.monebie.app.core.firstDayOfMonth
import com.monebie.app.core.formatDisplayDate
import com.monebie.app.core.formatMonth
import com.monebie.app.core.formatNepaliDate
import com.monebie.app.core.isInMonth
import com.monebie.app.core.monthOf
import com.monebie.app.core.shiftMonth
import com.monebie.app.domain.calculation.FinancialCalculator
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Bikram Sambat conversion against new-year days fixed by the almanac, and a
 * full round trip so no day in the table is skipped or doubled.
 */
class NepaliCalendarTest {

    @Test
    fun newYearDaysMatchTheAlmanac() {
        assertEquals(BsDate(2000, 1, 1), NepaliCalendar.fromAd("1943-04-14"))
        assertEquals(BsDate(2081, 1, 1), NepaliCalendar.fromAd("2024-04-13"))
        assertEquals(BsDate(2082, 1, 1), NepaliCalendar.fromAd("2025-04-14"))
        assertEquals(BsDate(2083, 1, 1), NepaliCalendar.fromAd("2026-04-14"))
        assertEquals("2026-04-14", NepaliCalendar.toAd(BsDate(2083, 1, 1)))
    }

    @Test
    fun formatsForDisplay() {
        assertEquals("1 Baisakh 2083", formatNepaliDate("2026-04-14"))
    }

    @Test
    fun weekdayFollowsTheGregorianDay() {
        // 14 Apr 2026 was a Tuesday.
        assertEquals(2, NepaliCalendar.weekday(BsDate(2083, 1, 1)))
    }

    @Test
    fun everyDayInTheTableRoundTrips() {
        for (year in NepaliCalendar.MIN_YEAR..NepaliCalendar.MAX_YEAR) {
            for (month in 1..12) {
                for (day in 1..NepaliCalendar.daysInMonth(year, month)) {
                    val bs = BsDate(year, month, day)
                    assertEquals(bs, NepaliCalendar.fromAd(NepaliCalendar.toAd(bs)))
                }
            }
        }
    }

    @Test
    fun outsideTheTableHasNoBsForm() {
        assertNull(NepaliCalendar.fromAd("1943-04-13"))
        assertNull(NepaliCalendar.fromAd("2034-04-14"))
    }
}

/**
 * Month bucketing in BS: the dashboard, budgets, insights and the transaction
 * list all lean on these helpers, so a BS user's "this month" must run from
 * 1st to last of the Nepali month, not the English one.
 */
class BsMonthTest {

    @AfterTest
    fun backToAd() {
        AppCalendar.set(CalendarSystem.AD)
    }

    @Test
    fun monthKeyFollowsTheChosenCalendar() {
        assertEquals("2026-10", monthOf("2026-10-14 09:30:00"))
        AppCalendar.set(CalendarSystem.BS)
        // 14 Oct 2026 is 28 Ashwin 2083.
        assertEquals("BS-2083-06", monthOf("2026-10-14 09:30:00"))
    }

    @Test
    fun bsMonthSpansItsOwnDays() {
        val ashwin = "BS-2083-06"
        val start = firstDayOfMonth(ashwin)
        val end = firstDayAfterMonth(ashwin)
        assertEquals(BsDate(2083, 6, 1), NepaliCalendar.fromAd(start))
        assertEquals(BsDate(2083, 7, 1), NepaliCalendar.fromAd(end))
        assertTrue(isInMonth("$start 00:00:00", ashwin))
        assertFalse(isInMonth("$end 00:00:00", ashwin))
        assertEquals(NepaliCalendar.daysInMonth(2083, 6), FinancialCalculator.elapsedDaysInMonth(ashwin, "2027-01-01"))
    }

    @Test
    fun shiftingCrossesTheNewYear() {
        assertEquals("BS-2084-01", shiftMonth("BS-2083-12", 1))
        assertEquals("BS-2082-12", shiftMonth("BS-2083-01", -1))
        assertEquals("2026-01", shiftMonth("2025-12", 1))
    }

    @Test
    fun keysKeepTheirCalendarAfterASwitch() {
        // A key made in AD still means the AD month once the user picks BS.
        AppCalendar.set(CalendarSystem.BS)
        assertEquals("2026-10-01", firstDayOfMonth("2026-10"))
        assertEquals("2026-11-01", firstDayAfterMonth("2026-10"))
    }

    @Test
    fun labelsUseTheMonthName() {
        assertEquals("Ashwin 2083", formatMonth("BS-2083-06"))
        AppCalendar.set(CalendarSystem.BS)
        assertEquals("28 Ashwin 2083", formatDisplayDate("2026-10-14"))
    }
}
