package com.monebie.app.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.monebie.app.core.AppCalendar
import com.monebie.app.core.CalendarSystem

/**
 * Persists the AD/BS choice made in Settings.
 *
 * The live value is [AppCalendar] in shared logic, which every month
 * calculation and date label reads; this only keeps it across restarts.
 */
object DateCalendarSettings {

    private const val FILE = "monebie_calendar"
    private const val KEY_CALENDAR = "calendar"

    /** Restores the saved choice. Called once at process start, before any screen. */
    fun load(context: Context) {
        val stored = prefs(context).getString(KEY_CALENDAR, null)
        AppCalendar.set(CalendarSystem.entries.firstOrNull { it.name == stored } ?: CalendarSystem.AD)
    }

    fun set(context: Context, calendar: CalendarSystem) {
        AppCalendar.set(calendar)
        prefs(context).edit().putString(KEY_CALENDAR, calendar.name).apply()
    }

    /** Forgets the choice, for a wipe back to a fresh install. */
    fun clear(context: Context) {
        prefs(context).edit().clear().commit()
        AppCalendar.set(CalendarSystem.AD)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}

/** The calendar dates are read in, recomposing the caller when it changes. */
@Composable
fun currentCalendar(): CalendarSystem = AppCalendar.system.collectAsState().value
