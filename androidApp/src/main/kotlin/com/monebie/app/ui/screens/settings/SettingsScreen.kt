package com.aurex.app.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurex.app.AurexApplication
import com.aurex.app.ResetAppData
import com.aurex.app.domain.model.ThemeMode
import com.aurex.app.notifications.PaymentReminderSettings
import com.aurex.app.notifications.PaymentReminders
import com.aurex.app.domain.model.displayName
import com.aurex.app.presentation.settings.SettingsEvent
import com.aurex.app.presentation.settings.SettingsViewModel
import com.aurex.app.security.AppLock
import com.aurex.app.security.AppLockSettings
import com.aurex.app.ui.components.AmbientBackdrop
import com.aurex.app.ui.components.AurexBrandMark
import com.aurex.app.ui.components.AurexButton
import com.aurex.app.ui.components.AurexSnackbarHost
import com.aurex.app.ui.components.showAurexMessage
import com.aurex.app.ui.components.AurexTextField
import com.aurex.app.ui.components.CardDivider
import com.aurex.app.ui.components.CurrencySelector
import com.aurex.app.ui.components.DetailTopBar
import com.aurex.app.ui.components.FormField
import com.aurex.app.ui.components.FormNotice
import com.aurex.app.ui.components.FormSection
import com.aurex.app.ui.components.GlowBadge
import com.aurex.app.ui.components.HeroCardShape
import com.aurex.app.ui.components.HeroDecoration
import com.aurex.app.ui.components.HeroIconButton
import com.aurex.app.ui.components.HeroPill
import com.aurex.app.ui.components.HeroTile
import com.aurex.app.ui.components.MicroLabel
import com.aurex.app.ui.components.OnHero
import com.aurex.app.ui.components.PremiumCard
import com.aurex.app.ui.components.PremiumCardShape
import com.aurex.app.ui.components.PremiumSectionHeader
import com.aurex.app.ui.components.entrance
import com.aurex.app.ui.components.heroBrush
import com.aurex.app.ui.components.pressable
import com.aurex.app.ui.components.rememberEntrance
import com.aurex.app.ui.theme.AurexBackgroundDark
import com.aurex.app.ui.theme.AurexBackgroundLight
import com.aurex.app.ui.theme.AurexOnSurfaceDark
import com.aurex.app.ui.theme.AurexOnSurfaceLight
import com.aurex.app.ui.theme.AurexSurfaceContainerDark
import com.aurex.app.ui.theme.AurexSurfaceContainerLight
import com.aurex.app.ui.theme.AurexTheme
import com.aurex.app.ui.theme.Palette100
import com.aurex.app.ui.theme.Palette300
import com.aurex.app.ui.theme.Palette700
import com.aurex.app.ui.theme.Palette900

