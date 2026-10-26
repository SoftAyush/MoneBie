package com.monebie.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monebie.app.core.getCurrentDateString
import com.monebie.app.domain.model.Currency
import com.monebie.app.domain.repository.BackupIdentity
import com.monebie.app.domain.model.ThemeMode
import com.monebie.app.domain.model.User
import com.monebie.app.domain.usecase.account.GetAccountsUseCase
import com.monebie.app.domain.usecase.backup.ObserveBackupIdentityUseCase
import com.monebie.app.domain.usecase.currency.GetCurrenciesUseCase
import com.monebie.app.domain.usecase.export.ExportedFile
import com.monebie.app.domain.usecase.export.ExportTransactionsUseCase
import com.monebie.app.domain.usecase.user.GetCurrentUserUseCase
import com.monebie.app.domain.usecase.user.SaveUserUseCase
import com.monebie.app.domain.usecase.user.SetThemeModeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val currencies: List<Currency> = emptyList(),
    /**
     * How many accounts exist, not which ones.
     *
     * The profile summarises the setup rather than listing it - the accounts
     * screen already owns the list, and repeating it here would give the same
     * thing two homes.
     */
    val accountCount: Int = 0,
    /** Who backups are kept for, which is also whether they happen at all. */
    val backupIdentity: BackupIdentity = BackupIdentity.SignedOut
) {
    /**
     * Whether cloud backup is on.
     *
     * Being signed in is the whole of it. There is nothing else to switch: an
     * account is what a backup is stored against, so having one is the on
     * state and having none is the off state.
     */
    val isBackupOn: Boolean get() = backupIdentity is BackupIdentity.SignedIn

    /** The signed-in Google address, when there is one worth showing. */
    val backupAccountEmail: String?
        get() = (backupIdentity as? BackupIdentity.SignedIn)?.email?.takeIf { it.isNotEmpty() }

    val selectedCurrency: Currency?
        get() = currencies.firstOrNull { it.code == user?.currencyCode }

    /** Falls back to following the phone while the user row is still loading. */
    val themeMode: ThemeMode get() = user?.themeMode ?: ThemeMode.SYSTEM
}

/** Transient outcome of an action, surfaced once and then cleared. */
sealed interface SettingsEvent {
    data class ProfileSaved(val name: String) : SettingsEvent
    data class CurrencyChanged(val currency: Currency) : SettingsEvent
    data class ExportReady(val file: ExportedFile) : SettingsEvent
    data class ThemeChanged(val mode: ThemeMode) : SettingsEvent
    data class Failed(val message: String) : SettingsEvent
}

/**
 * Profile and app settings.
 *
 * Fills a real gap: a name and currency chosen once during onboarding were
 * previously impossible to change.
 */
class SettingsViewModel(
    getCurrentUserUseCase: GetCurrentUserUseCase,
    getCurrenciesUseCase: GetCurrenciesUseCase,
    getAccountsUseCase: GetAccountsUseCase,
    observeBackupIdentityUseCase: ObserveBackupIdentityUseCase,
    private val saveUserUseCase: SaveUserUseCase,
    private val exportTransactionsUseCase: ExportTransactionsUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase
) : ViewModel() {

    private val currentUser = getCurrentUserUseCase()


    val uiState: StateFlow<SettingsUiState> = combine(
        currentUser,
        getCurrenciesUseCase(),
        getAccountsUseCase(),
        observeBackupIdentityUseCase()
    ) { user, currencies, accounts, backupIdentity ->
        SettingsUiState(
            isLoading = false,
            user = user,
            currencies = currencies,
            accountCount = accounts.size,
            backupIdentity = backupIdentity
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _event = MutableStateFlow<SettingsEvent?>(null)
    val event: StateFlow<SettingsEvent?> = _event.asStateFlow()

    /**
     * Switches the palette.
     *
     * Written straight through rather than held in the view model: the theme is
     * applied at the root of the app from the same stored value, so the screen
     * that changed it has nothing to keep.
     */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            setThemeModeUseCase(mode)
            _event.value = SettingsEvent.ThemeChanged(mode)
        }
    }

    fun consumeEvent() {
        _event.value = null
    }

    fun saveName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _event.value = SettingsEvent.Failed("Your name cannot be empty")
            return
        }
        val user = uiState.value.user ?: return

        viewModelScope.launch {
            // The currency is carried through unchanged: the save is an upsert
            // of the whole row, so omitting it would silently reset it.
            saveUserUseCase(trimmed, getCurrentDateString(), user.currencyCode)
            _event.value = SettingsEvent.ProfileSaved(trimmed)
        }
    }

    /**
     * Changes the display currency.
     *
     * Only the symbol amounts are shown in changes - stored values are plain
     * minor units with no currency attached, so nothing is converted and no
     * historical figure is rewritten.
     */
    fun changeCurrency(currency: Currency) {
        val user = uiState.value.user ?: return
        if (user.currencyCode == currency.code) return

        viewModelScope.launch {
            saveUserUseCase(user.name, getCurrentDateString(), currency.code)
            _event.value = SettingsEvent.CurrencyChanged(currency)
        }
    }

    fun exportTransactions() {
        if (_isBusy.value) return
        _isBusy.update { true }

        viewModelScope.launch {
            val result = runCatching { exportTransactionsUseCase() }
            _isBusy.update { false }
            result
                .onSuccess { file ->
                    _event.value = if (file.rowCount == 0) {
                        SettingsEvent.Failed("There are no transactions to export yet")
                    } else {
                        SettingsEvent.ExportReady(file)
                    }
                }
                .onFailure {
                    _event.value = SettingsEvent.Failed(
                        it.message ?: "Could not build the export"
                    )
                }
        }
    }
}
