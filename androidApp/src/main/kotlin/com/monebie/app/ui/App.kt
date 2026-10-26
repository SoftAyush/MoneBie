package com.monebie.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.monebie.app.MoneBieApplication
import com.monebie.app.domain.model.ThemeMode
import com.monebie.app.presentation.main.MainViewModel
import com.monebie.app.security.AppLock
import com.monebie.app.ui.components.MoneBieLoadingScreen
import com.monebie.app.ui.navigation.Main
import com.monebie.app.ui.navigation.MainShell
import com.monebie.app.ui.screens.setup.AccountSetupScreen
import com.monebie.app.ui.screens.lock.LockScreen
import com.monebie.app.ui.screens.onboarding.OnboardingScreen
import com.monebie.app.ui.screens.setup.ProfileSetupScreen
import com.monebie.app.ui.theme.MoneBieTheme
import kotlinx.serialization.Serializable

@Serializable object Onboarding
@Serializable object ProfileSetup
@Serializable object AccountSetup

@Composable
@Preview
fun App(
    /** The cached choice, so the first composed frame is already the right one. */
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
    /** A payment reminder was tapped, so open the screen it is about. */
    openReminders: Boolean = false,
    onRemindersOpened: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneBieApplication

    val viewModel: MainViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                MainViewModel(
                    getUsersUseCase = app.appContainer.getUsersUseCase,
                    hasAccountUseCase = app.appContainer.hasAccountUseCase,
                    getThemeModeUseCase = app.appContainer.getThemeModeUseCase,
                    initialThemeMode = initialThemeMode
                )
            }
        }
    )

    val isUserCreated by viewModel.isUserCreated.collectAsStateWithLifecycle()
    val isAccountSetup by viewModel.isAccountSetup.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    // Resolved once, here, and inherited by every nested MoneBieTheme call: the
    // user's choice has to outrank the phone wherever the theme is applied.
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MoneBieTheme(darkTheme = darkTheme) {
        val isLocked by AppLock.isLocked

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // The gate covers the pixels; this covers the semantics.
                    // Without it a screen reader - or anything else using the
                    // accessibility tree - could read every balance straight
                    // through the PIN pad.
                    .then(if (isLocked) Modifier.clearAndSetSemantics { } else Modifier)
            ) {
                AppContent(
                    isUserCreated = isUserCreated,
                    isAccountSetup = isAccountSetup,
                    openReminders = openReminders,
                    onRemindersOpened = onRemindersOpened
                )
            }

            // Drawn over the content rather than in place of it, so the screen
            // the user was on - and the back stack behind it - is still there
            // once the PIN goes in.
            if (isLocked) {
                LockScreen(onUnlocked = AppLock::unlock, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun AppContent(
    isUserCreated: Boolean?,
    isAccountSetup: Boolean?,
    openReminders: Boolean,
    onRemindersOpened: () -> Unit
) {
    /*
     * The start destination is latched to the first fully-resolved reading
     * and never recomputed.
     *
     * Both flags change while the user moves through setup - creating the
     * profile flips isUserCreated, finishing setup flips isAccountSetup - and
     * NavHost rebuilds its graph whenever startDestination changes, which
     * would reset the back stack mid-flow. Navigation after launch is
     * explicit, so latching is both correct and stable.
     */
    var startDestination by remember { mutableStateOf<Any?>(null) }

    val resolvedUser = isUserCreated
    val resolvedAccount = isAccountSetup
    if (startDestination == null && resolvedUser != null && resolvedAccount != null) {
        startDestination = when {
            resolvedUser && resolvedAccount -> Main
            resolvedUser -> AccountSetup
            else -> Onboarding
        }
    }

    val destination = startDestination
    if (destination == null) {
        // Still reading the database; a branded placeholder beats a blank frame.
        MoneBieLoadingScreen(modifier = Modifier.fillMaxSize())
        return
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = destination,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        composable<Onboarding> {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(ProfileSetup) { launchSingleTop = true }
                }
            )
        }

        composable<ProfileSetup> {
            ProfileSetupScreen(
                onFinished = {
                    navController.navigate(AccountSetup) { launchSingleTop = true }
                }
            )
        }

        composable<AccountSetup> {
            AccountSetupScreen(
                onFinished = {
                    navController.navigate(Main) {
                        // This clears the entire backstack up to the start destination
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<Main> {
            MainShell(
                openReminders = openReminders,
                onRemindersOpened = onRemindersOpened
            )
        }
    }
}
