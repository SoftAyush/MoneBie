package com.monebie.app.ui.screens.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.monebie.app.MoneBieApplication
import com.monebie.app.data.backup.GoogleIdTokenProvider
import com.monebie.app.domain.repository.BackupIdentity
import com.monebie.app.domain.repository.BackupMetadata
import com.monebie.app.presentation.backup.BackupEvent
import com.monebie.app.presentation.backup.BackupViewModel
import com.monebie.app.ui.components.AmbientBackdrop
import com.monebie.app.ui.components.MoneBieButton
import com.monebie.app.ui.components.MoneBieSnackbarHost
import com.monebie.app.ui.components.CardDivider
import com.monebie.app.ui.components.DetailTopBar
import com.monebie.app.ui.components.GlowBadge
import com.monebie.app.ui.components.PremiumCard
import com.monebie.app.ui.components.PremiumSectionHeader
import com.monebie.app.ui.components.pressable
import com.monebie.app.ui.components.showMoneBieMessage
import com.monebie.app.ui.theme.MoneBieTheme

/**
 * Backing the ledger up to the cloud, and putting it back.
 *
 * The screen is deliberately two screens in one: signed out it is only a
 * sign-in form, because nothing else on it can do anything useful until there
 * is an account for a backup to belong to. Showing the backup controls greyed
 * out would just be a list of things that do not work.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneBieApplication
    val activity = remember(context) { context.findActivity() }

    val viewModel: BackupViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                BackupViewModel(
                    remoteBackupStore = app.appContainer.remoteBackupStore,
                    backUpNowUseCase = app.appContainer.backUpNowUseCase,
                    restoreBackupUseCase = app.appContainer.restoreBackupUseCase
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showRestoreSheet by remember { mutableStateOf(false) }
    val restoreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(event) {
        when (val current = event) {
            is BackupEvent.SignedIn ->
                snackbarHostState.showMoneBieMessage("Signed in as ${current.label}.")

            BackupEvent.SignedOut -> snackbarHostState.showMoneBieMessage("Signed out.")

            is BackupEvent.BackedUp ->
                snackbarHostState.showMoneBieMessage("Backed up ${current.rowCount} rows.")

            is BackupEvent.Restored ->
                snackbarHostState.showMoneBieMessage("Restored ${current.rowCount} rows.")

            BackupEvent.Deleted -> snackbarHostState.showMoneBieMessage("Backup deleted.")

            is BackupEvent.Failed -> snackbarHostState.showMoneBieMessage(current.message)
            null -> Unit
        }
        if (event != null) viewModel.consumeEvent()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(title = "Backup & restore", onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MoneBieTheme.spacing.medium)
                    .imePadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.large)
            ) {
                Spacer(Modifier.height(MoneBieTheme.spacing.extraSmall))

                when (val identity = uiState.identity) {
                    BackupIdentity.SignedOut -> SignInSection(
                        isBusy = uiState.isBusy,
                        // The account sheet needs the Activity, so the request
                        // is handed to the view model as a lambda rather than
                        // the view model being given a Context it should not
                        // hold.
                        onSignIn = {
                            viewModel.signIn { GoogleIdTokenProvider.requestIdToken(activity) }
                        }
                    )

                    is BackupIdentity.SignedIn -> {
                        SignedInSection(
                            email = identity.email,
                            isBusy = uiState.isBusy,
                            onSignOut = viewModel::signOut
                        )

                        StoredBackupSection(
                            metadata = uiState.lastBackup,
                            isLoading = uiState.isLoadingStatus,
                            progressFraction = uiState.progress?.fraction,
                            canBackUp = uiState.canBackUp,
                            canRestore = uiState.canRestore,
                            isBusy = uiState.isBusy,
                            onBackUpNow = viewModel::backUpNow,
                            onRestore = { showRestoreSheet = true },
                            onDelete = viewModel::deleteBackup
                        )
                    }
                }

                Spacer(Modifier.height(MoneBieTheme.spacing.large))
            }
        }

        MoneBieSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }

    if (showRestoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRestoreSheet = false },
            sheetState = restoreSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            RestoreConfirmation(
                metadata = uiState.lastBackup,
                onConfirm = {
                    showRestoreSheet = false
                    viewModel.restore()
                },
                onCancel = { showRestoreSheet = false }
            )
        }
    }
}

/**
 * The signed-out state: one button.
 *
 * No sign-up to choose between, because Google sign-in has no such distinction -
 * Firebase creates the account the first time it sees the identity. Offering
 * "sign in" and "register" separately would invent a decision the user cannot
 * get wrong but can still be confused by.
 */
@Composable
private fun SignInSection(
    isBusy: Boolean,
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.large)
    ) {
        InfoCard(
            icon = Icons.Rounded.CloudOff,
            title = "Your data is on this phone only",
            body = "Sign in with Google to keep a copy in the cloud. Signing in with the " +
                "same account on a new phone brings everything back."
        )

        MoneBieButton(
            text = "Continue with Google",
            onClick = onSignIn,
            loading = isBusy,
            icon = Icons.Rounded.AccountCircle
        )
    }
}