/**
 * Profile and app settings.
 *
 * Closes a real gap: the name and currency picked during onboarding could not
 * be changed afterwards.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenAppLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AurexApplication

    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    getCurrentUserUseCase = app.appContainer.getCurrentUserUseCase,
                    getCurrenciesUseCase = app.appContainer.getCurrenciesUseCase,
                    getAccountsUseCase = app.appContainer.getAccountsUseCase,
                    observeBackupIdentityUseCase = app.appContainer.observeBackupIdentityUseCase,
                    saveUserUseCase = app.appContainer.saveUserUseCase,
                    exportTransactionsUseCase = app.appContainer.exportTransactionsUseCase,
                    setThemeModeUseCase = app.appContainer.setThemeModeUseCase
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    /*
     * The draft is seeded when the sheet opens rather than when the profile
     * loads, which is what lets it be a plain String: there is no window where
     * the flow can re-emit over something half-typed, because the field does
     * not exist until the moment it is filled from the current name.
     */
    var showNameSheet by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }

    var showResetSheet by remember { mutableStateOf(false) }
    var resetConfirmation by remember { mutableStateOf("") }

    /*
     * Reminders live outside the database, so the switch reads the setting once
     * and then owns it. Re-read on every return to the screen as well: Android's
     * own settings can take the notification permission back while Aurex is in
     * the background, and a switch still showing "on" would be a lie.
     */
    var remindersOn by remember {
        mutableStateOf(PaymentReminderSettings.isEnabled(context) && notificationsAllowed(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val allowed = PaymentReminderSettings.isEnabled(context) && notificationsAllowed(context)
            if (!allowed && PaymentReminderSettings.isEnabled(context)) {
                // Permission revoked from outside: stop the daily check rather
                // than leave work running that can no longer post anything.
                PaymentReminders.disable(context)
            }
            remindersOn = allowed
        }
    }

    var permissionRefused by remember { mutableStateOf(false) }

    // Read on every return, like the reminder switch: the lock screen a level
    // down changes these and this row has to say so when the user comes back.
    var appLockOn by remember { mutableStateOf(AppLockSettings.isEnabled(context)) }
    var biometricOn by remember { mutableStateOf(AppLockSettings.isBiometricEnabled(context)) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            appLockOn = AppLockSettings.isEnabled(context)
            biometricOn = AppLockSettings.isBiometricEnabled(context)
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            PaymentReminders.enable(context)
            remindersOn = true
        } else {
            remindersOn = false
            permissionRefused = true
        }
    }

    LaunchedEffect(permissionRefused) {
        if (permissionRefused) {
            snackbarHostState.showAurexMessage(
                "Aurex needs permission to send notifications. You can allow it in " +
                    "Android's app settings."
            )
            permissionRefused = false
        }
    }

    LaunchedEffect(event) {
        when (val current = event) {
            is SettingsEvent.ProfileSaved ->
                snackbarHostState.showAurexMessage("Saved. Hello, " + current.name + ".")

            is SettingsEvent.CurrencyChanged ->
                snackbarHostState.showAurexMessage(
                    "Now showing amounts in " + current.currency.code + "."
                )

            is SettingsEvent.ExportReady -> {
                val shared = shareCsv(
                    context = context,
                    fileName = current.file.fileName,
                    mimeType = current.file.mimeType,
                    content = current.file.content
                )
                snackbarHostState.showAurexMessage(
                    if (shared) {
                        "Exported " + current.file.rowCount + " transactions."
                    } else {
                        "Could not open anything to share the file with."
                    }
                )
            }

            is SettingsEvent.ThemeChanged -> snackbarHostState.showAurexMessage(
                when (current.mode) {
                    ThemeMode.SYSTEM -> "Following your phone's appearance."
                    ThemeMode.LIGHT -> "Light theme on."
                    ThemeMode.DARK -> "Dark theme on."
                }
            )

            is SettingsEvent.Failed -> snackbarHostState.showAurexMessage(current.message)
            null -> Unit
        }
        if (event != null) viewModel.consumeEvent()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                title = "Profile & settings",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AurexTheme.spacing.medium)
                    .imePadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.large)
            ) {
                Spacer(Modifier.height(AurexTheme.spacing.extraSmall))

                ProfileHero(
                    name = uiState.user?.name,
                    accountCount = uiState.accountCount,
                    currencyCode = uiState.selectedCurrency?.code,
                    themeMode = uiState.themeMode,
                    isBackupOn = uiState.isBackupOn,
                    onEditName = {
                        nameDraft = uiState.user?.name.orEmpty()
                        nameError = null
                        showNameSheet = true
                    }
                )

                FormSection(title = "Appearance") {
                    FormField(
                        label = "Theme",
                        hint = when (uiState.themeMode) {
                            ThemeMode.SYSTEM ->
                                "Following your phone, switching with it as it changes."
                            ThemeMode.LIGHT -> "Always light, whatever your phone is set to."
                            ThemeMode.DARK -> "Always dark, whatever your phone is set to."
                        }
                    ) {
                        ThemePicker(
                            selected = uiState.themeMode,
                            onSelect = viewModel::setThemeMode
                        )
                    }
                }

                uiState.selectedCurrency?.let { selected ->
                    FormSection(title = "Currency") {
                        FormField(
                            label = "Shown throughout the app",
                            // Important to say: amounts are stored as plain
                            // numbers with no currency attached, so switching
                            // relabels rather than converts. Silently implying
                            // conversion would be worse.
                            hint = "This changes the symbol shown throughout the app. Existing " +
                                "amounts are not converted."
                        ) {
                            CurrencySelector(
                                currencies = uiState.currencies,
                                selected = selected,
                                onCurrencySelected = viewModel::changeCurrency
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    PremiumSectionHeader(title = "Categories, reminders & data")

                    Spacer(Modifier.height(AurexTheme.spacing.small))

                    PremiumCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = AurexTheme.spacing.medium
                    ) {
                        SettingsRow(
                            icon = Icons.Rounded.Sell,
                            title = "Categories",
                            subtitle = "Rename the built-in ones, or add your own",
                            onClick = onOpenCategories
                        )

                        CardDivider(alpha = 0.07f)

                        SettingsRow(
                            icon = Icons.Rounded.Savings,
                            title = "Budgets",
                            subtitle = "Monthly limits by category, and how much is left",
                            onClick = onOpenBudgets
                        )

                        CardDivider(alpha = 0.07f)

                        SettingsRow(
                            icon = Icons.Rounded.EventRepeat,
                            title = "Recurring transactions",
                            subtitle = "Rent, salary and subscriptions, recorded on their day",
                            onClick = onOpenRecurring
                        )

                        CardDivider(alpha = 0.07f)

                        SettingsRow(
                            icon = Icons.Rounded.NotificationsNone,
                            title = "Upcoming payments",
                            subtitle = "Everything due across debts, EMIs and personal loans",
                            onClick = onOpenReminders
                        )

                        CardDivider(alpha = 0.07f)

                        SettingsToggleRow(
                            icon = Icons.Rounded.NotificationsActive,
                            title = "Payment reminders",
                            subtitle = "A notification for each debt and EMI a week before, " +
                                "three days before, and on the day it is due",
                            checked = remindersOn,
                            onCheckedChange = { wanted ->
                                if (!wanted) {
                                    PaymentReminders.disable(context)
                                    remindersOn = false
                                } else if (notificationsAllowed(context)) {
                                    PaymentReminders.enable(context)
                                    remindersOn = true
                                } else {
                                    // The prompt arrives with a reason attached
                                    // because it is asked for at the moment the
                                    // user switches the feature on.
                                    notificationPermission.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                }
                            }
                        )

                        CardDivider(alpha = 0.07f)

                        SettingsRow(
                            icon = Icons.Rounded.FileDownload,
                            title = if (isBusy) "Preparing export..." else "Export transactions",
                            subtitle = "Save your full history as a CSV file",
                            onClick = { if (!isBusy) viewModel.exportTransactions() }
                        )

                        CardDivider(alpha = 0.07f)

                        SettingsRow(
                            icon = Icons.Rounded.FileUpload,
                            title = "Import transactions",
                            subtitle = "Bring in a CSV from Aurex or another app",
                            onClick = onOpenImport
                        )

                        CardDivider(alpha = 0.07f)

                        SettingsRow(
                            icon = Icons.Rounded.CloudUpload,
                            title = "Backup & restore",
                            // Says which account, not just that there is one:
                            // "on" is only reassuring if the user recognises
                            // where their finances are going.
                            subtitle = uiState.backupAccountEmail
                                ?.let { "Backing up to $it" }
                                ?: "Your data is only on this phone",
                            status = if (uiState.isBackupOn) "On" else "Off",
                            statusIsOn = uiState.isBackupOn,
                            onClick = onOpenBackup
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    PremiumSectionHeader(title = "Privacy")

                    Spacer(Modifier.height(AurexTheme.spacing.small))

                    PremiumCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = AurexTheme.spacing.medium
                    ) {
                        SettingsRow(
                            icon = Icons.Rounded.Lock,
                            title = "App lock",
                            subtitle = if (appLockOn) {
                                "A PIN" + (if (biometricOn) " or fingerprint" else "") +
                                    " is asked for on the way in"
                            } else {
                                "Ask for a PIN or fingerprint before opening Aurex"
                            },
                            status = if (appLockOn) "On" else "Off",
                            statusIsOn = appLockOn,
                            onClick = onOpenAppLock
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    PremiumSectionHeader(title = "Danger zone")

                    Spacer(Modifier.height(AurexTheme.spacing.small))

                    // Tinted rather than neutral: this card holds the one action
                    // on the page that cannot be taken back, and a card that
                    // looks like every other card is read as one.
                    DangerCard {
                        SettingsRow(
                            icon = Icons.Rounded.DeleteForever,
                            title = "Delete everything",
                            subtitle = "Erase your profile, accounts and full history, " +
                                "and start over",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { showResetSheet = true }
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    PremiumSectionHeader(title = "About Aurex")

                    Spacer(Modifier.height(AurexTheme.spacing.small))

                    PremiumCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = AurexTheme.spacing.medium
                    ) {
                        AboutRow(
                            icon = Icons.Rounded.PhoneAndroid,
                            // Worth stating outright on a screen about the
                            // user: a finance app that says nothing about where
                            // the numbers go invites the worse assumption.
                            //
                            // Which is exactly why this has to follow the backup
                            // setting rather than stay fixed. Promising that
                            // nothing is uploaded, on the same screen offering
                            // to upload it, would be the one claim here a user
                            // could catch the app out on - and a privacy promise
                            // caught out is worse than one never made.
                            title = if (uiState.isBackupOn) {
                                "This phone, plus your backup"
                            } else {
                                "Everything stays on this phone"
                            },
                            subtitle = if (uiState.isBackupOn) {
                                "Your accounts, transactions and loans live in a database " +
                                    "on this device. A copy is uploaded to your Google " +
                                    "account when you back up."
                            } else {
                                "Your accounts, transactions and loans live in a database " +
                                    "on this device. Nothing is uploaded or synced."
                            }
                        )

                        CardDivider(alpha = 0.07f)

                        AboutRow(
                            icon = Icons.Rounded.NotificationsNone,
                            // Follows the switch above rather than stating one
                            // of the two as a fact: this row is the answer to
                            // "will Aurex tell me while it is closed", and it
                            // has to match what the setting actually says.
                            title = if (remindersOn) {
                                "Reminders reach you outside Aurex"
                            } else {
                                "Reminders are in-app only"
                            },
                            subtitle = if (remindersOn) {
                                "Aurex checks your debts and EMIs each morning and notifies " +
                                    "you a week before, three days before, and on the day."
                            } else {
                                "Due dates are shown inside Aurex. Switch on payment reminders " +
                                    "above to be told while it is closed."
                            }
                        )
                    }
                }

                // The version sits under the mark rather than in a list row:
                // it is the end of the page, not another fact to read.
                BrandFooter(version = appVersionLabel())

                Spacer(Modifier.height(AurexTheme.spacing.extraExtraLarge))
            }
        }

        AurexSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }

    if (showNameSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNameSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            NameSheet(
                name = nameDraft,
                error = nameError,
                isSaving = isBusy,
                onNameChange = {
                    nameDraft = it
                    nameError = null
                },
                onSave = {
                    val trimmed = nameDraft.trim()
                    if (trimmed.isEmpty()) {
                        nameError = "Tell us what to call you"
                    } else {
                        viewModel.saveName(trimmed)
                        showNameSheet = false
                    }
                }
            )
        }
    }

    if (showResetSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showResetSheet = false
                resetConfirmation = ""
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            ResetSheet(
                confirmation = resetConfirmation,
                onConfirmationChange = { resetConfirmation = it },
                onExport = {
                    showResetSheet = false
                    resetConfirmation = ""
                    viewModel.exportTransactions()
                },
                onDelete = {
                    val wiped = ResetAppData.wipeAndRestart(context)
                    // Only reached when the wipe failed; the success path never
                    // returns, because the process is already on its way out.
                    if (!wiped) {
                        showResetSheet = false
                        resetConfirmation = ""
                    }
                }
            )
        }
    }
}

// -----------------------------------------------------------------------------

/**
 * Who the app thinks you are, and what it is set to.
 *
 * The one thing worth a hero on a settings screen. The three tiles underneath
 * the name are the whole of the user's setup in one line - how many accounts
 * they keep, what amounts are labelled in, which palette is in force. Every
 * control further down the page changes one of them, so the hero doubles as
 * confirmation that a change landed.
 *
 * Tapping it edits the name, because the card showing the name is where a user
 * goes to change it - a separate field further down the page is a second place
 * to look for the same thing.
 */
@Composable
private fun ProfileHero(
    name: String?,
    accountCount: Int,
    currencyCode: String?,
    themeMode: ThemeMode,
    isBackupOn: Boolean,
    onEditName: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entrance = rememberEntrance()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .entrance(entrance)
            .shadow(
                elevation = 22.dp,
                shape = HeroCardShape,
                ambientColor = Palette900,
                spotColor = Palette700
            )
            .clip(HeroCardShape)
            .background(heroBrush())
            .pressable(onEditName)
    ) {
        HeroDecoration(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MicroLabel(text = "Signed in as", color = OnHero.copy(alpha = 0.75f))

                Spacer(Modifier.weight(1f))

                // Where the numbers live, stated on the card about the person
                // who owns them - the one fact here worth knowing without
                // scrolling to the row that sets it.
                HeroPill(text = if (isBackupOn) "Backed up" else "On this phone")
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(name = name)

                Spacer(Modifier.width(16.dp))

                Text(
                    text = name.orEmpty().ifBlank { "Set your name" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnHero,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(12.dp))

                // The whole card is tappable; the pencil is what says so.
                HeroIconButton(
                    icon = Icons.Rounded.Edit,
                    contentDescription = "Change your name",
                    onClick = onEditName
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile(
                    label = if (accountCount == 1) "Account" else "Accounts",
                    value = accountCount.toString(),
                    valueColor = OnHero,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Currency",
                    value = currencyCode ?: "--",
                    valueColor = OnHero,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Theme",
                    value = themeMode.displayName,
                    valueColor = OnHero,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * A single initial on a lit ring.
 *
 * Rather than a stock silhouette: the initial is the only picture of the user
 * this application ever has, and a generic avatar would say less than the
 * letter does.
 *
 * The fill is the fixed brand cyan rather than a translucent white, so it is
 * the brightest thing on the hero instead of a hole in it - and so it reads the
 * same as the avatar on the dashboard, which is where the user tapped to get
 * here.
 */
@Composable
private fun Avatar(name: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.10f))
                )
            )
            .padding(3.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Palette100, Palette300))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name?.trim()?.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = Palette900
        )
    }
}

