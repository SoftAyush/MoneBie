package com.monebie.app.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.monebie.app.MoneBieApplication
import com.monebie.app.MainActivity
import com.monebie.app.R
import com.monebie.app.core.MoneyFormatter
import com.monebie.app.core.formatDisplayDate
import com.monebie.app.core.getCurrentDay
import com.monebie.app.domain.model.UpcomingPayment
import com.monebie.app.domain.model.UpcomingPaymentKind
import kotlinx.coroutines.flow.first

/**
 * The daily check behind [PaymentReminders].
 *
 * Reads the same projection the reminder screen shows rather than any store of
 * its own, so a debt settled or an instalment paid this morning cannot produce
 * a reminder this afternoon - there is no scheduled notification to go stale,
 * only a question asked again each day.
 */
class PaymentReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!PaymentReminderSettings.isEnabled(context)) return Result.success()

        val app = context as? MoneBieApplication ?: return Result.success()
        val today = getCurrentDay()

        val board = app.appContainer.getUpcomingPaymentsUseCase().first { !it.isLoading }

        val due = board.buckets.values.flatten()
            .filter { it.kind == UpcomingPaymentKind.DEBT || it.kind == UpcomingPaymentKind.EMI }
            .filter { it.daysUntilDue in PaymentReminders.OFFSETS }

        if (due.isEmpty()) return Result.success()

        // Checked here rather than trusted from the setting: the user can take
        // the permission back in Android's own settings at any time, and posting
        // without it throws.
        if (!canPost(context)) return Result.success()

        PaymentReminders.ensureChannel(context)

        val posted = mutableSetOf<String>()
        val manager = NotificationManagerCompat.from(context)

        due.forEach { payment ->
            val key = PaymentReminderSettings.keyFor(
                paymentId = payment.id,
                dueDate = payment.dueDate,
                daysBefore = payment.daysUntilDue
            )
            if (key in PaymentReminderSettings.alreadySent(context)) return@forEach

            runCatching {
                manager.notify(
                    key.hashCode(),
                    build(context, payment, board.currencySymbol)
                )
            }.onSuccess { posted += key }
        }

        if (posted.isNotEmpty()) {
            PaymentReminderSettings.recordSent(context, posted, today)
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

    private fun build(
        context: Context,
        payment: UpcomingPayment,
        currencySymbol: String
    ) = NotificationCompat.Builder(context, PaymentReminders.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title(payment))
        .setContentText(
            MoneyFormatter.format(payment.amount, currencySymbol, showDecimals = false) +
                " · " + payment.subtitle
        )
        .setStyle(
            NotificationCompat.BigTextStyle().bigText(
                MoneyFormatter.format(payment.amount, currencySymbol, showDecimals = false) +
                    " · " + payment.subtitle +
                    "\nDue " + formatDisplayDate(payment.dueDate)
            )
        )
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setAutoCancel(true)
        .setContentIntent(openReminders(context, payment.id))
        .build()

    /** Says what it is and when, in that order, because the tray truncates the end. */
    private fun title(payment: UpcomingPayment): String {
        val kind = if (payment.kind == UpcomingPaymentKind.EMI) "EMI" else "Debt"
        val when_ = when (payment.daysUntilDue) {
            0L -> "is due today"
            1L -> "is due tomorrow"
            else -> "is due in " + payment.daysUntilDue + " days"
        }
        return payment.title + " " + when_ + " · " + kind
    }

    /**
     * Opens the reminder screen rather than wherever the app was left.
     *
     * A notification about a payment that lands the user on the dashboard makes
     * them go and find the thing they were just told about.
     */
    private fun openReminders(context: Context, paymentId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_OPEN_REMINDERS, true)
        }

        return PendingIntent.getActivity(
            context,
            paymentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
