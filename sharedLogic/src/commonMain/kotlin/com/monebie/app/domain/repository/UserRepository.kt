package com.aurex.app.domain.repository

import com.aurex.app.domain.model.ThemeMode
import com.aurex.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun insertUser(name: String, date: String, currencyCode: String)
    fun getAllUsers(): Flow<List<User>>
    fun getSelectedCurrencySymbol(): Flow<String?>

    /**
     * The appearance setting, defaulted to following the phone.
     *
     * Observed rather than read once: the whole app is re-themed the moment it
     * changes, from wherever it was changed.
     */
    fun observeThemeMode(): Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