// -----------------------------------------------------------------------------
// Appearance
// -----------------------------------------------------------------------------

/**
 * The theme choice, shown rather than named.
 *
 * Three words in a segmented control ask the user to imagine the result. Three
 * miniatures of the app show it, which matters most for System - the option
 * whose whole point is that it is sometimes one and sometimes the other, and
 * the split preview is the only way to say that without a sentence.
 */
@Composable
private fun ThemePicker(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
    ) {
        ThemeMode.entries.forEach { mode ->
            ThemeOption(
                mode = mode,
                selected = mode == selected,
                onClick = { onSelect(mode) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    val borderColor by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.outline.copy(alpha = 0.30f),
        label = "themeOptionBorder"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.onSurfaceVariant,
        label = "themeOptionLabel"
    )
    // A tinted bed under the chosen preview. The ring alone reads as an outline
    // at a glance; the wash is what makes the choice visible without hunting
    // for which of three borders is the heavier one.
    val bedColor by animateColorAsState(
        targetValue = if (selected) scheme.primary.copy(alpha = 0.10f) else Color.Transparent,
        label = "themeOptionBed"
    )

    Column(
        modifier = modifier
            .clip(shape)
            .background(bedColor)
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .pressable(onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            when (mode) {
                ThemeMode.LIGHT -> ThemeMiniature(dark = false)
                ThemeMode.DARK -> ThemeMiniature(dark = true)
                // Split down the middle: the option is literally both, and
                // which one it lands on is the phone's business, not ours.
                ThemeMode.SYSTEM -> Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight()) { ThemeMiniature(dark = false) }
                    Box(Modifier.weight(1f).fillMaxHeight()) { ThemeMiniature(dark = true) }
                }
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = mode.displayName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = labelColor,
            maxLines = 1
        )
    }
}

/**
 * A miniature of the app in one palette.
 *
 * Painted from the palette tokens directly rather than from the current
 * `MaterialTheme`, because the whole job of the preview is to show the theme
 * that is *not* currently applied.
 */
@Composable
private fun ThemeMiniature(dark: Boolean, modifier: Modifier = Modifier) {
    val background = if (dark) AurexBackgroundDark else AurexBackgroundLight
    val surface = if (dark) AurexSurfaceContainerDark else AurexSurfaceContainerLight
    val ink = if (dark) AurexOnSurfaceDark else AurexOnSurfaceLight

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // The hero card, which is the same gradient in both palettes - the
        // one part of the app that does not change with the theme.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Brush.linearGradient(listOf(Palette900, Palette700)))
        )

        repeat(2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(surface)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(3.dp)
                .clip(CircleShape)
                .background(ink.copy(alpha = 0.35f))
        )
    }
}

