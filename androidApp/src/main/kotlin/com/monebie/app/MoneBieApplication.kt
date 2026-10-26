package com.aurex.app

import android.app.Application
import com.aurex.app.core.AppContainer
import com.aurex.app.data.backup.FirestoreBackupStore
import com.aurex.app.data.database.DatabaseDriverFactory
import com.aurex.app.notifications.PaymentReminders
import com.aurex.app.notifications.RecurringPosting
import com.aurex.app.security.AppLock
import com.aurex.app.ui.theme.ThemeStartupCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AurexApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    /** Lives as long as the process; only ever used to keep the startup cache honest. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val driverFactory = DatabaseDriverFactory(this)
        val driver = driverFactory.createDriver()
        // FirebaseApp is already initialised here: its ContentProvider runs
        // between attachBaseContext and onCreate.
        appContainer = AppContainer(driver, FirestoreBackupStore())

        // Before any activity: a cold start into a locked app must be locked
        // before the first frame, not after the dashboard has drawn.
        AppLock.onProcessStart(this)

        mirrorThemeChoice()
        PaymentReminders.rescheduleIfEnabled(this)
        catchUpRecurring()
        RecurringPosting.schedule(this)
    }

    /**
     * Posts anything that fell due while the app was closed.
     *
     * Run at launch as well as daily: the daily worker keeps the ledger current
     * on a phone that is rarely opened, and this keeps it current on one that
     * is opened before the worker has had its turn.
     */
    private fun catchUpRecurring() {
        applicationScope.launch {
            runCatching { appContainer.postDueRecurringUseCase() }
        }
    }

    /**
     * Keeps the synchronous copy of the theme choice up to date.
     *
     * Mirrored from the database rather than written at the point the user taps,
     * so it stays correct however the setting comes to change - and so the
     * screen that changes it has nothing extra to remember to do.
     */
    private fun mirrorThemeChoice() {
        val context = this
        applicationScope.launch {
            appContainer.getThemeModeUseCase().collect { mode ->
                ThemeStartupCache.write(context, mode)
            }
        }
    }
}
