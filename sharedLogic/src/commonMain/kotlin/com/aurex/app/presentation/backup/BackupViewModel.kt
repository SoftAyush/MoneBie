package com.aurex.app.presentation.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurex.app.domain.repository.BackupIdentity
import com.aurex.app.domain.repository.BackupMetadata
import com.aurex.app.domain.repository.RemoteBackupStore
import com.aurex.app.domain.usecase.backup.BackUpNowUseCase
import com.aurex.app.domain.usecase.backup.BackupOutcome
import com.aurex.app.domain.usecase.backup.RestoreBackupUseCase
import com.aurex.app.domain.usecase.backup.RestoreOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Rows written out of rows to write, while an upload is running. */
data class BackupProgress(val written: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else written.toFloat() / total
}

data class BackupUiState(
    val identity: BackupIdentity = BackupIdentity.SignedOut,
    val lastBackup: BackupMetadata? = null,
    /**
     * True only while the stored backup is first being looked up.
     *
     * Separate from [isBusy] so the screen can show a quiet placeholder on open
     * without the buttons reading as though an upload were already running.
     */
    val isLoadingStatus: Boolean = true,
    val isBusy: Boolean = false,
    val progress: BackupProgress? = null
) {
    val isSignedIn: Boolean get() = identity is BackupIdentity.SignedIn
    val canBackUp: Boolean get() = isSignedIn && !isBusy
    val canRestore: Boolean get() = isSignedIn && !isBusy && lastBackup?.isComplete == true
}

/** Transient outcome of an action, surfaced once and then cleared. */
sealed interface BackupEvent {
    data class SignedIn(val label: String) : BackupEvent
    data object SignedOut : BackupEvent
    data class BackedUp(val rowCount: Int) : BackupEvent
    data class Restored(val rowCount: Int) : BackupEvent
    data object Deleted : BackupEvent
    data class Failed(val message: String) : BackupEvent
}

/**
 * Backing the ledger up to the cloud, and putting it back.
 *
 * The store is injected as the [RemoteBackupStore] interface, so everything
 * here is testable against a fake and none of it is tied to Firebase.
 */
class BackupViewModel(
    private val remoteBackupStore: RemoteBackupStore,
    private val backUpNowUseCase: BackUpNowUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _event = MutableStateFlow<BackupEvent?>(null)
    val event: StateFlow<BackupEvent?> = _event.asStateFlow()

    init {
        viewModelScope.launch {
            remoteBackupStore.identity.collect { identity ->
                _uiState.update { it.copy(identity = identity) }
                // Whose backup it is has just changed, so whatever was on screen
                // describes someone else's.
                refreshStatus()
            }
        }
    }

    fun consumeEvent() {
        _event.value = null
    }

    /**
     * Re-reads what is stored.
     *
     * A signed-out user has no backup to describe, so the lookup is skipped
     * rather than attempted and reported as a failure.
     */
    fun refreshStatus() {
        if (_uiState.value.identity !is BackupIdentity.SignedIn) {
            _uiState.update { it.copy(lastBackup = null, isLoadingStatus = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStatus = true) }
            runCatching { remoteBackupStore.latestMetadata() }
                .onSuccess { metadata ->
                    _uiState.update { it.copy(lastBackup = metadata, isLoadingStatus = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingStatus = false) }
                    _event.value = BackupEvent.Failed(
                        error.message ?: "Could not check for an existing backup"
                    )
                }
        }
    }

    /**
     * Signs in with Google.
     *
     * [requestGoogleIdToken] is supplied by the caller because getting a token
     * means putting a platform account picker on screen, which a view model has
     * no business knowing about. Running it inside the same `runCatching` as the
     * sign-in itself is what keeps one failure path: whether the account sheet
     * or Firebase is what went wrong, the user gets told the same way.
     *
     * A null token means the user dismissed the sheet. Nothing is reported for
     * it - they know what they did, and an error message for a deliberate
     * cancellation reads as a bug. The cases worth telling them about, such as
     * there being no Google account on the phone, arrive as exceptions and are
     * reported below.
     */
    fun signIn(requestGoogleIdToken: suspend () -> String?) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }

        viewModelScope.launch {
            val result = runCatching {
                requestGoogleIdToken()?.let { token -> remoteBackupStore.signIn(token) }
            }
            _uiState.update { it.copy(isBusy = false) }

            result
                .onSuccess { identity ->
                    if (identity is BackupIdentity.SignedIn) {
                        _event.value = BackupEvent.SignedIn(identity.email)
                    }
                }
                .onFailure {
                    _event.value = BackupEvent.Failed(it.message ?: "Could not sign in with Google")
                }
        }
    }

    fun signOut() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }

        viewModelScope.launch {
            runCatching { remoteBackupStore.signOut() }
                .also { _uiState.update { state -> state.copy(isBusy = false) } }
                .onSuccess { _event.value = BackupEvent.SignedOut }
                .onFailure {
                    _event.value = BackupEvent.Failed(it.message ?: "Could not sign out")
                }
        }
    }

    fun backUpNow() {
        if (!_uiState.value.canBackUp) return
        _uiState.update { it.copy(isBusy = true, progress = BackupProgress(0, 0)) }

        viewModelScope.launch {
            val result = runCatching {
                backUpNowUseCase { written, total ->
                    _uiState.update { it.copy(progress = BackupProgress(written, total)) }
                }
            }
            _uiState.update { it.copy(isBusy = false, progress = null) }

            result
                .onSuccess { outcome ->
                    _event.value = when (outcome) {
                        is BackupOutcome.Uploaded -> BackupEvent.BackedUp(outcome.rowCount)
                        BackupOutcome.NothingToBackUp ->
                            BackupEvent.Failed("There is nothing to back up yet")
                    }
                    refreshStatus()
                }
                .onFailure {
                    _event.value = BackupEvent.Failed(it.message ?: "Could not upload the backup")
                }
        }
    }

    /**
     * Replaces local data with the stored backup.
     *
     * The confirmation belongs to the screen, not here: by the time this runs
     * the user has already been told their current data will be replaced.
     */
    fun restore() {
        if (!_uiState.value.canRestore) return
        _uiState.update { it.copy(isBusy = true) }

        viewModelScope.launch {
            val result = runCatching { restoreBackupUseCase() }
            _uiState.update { it.copy(isBusy = false) }

            result
                .onSuccess { outcome ->
                    _event.value = when (outcome) {
                        is RestoreOutcome.Restored -> BackupEvent.Restored(outcome.rowCount)
                        RestoreOutcome.NothingStored ->
                            BackupEvent.Failed("There is no backup stored for this account")
                        RestoreOutcome.BackupIncomplete ->
                            BackupEvent.Failed(
                                "The stored backup was never finished uploading. " +
                                    "Back up again from the device that has your data."
                            )
                        is RestoreOutcome.FormatTooNew ->
                            BackupEvent.Failed(
                                "That backup was made by a newer version of Aurex. " +
                                    "Update the app to restore it."
                            )
                    }
                }
                .onFailure {
                    _event.value = BackupEvent.Failed(it.message ?: "Could not restore the backup")
                }
        }
    }

    fun deleteBackup() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }

        viewModelScope.launch {
            val result = runCatching { remoteBackupStore.delete() }
            _uiState.update { it.copy(isBusy = false) }

            result
                .onSuccess {
                    _event.value = BackupEvent.Deleted
                    refreshStatus()
                }
                .onFailure {
                    _event.value = BackupEvent.Failed(it.message ?: "Could not delete the backup")
                }
        }
    }
}
