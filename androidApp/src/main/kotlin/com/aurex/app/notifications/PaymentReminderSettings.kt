package com.aurex.app.notifications

import android.content.Context

/**
 * Whether payment reminders are on, and which ones have already been sent.
 *
 * Kept outside the database on purpose. Both facts are about this installation
 * rather than about the user's finances: a restored backup should not start
 * posting notifications on a new phone because the old one had them switched
 * on, and it certainly should not carry over which reminders that phone had
 * already shown.
 */
object PaymentReminderSettings {

    private const val FILE = "aurex_reminders"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_SENT = "sent"

    /**
     * Off until asked for.
     *
     * Notifications need the user's permission from Android 13 on, and turning
     * them on by default would mean a permission prompt on first launch for a
     * feature nobody has asked for yet.
     */
    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * The reminders already posted, as `<payment id>|<due date>|<days before>`.
     *
     * The check runs daily but a due date can sit in range for a whole day, and
     * a reminder that arrives twice for the same instalment reads as a bug in
     * the arithmetic. Keyed by due date as well as id, so an EMI that rolls to
     * its next instalment is a new obligation rather than one already announced.
     */
    fun alreadySent(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_SENT, emptySet()).orEmpty()

    /**
     * Records what was posted, dropping anything for a date now in the past.
     *
     * Without the prune this set is the one thing in the app that only ever
     * grows - a reminder for an instalment paid two years ago cannot fire
     * again, so keeping its key buys nothing.
     */
    fun recordSent(context: Context, keys: Set<String>, today: String) {
        val kept = (alreadySent(context) + keys).filterTo(mutableSetOf()) { key ->
            key.dueDatePart()?.let { it >= today } ?: false
        }
        prefs(context).edit().putStringSet(KEY_SENT, kept).apply()
    }

    /** Forgets everything, for a wipe back to a fresh install. */
    fun clear(context: Context) {
        prefs(context).edit().clear().commit()
    }

    fun keyFor(paymentId: String, dueDate: String, daysBefore: Long): String =
        paymentId + "|" + dueDate + "|" + daysBefore

    // Dates are stored as yyyy-MM-dd, so they compare correctly as strings.
    private fun String.dueDatePart(): String? = split("|").getOrNull(1)

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
