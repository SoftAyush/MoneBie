package com.monebie.app.ui.theme

import android.content.Context
import android.content.res.Configuration
import com.monebie.app.domain.model.ThemeMode
import com.monebie.app.domain.model.themeModeOf

/**
 * The chosen palette, mirrored where it can be read synchronously.
 *
 * The real setting lives in the database, but the database is read through a
 * coroutine and the window needs a colour before any of that can finish. Every
 * frame between the activity opening and the first flow emission would
 * otherwise be painted from the phone's setting - a white flash on the way into
 * an app the user has asked to keep dark, and the reverse.
 *
 * The database stays the source of truth. This is a cache that follows it, kept
 * current by [com.monebie.app.MoneBieApplication], and only ever read at startup.
 */
object ThemeStartupCache {

    private const val FILE = "monebie_startup"
    private const val KEY_THEME_MODE = "theme_mode"

    fun read(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return themeModeOf(prefs.getString(KEY_THEME_MODE, null))
    }

    fun write(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

    /**
     * Forgets the choice, for a wipe back to a fresh install.
     *
     * Committed rather than applied: the caller ends the process immediately
     * afterwards, and an asynchronous write would not survive it.
     */
    fun clear(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    /** Resolves a mode against the phone, for the frames drawn before Compose runs. */
    fun resolveDark(context: Context, mode: ThemeMode): Boolean = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> {
            val night = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
            night == Configuration.UI_MODE_NIGHT_YES
        }
    }
}
