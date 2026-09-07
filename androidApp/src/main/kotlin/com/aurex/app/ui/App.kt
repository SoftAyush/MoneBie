package com.aurex.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aurex.app.AurexApplication
import com.aurex.app.domain.model.ThemeMode
import com.aurex.app.presentation.main.MainViewModel
import com.aurex.app.ui.components.AurexLoadingScreen
import com.aurex.app.ui.navigation.Main
import com.aurex.app.ui.navigation.MainShell
import com.aurex.app.ui.screens.setup.AccountSetupScreen
import com.aurex.app.ui.screens.onboarding.OnboardingScreen
import com.aurex.app.ui.screens.setup.ProfileSetupScreen
import com.aurex.app.ui.theme.AurexTheme
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
    val app = context.applicationContext as AurexApplication

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

    // Resolved once, here, and inherited by every nested AurexTheme call: the
    // user's choice has to outrank the phone wherever the theme is applied.
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    AurexTheme(darkTheme = darkTheme) {
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
            AurexLoadingScreen(modifier = Modifier.fillMaxSize())
            return@AurexTheme
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
}
