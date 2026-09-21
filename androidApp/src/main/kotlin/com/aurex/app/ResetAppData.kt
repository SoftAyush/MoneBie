package com.aurex.app

import android.content.Context
import android.content.Intent
import com.aurex.app.data.database.AUREX_DATABASE_NAME
import com.aurex.app.notifications.PaymentReminderSettings
import com.aurex.app.notifications.PaymentReminders
import com.aurex.app.notifications.RecurringPosting
import com.aurex.app.security.AppLockSettings
import com.aurex.app.ui.theme.ThemeStartupCache

/**
 * Wipes Aurex back to a fresh install.
 *
 * The database file is deleted rather than emptied table by table. Emptying it
 * would leave the built-in categories and the currency list to be re-seeded by
 * hand, and those are seeded in exactly one place - the schema - which only
 * runs when the file is created. Deleting the file lets that single seed do the
 * work, so a reset lands on precisely the state a new install lands on, with no
 * third copy of the seed to keep in step.
 *
 * The cost is that the process has to restart: the open driver, every
 * repository holding it and every view model observing them all point at a file
 * that no longer exists. Rebuilding that graph in place would leave a screen
 * somewhere reading from a dead connection, so the app is relaunched instead -
 * which is honest for an action the user has just confirmed will clear
 * everything.
 */
object ResetAppData {

    /**
     * Deletes everything and relaunches. Does not return in the normal case.
     *
     * Returns false only when the database could not be deleted, in which case
     * nothing has been restarted and the caller can report the failure rather
     * than leaving the user looking at a screen that lied about wiping.
     */
    fun wipeAndRestart(context: Context): Boolean {
        val app = context.applicationContext

        val deleted = runCatching { app.deleteDatabase(AUREX_DATABASE_NAME) }.getOrDefault(false)
        if (!deleted && databaseStillExists(app)) return false

        // The theme is mirrored outside the database, so it has to be cleared
        // separately or the fresh install would open in the old user's palette.
        ThemeStartupCache.clear(app)

        // Same for reminders, and with a queue of their own to stop: a daily
        // check left running against a deleted database is work that can only
        // ever find nothing.
        PaymentReminders.cancel(app)
        PaymentReminderSettings.clear(app)
        RecurringPosting.cancel(app)

        // And the lock: a fresh install has no PIN, and leaving this one behind
        // would lock the next user out of an empty app.
        AppLockSettings.clear(app)

        val intent = Intent(app, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        app.startActivity(intent)

        // The activity start is already queued with the system, so killing the
        // process here brings it back on the fresh file rather than ending it.
        Runtime.getRuntime().exit(0)
        return true
    }

    private fun databaseStillExists(context: Context): Boolean =
        context.getDatabasePath(AUREX_DATABASE_NAME).exists()
}
