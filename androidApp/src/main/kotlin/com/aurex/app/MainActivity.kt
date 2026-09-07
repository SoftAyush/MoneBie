package com.aurex.app

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.aurex.app.ui.App
import com.aurex.app.ui.theme.AurexBackgroundDark
import com.aurex.app.ui.theme.AurexBackgroundLight
import com.aurex.app.ui.theme.ThemeStartupCache

class MainActivity : ComponentActivity() {

    /**
     * Whether a payment reminder is waiting to be opened.
     *
     * Held as state rather than read off the intent inside the composition: the
     * intent is a fact about how the activity was started, and the screen has to
     * be able to say it has acted on it - otherwise every recomposition would
     * navigate to the reminder screen again.
     */
    private val pendingReminders = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        /*
         * The palette is settled before anything is drawn.
         *
         * The theme resource can only carry one answer, chosen at build time,
         * so it follows the phone - which is wrong for a user who asked for the
         * opposite. Repainting the window here, from the cached choice, covers
         * every frame from this point until Compose takes over, and the same
         * value seeds Compose so its first frame agrees rather than correcting
         * itself once the database answers.
         */
        val startupMode = ThemeStartupCache.read(this)
        val startupDark = ThemeStartupCache.resolveDark(this, startupMode)

        window.setBackgroundDrawable(
            ColorDrawable(
                (if (startupDark) AurexBackgroundDark else AurexBackgroundLight).toArgb()
            )
        )

        pendingReminders.value = intent?.getBooleanExtra(EXTRA_OPEN_REMINDERS, false) == true

        setContent {
            App(
                initialThemeMode = startupMode,
                openReminders = pendingReminders.value,
                onRemindersOpened = { pendingReminders.value = false }
            )
        }
    }

    /** A reminder tapped while Aurex is already open arrives here, not in onCreate. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_REMINDERS, false)) {
            pendingReminders.value = true
        }
    }

    companion object {
        /** Set by a payment reminder, to land on the screen it is about. */
        const val EXTRA_OPEN_REMINDERS = "open_reminders"
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
