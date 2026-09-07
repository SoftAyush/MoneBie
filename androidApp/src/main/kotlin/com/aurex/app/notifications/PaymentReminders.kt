package com.aurex.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Reminders for what has to be paid.
 *
 * A debt or an EMI instalment is announced three times: a week out, three days
 * out, and on the morning it is due. The week is long enough to move money, the
 * three days is the nudge for anyone who read the first one and forgot, and the
 * day itself is the one that stops a payment being late by an afternoon.
 *
 * Only money going *out* is announced. A personal loan the user is waiting to
 * be repaid has a due date too, but it is not something they can miss.
 */
object PaymentReminders {

    const val CHANNEL_ID = "aurex_payment_reminders"

    /** How many days before the due date a reminder is posted. */
    val OFFSETS = listOf(7L, 3L, 0L)

    private const val DAILY_WORK = "aurex_payment_reminders_daily"
    private const val IMMEDIATE_WORK = "aurex_payment_reminders_now"

    /**
     * The hour the daily check runs.
     *
     * Morning, so a payment due today is announced while the banks are still a
     * realistic option rather than at the end of the day it was due.
     */
    private const val RUN_AT_HOUR = 9

    /**
     * Creates the channel the reminders post to.
     *
     * Safe to call on every launch - creating a channel that exists is a no-op,
     * and the user's own changes to its importance survive it.
     */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Payment reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Debts and EMIs, a week before, three days before, and on the day."
        }

        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    /**
     * Starts the daily check, and runs one straight away.
     *
     * The immediate run matters: someone switching reminders on today with an
     * instalment due in three days should hear about it now, not tomorrow
     * morning - by which point the three-day warning has become a two-day one.
     */
    fun enable(context: Context) {
        PaymentReminderSettings.setEnabled(context, true)
        ensureChannel(context)
        schedule(context, ExistingPeriodicWorkPolicy.UPDATE)

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<PaymentReminderWorker>().build()
        )
    }

    fun disable(context: Context) {
        PaymentReminderSettings.setEnabled(context, false)
        cancel(context)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(DAILY_WORK)
            cancelUniqueWork(IMMEDIATE_WORK)
        }
    }

    /**
     * Re-arms the daily check on launch.
     *
     * WorkManager keeps periodic work across reboots and updates on its own, so
     * this is only here for the installation that has the setting on but lost
     * its queue - a restore from backup, or a "clear data" that left the
     * preference behind.
     */
    fun rescheduleIfEnabled(context: Context) {
        if (!PaymentReminderSettings.isEnabled(context)) return
        ensureChannel(context)
        schedule(context, ExistingPeriodicWorkPolicy.KEEP)
    }

    private fun schedule(context: Context, policy: ExistingPeriodicWorkPolicy) {
        val request = PeriodicWorkRequestBuilder<PaymentReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNextRun(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(DAILY_WORK, policy, request)
    }

    /**
     * How long until the next [RUN_AT_HOUR] in the phone's own time zone.
     *
     * Computed against the local calendar rather than as a fixed offset from
     * now, so the check keeps landing in the morning instead of drifting to
     * whenever the user happened to switch it on.
     */
    private fun millisUntilNextRun(now: Long = System.currentTimeMillis()): Long {
        val next = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, RUN_AT_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis - now
    }
}
