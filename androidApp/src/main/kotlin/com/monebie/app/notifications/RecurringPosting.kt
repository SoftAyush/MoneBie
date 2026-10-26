package com.aurex.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aurex.app.AurexApplication
import com.aurex.app.MainActivity
import com.aurex.app.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * The daily pass that posts recurring transactions.
 *
 * Always scheduled, with no setting behind it: a schedule the user created is
 * a request for it to run, and the pass costs nothing on a day with nothing
 * due. Runs in the early morning so a salary dated the 1st is in the ledger
 * before the user opens the app to check it.
 */
object RecurringPosting {

    const val CHANNEL_ID = "aurex_recurring"

    private const val DAILY_WORK = "aurex_recurring_daily"
    private const val RUN_AT_HOUR = 6

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecurringPostingWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNextRun(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(DAILY_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DAILY_WORK)
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Recurring transactions",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "A quiet note when a scheduled transaction is recorded for you."
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

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

class RecurringPostingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AurexApplication ?: return Result.success()
        val result = runCatching { app.appContainer.postDueRecurringUseCase() }
            .getOrElse { return Result.retry() }

        // Told, quietly, only when something was written. A ledger changing on
        // its own with no word about it is how trust in the numbers is lost.
        if (!result.isEmpty && canPost(applicationContext)) {
            RecurringPosting.ensureChannel(applicationContext)
            runCatching {
                NotificationManagerCompat.from(applicationContext).notify(
                    NOTIFICATION_ID,
                    build(applicationContext, result.postedCount, result.postedNames)
                )
            }
        }
        return Result.success()
    }

    private fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun build(context: Context, count: Int, names: List<String>) =
        NotificationCompat.Builder(context, RecurringPosting.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                if (count == 1) "1 recurring transaction recorded" else "$count recurring transactions recorded"
            )
            .setContentText(names.distinct().joinToString(", "))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    NOTIFICATION_ID,
                    Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private companion object {
        const val NOTIFICATION_ID = 7_110
    }
}