/**
 * Changing your name.
 *
 * A sheet rather than a field on the page: it is a rare edit, and the field
 * standing open under the card it duplicates gave the same fact two homes.
 */
@Composable
private fun NameSheet(
    name: String,
    error: String?,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AurexTheme.spacing.medium)
            .imePadding()
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlowBadge(
                icon = Icons.Rounded.PersonOutline,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(AurexTheme.spacing.medium))
            Column {
                Text(
                    text = "Your name",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                MicroLabel(text = name.trim().ifEmpty { "Not set yet" })
            }
        }

        Spacer(Modifier.height(AurexTheme.spacing.large))

        FormField(
            label = "Name",
            error = error,
            hint = "Used for the greeting on your dashboard."
        ) {
            AurexTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = "Your name",
                leadingIcon = Icons.Rounded.PersonOutline,
                textStyle = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.height(AurexTheme.spacing.extraLarge))

        AurexButton(
            text = "Save name",
            loading = isSaving,
            onClick = onSave
        )

        Spacer(Modifier.height(AurexTheme.spacing.extraExtraLarge))
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Anything but the brand colour marks a row that does something unusual. */
    tint: Color = MaterialTheme.colorScheme.primary,
    /**
     * A short state word shown as a pill before the chevron.
     *
     * For rows that are not only a way in but also a thing that is currently on
     * or off. The subtitle already says as much in prose, but a user checking
     * whether their finances are backed up should not have to read a sentence
     * to find out.
     */
    status: String? = null,
    /** Colours [status] as a healthy state rather than a dormant one. */
    statusIsOn: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .pressable(onClick)
            .padding(vertical = AurexTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)
    ) {
        GlowBadge(icon = icon, tint = tint)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (tint == MaterialTheme.colorScheme.primary) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    tint
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        status?.let {
            val statusColor = if (statusIsOn) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // The chevron on a plate of its own, so the end of every row lands on
        // the same mark at the same size whatever the row carries before it.
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

/**
 * A row that is the setting rather than a way to one.
 *
 * Shaped like [SettingsRow] with the chevron replaced by a switch: the two sit
 * in the same card, and what ends the row is what says whether tapping it takes
 * you somewhere or changes something here.
 */
@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            // The whole row toggles, so the switch is a target of last resort
            // rather than the only one.
            .pressable { onCheckedChange(!checked) }
            .padding(vertical = AurexTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)
    ) {
        GlowBadge(
            icon = icon,
            tint = if (checked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

/**
 * Whether Aurex may post a notification at all.
 *
 * Always true below Android 13, where notifications need no permission; from 13
 * on it is the user's to grant and to take back at any time.
 */
private fun notificationsAllowed(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * The card the irreversible action sits on.
 *
 * Shaped like [PremiumCard] and coloured unlike it: a faint wash of the error
 * colour and a ring in the same hue, so the section is told apart before its
 * heading is read.
 */
@Composable
private fun DangerCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val error = MaterialTheme.colorScheme.error

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PremiumCardShape)
            .background(
                Brush.verticalGradient(
                    listOf(error.copy(alpha = 0.10f), error.copy(alpha = 0.04f))
                )
            )
            .border(1.dp, error.copy(alpha = 0.28f), PremiumCardShape)
            .padding(AurexTheme.spacing.medium),
        content = content
    )
}

/**
 * The mark, and what build this is.
 *
 * Closes the page the way the onboarding opens it. A version number is the last
 * thing anyone scrolls this far for, and a row in a card gave it the same
 * weight as a promise about where the data lives.
 */
@Composable
private fun BrandFooter(version: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AurexTheme.spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AurexBrandMark(size = 44.dp)

        Spacer(Modifier.height(AurexTheme.spacing.small))

        Text(
            text = version,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(2.dp))

        MicroLabel(
            text = "Your money, on your phone",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Deleting everything.
 *
 * A sheet with a typed confirmation rather than a two-button dialog. There is
 * no server copy and no undo - the file is gone the moment this is confirmed -
 * and a dialog whose destructive button sits under the thumb is dismissed by
 * reflex. Typing the word cannot be done by accident.
 *
 * The export is offered in the same breath as the deletion, because the moment
 * a user decides to wipe is the last moment they can still keep the history,
 * and sending them to find it elsewhere in the settings is how it gets lost.
 */
@Composable
private fun ResetSheet(
    confirmation: String,
    onConfirmationChange: (String) -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val error = MaterialTheme.colorScheme.error
    val canDelete = confirmation.trim().equals(CONFIRM_WORD, ignoreCase = true)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AurexTheme.spacing.medium)
            .imePadding()
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlowBadge(icon = Icons.Rounded.DeleteForever, tint = error)
            Spacer(Modifier.width(AurexTheme.spacing.medium))
            Column {
                Text(
                    text = "Delete everything",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                MicroLabel(text = "This cannot be undone", color = error)
            }
        }

        Spacer(Modifier.height(AurexTheme.spacing.large))

        FormNotice(
            icon = Icons.Rounded.Info,
            // Named individually rather than as "all your data": a user
            // deserves to recognise what they are about to lose.
            text = "Your name, currency and theme, every account and its balance, " +
                "every transaction, debt, EMI and personal loan, and any categories " +
                "you added. Aurex will close and open as it did on the first day.",
            accent = error
        )

        Spacer(Modifier.height(AurexTheme.spacing.medium))

        AurexButton(
            text = "Export transactions first",
            onClick = onExport,
            icon = Icons.Rounded.FileDownload,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(AurexTheme.spacing.large))

        FormField(
            label = "Type $CONFIRM_WORD to confirm",
            hint = "Nothing is deleted until this matches."
        ) {
            AurexTextField(
                value = confirmation,
                onValueChange = onConfirmationChange,
                placeholder = CONFIRM_WORD,
                leadingIcon = Icons.Rounded.DeleteForever,
                textStyle = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.height(AurexTheme.spacing.extraLarge))

        AurexButton(
            text = "Delete everything",
            onClick = onDelete,
            enabled = canDelete,
            containerColor = error,
            contentColor = MaterialTheme.colorScheme.onError
        )

        Spacer(Modifier.height(AurexTheme.spacing.extraExtraLarge))
    }
}

/** Short, unambiguous, and not a word anyone types into a settings screen by chance. */
private const val CONFIRM_WORD = "DELETE"

/**
 * A statement of fact, not a place to tap.
 *
 * Deliberately shaped like [SettingsRow] minus the chevron: the two sit in
 * matching cards, and the missing arrow is what says one leads somewhere and
 * the other does not.
 */
@Composable
private fun AboutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AurexTheme.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)
    ) {
        // A flat plate rather than the lit badge the tappable rows carry: the
        // quieter mark is half of what says this row goes nowhere.
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The installed version, read from the package rather than hard-coded.
 *
 * A literal here would be one more thing to remember to bump, and the number a
 * user reads would eventually stop matching the build they are running.
 */
@Composable
private fun appVersionLabel(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "Aurex " + info.versionName
        }.getOrDefault("Aurex")
    }
}

/**
 * Writes the CSV to app cache and hands it to any app that can take it.
 *
 * Cache rather than external storage keeps it inside the app sandbox, and the
 * FileProvider grants read access for the one share only.
 */
private fun shareCsv(
    context: android.content.Context,
    fileName: String,
    mimeType: String,
    content: String
): Boolean = runCatching {
    val exportDir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
    val file = java.io.File(exportDir, fileName)
    file.writeText(content)

    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Aurex transactions")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    AppLock.expectExternalActivity()
    context.startActivity(
        Intent.createChooser(intent, "Export transactions").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
    true
}.getOrDefault(false)