@Composable
private fun SignedInSection(
    email: String,
    isBusy: Boolean,
    onSignOut: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PremiumSectionHeader(title = "Signed in as")

        Spacer(Modifier.height(MoneBieTheme.spacing.small))

        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
            ) {
                GlowBadge(icon = Icons.Rounded.CloudDone, tint = MaterialTheme.colorScheme.primary)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = email.ifEmpty { "Your backup account" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Backups are stored against this account",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Sign out",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .pressable { if (!isBusy) onSignOut() }
                        .padding(MoneBieTheme.spacing.small)
                )
            }
        }
    }
}

@Composable
private fun StoredBackupSection(
    metadata: BackupMetadata?,
    isLoading: Boolean,
    progressFraction: Float?,
    canBackUp: Boolean,
    canRestore: Boolean,
    isBusy: Boolean,
    onBackUpNow: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PremiumSectionHeader(title = "Cloud backup")

        Spacer(Modifier.height(MoneBieTheme.spacing.small))

        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
            ) {
                when {
                    isLoading -> StatusLine("Checking for a backup...")

                    metadata == null -> StatusLine("No backup yet.")

                    // Surfaced rather than hidden: a backup that never finished
                    // uploading is exactly the one a user must not be left
                    // believing in.
                    !metadata.isComplete -> StatusLine(
                        "The last upload did not finish. Back up again before relying on it.",
                        tint = MaterialTheme.colorScheme.error
                    )

                    else -> StatusLine(
                        "Last backup ${metadata.createdAt}, ${metadata.rowCount} rows, " +
                            "from ${metadata.deviceLabel}."
                    )
                }

                if (progressFraction != null) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                MoneBieButton(
                    text = "Back up now",
                    onClick = onBackUpNow,
                    enabled = canBackUp,
                    loading = isBusy && progressFraction != null,
                    icon = Icons.Rounded.CloudUpload
                )
            }
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.large))

        PremiumSectionHeader(title = "Restore & remove")

        Spacer(Modifier.height(MoneBieTheme.spacing.small))

        PremiumCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = MoneBieTheme.spacing.medium
        ) {
            ActionRow(
                icon = Icons.Rounded.Restore,
                title = "Restore from backup",
                subtitle = "Replaces everything on this phone with the backed-up copy",
                enabled = canRestore,
                onClick = onRestore
            )

            CardDivider(alpha = 0.07f)

            ActionRow(
                icon = Icons.Rounded.DeleteForever,
                title = "Delete cloud backup",
                subtitle = "Removes the stored copy. Your data on this phone is untouched.",
                enabled = !isBusy && metadata != null,
                tint = MaterialTheme.colorScheme.error,
                onClick = onDelete
            )
        }
    }
}

/**
 * The last step before a restore overwrites the ledger.
 *
 * States what is about to be lost and what replaces it, because "restore" reads
 * as an additive word and the operation is anything but.
 */
@Composable
private fun RestoreConfirmation(
    metadata: BackupMetadata?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MoneBieTheme.spacing.large)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
    ) {
        GlowBadge(icon = Icons.Rounded.Warning, tint = MaterialTheme.colorScheme.error)

        Text(
            text = "Replace everything on this phone?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = buildString {
                append(
                    "Every account, transaction, debt, EMI and personal loan on this " +
                        "phone will be deleted and replaced with the backup"
                )
                metadata?.let { append(" taken on ${it.createdAt}") }
                append(". This cannot be undone.")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(MoneBieTheme.spacing.small))

        MoneBieButton(
            text = "Replace my data",
            onClick = onConfirm,
            icon = Icons.Rounded.Restore,
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )

        MoneBieButton(
            text = "Cancel",
            onClick = onCancel,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusLine(
    text: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = tint
    )
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
        ) {
            GlowBadge(icon = icon, tint = MaterialTheme.colorScheme.primary)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * A settings-style row that can be switched off.
 *
 * Shaped like the rows on the settings screen, with a disabled state those do
 * not need: restoring is impossible until there is a complete backup to restore
 * from, and a row that simply did nothing when tapped would read as a bug.
 */
@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    val alpha = if (enabled) 1f else 0.4f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .pressable { if (enabled) onClick() }
            .padding(vertical = MoneBieTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
    ) {
        GlowBadge(icon = icon, tint = tint.copy(alpha = alpha))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (tint == MaterialTheme.colorScheme.primary) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                } else {
                    tint.copy(alpha = alpha)
                }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
        }
    }
}

/**
 * The Activity behind a Compose [Context].
 *
 * Credential Manager puts a sheet on screen and so needs the Activity itself,
 * not the application context. The context a composable sees is usually a
 * wrapper around it rather than the Activity, so it is unwrapped rather than
 * cast - a plain cast works right up until something inserts a themed wrapper.
 */
private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("Backup screen is not hosted in an Activity")
}
