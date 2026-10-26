package com.aurex.app.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurex.app.domain.model.ThemeMode
import com.aurex.app.domain.usecase.account.HasAccountUseCase
import com.aurex.app.domain.usecase.user.GetThemeModeUseCase
import com.aurex.app.domain.usecase.user.GetUsersUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Decides where the app opens.
 *
 * Both flags start as null so the shell can show a loading state instead of
 * briefly routing to onboarding while the database is still being read.
 */
class MainViewModel(
    getUsersUseCase: GetUsersUseCase,
    hasAccountUseCase: HasAccountUseCase,
    getThemeModeUseCase: GetThemeModeUseCase,
    /**
     * What the platform already knows the choice to be.
     *
     * Reading it from the database takes a frame or two, and defaulting to
     * SYSTEM in the meantime shows the phone's palette to a user who asked for
     * the other one - a flash of the wrong theme on every launch.
     */
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM
) : ViewModel() {

    /**
     * The palette the whole app is drawn in.
     *
     * Starts on SYSTEM so the first frame follows the phone rather than
     * flashing the light theme at a user who chose dark.
     */
    val themeMode: StateFlow<ThemeMode> = getThemeModeUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = initialThemeMode
        )

    val isUserCreated: StateFlow<Boolean?> = getUsersUseCase()
        .map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Backed by a real account count now that accounts are persisted. */
    val isAccountSetup: StateFlow<Boolean?> = hasAccountUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
