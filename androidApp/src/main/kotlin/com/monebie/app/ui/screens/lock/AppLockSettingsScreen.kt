package com.monebie.app.ui.screens.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.monebie.app.security.AppLock
import com.monebie.app.security.AppLockSettings
import com.monebie.app.ui.components.AmbientBackdrop
import com.monebie.app.ui.components.MoneBieButton
import com.monebie.app.ui.components.MoneBieSnackbarHost
import com.monebie.app.ui.components.CardDivider
import com.monebie.app.ui.components.ConfirmDialog
import com.monebie.app.ui.components.DetailTopBar
import com.monebie.app.ui.components.FormNotice
import com.monebie.app.ui.components.GlowBadge
import com.monebie.app.ui.components.PremiumCard
import com.monebie.app.ui.components.PremiumSectionHeader
import com.monebie.app.ui.components.SelectableChip
import com.monebie.app.ui.components.pressable
import com.monebie.app.ui.components.showMoneBieMessage
import com.monebie.app.ui.theme.MoneBieTheme

/**
 * The app lock settings.
 *
 * Plain state read from preferences on each change rather than a view model:
 * there is no database behind any of this, and nothing else in the app
 * observes it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppLockSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var enabled by remember { mutableStateOf(AppLockSettings.isEnabled(context)) }
    var biometric by remember { mutableStateOf(AppLockSettings.isBiometricEnabled(context)) }
    var timeout by remember { mutableStateOf(AppLockSettings.timeout(context)) }
    val canUseBiometrics = remember { AppLock.canUseBiometrics(context) }

    /** Why the PIN sheet is open: switching on, or changing an existing PIN. */
    var pinSheet by remember { mutableStateOf<PinSheetReason?>(null) }
    var confirmDisable by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(message) {
        message?.let { snackbarHostState.showMoneBieMessage(it) }
        message = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                title = "App lock",
                onBack = onBack,
                subtitle = if (enabled) "On" else "Off"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MoneBieTheme.spacing.medium)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
            ) {
                Spacer(Modifier.height(MoneBieTheme.spacing.extraSmall))

                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
                    ) {
                        GlowBadge(
                            icon = if (enabled) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ask for a PIN",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (enabled) {
                                    "MoneBie locks when you leave it and asks on the way back in."
                                } else {
                                    "Anyone who picks up your phone can open MoneBie."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { wanted ->
                                if (wanted) pinSheet = PinSheetReason.ENABLE else confirmDisable = true
                            }
                        )
                    }
                }

                if (enabled) {
                    PremiumSectionHeader(title = "Unlocking")

                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .pressable { pinSheet = PinSheetReason.CHANGE }
                                .padding(vertical = MoneBieTheme.spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
                        ) {
                            GlowBadge(icon = Icons.Rounded.Password, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Change PIN",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = AppLockSettings.pinLength(context).toString() + " digits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        CardDivider(alpha = 0.07f)

                        Row(
                            modifier = Modifier.padding(vertical = MoneBieTheme.spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
                        ) {
                            GlowBadge(icon = Icons.Rounded.Fingerprint, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Fingerprint or face",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (canUseBiometrics) {
                                        "Offered first; the PIN is always there as a fallback."
                                    } else {
                                        "Set one up in your phone's settings to use it here."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = biometric && canUseBiometrics,
                                enabled = canUseBiometrics,
                                onCheckedChange = { wanted ->
                                    AppLockSettings.setBiometricEnabled(context, wanted)
                                    biometric = wanted
                                }
                            )
                        }
                    }

                    PremiumSectionHeader(title = "Lock again")

                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
                        ) {
                            GlowBadge(icon = Icons.Rounded.Timer, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "After leaving the app",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "A short grace period means switching to your bank app " +
                                        "and back does not cost a PIN.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(MoneBieTheme.spacing.medium))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small),
                            verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small)
                        ) {
                            AppLockSettings.Timeout.entries.forEach { option ->
                                SelectableChip(
                                    label = option.label,
                                    selected = timeout == option,
                                    onClick = {
                                        AppLockSettings.setTimeout(context, option)
                                        timeout = option
                                    }
                                )
                            }
                        }
                    }
                }

                FormNotice(
                    icon = Icons.Rounded.Lock,
                    text = "The PIN lives only on this phone. There is no way to recover it - " +
                        "if it is forgotten, the only way back in is to delete everything from " +
                        "the settings screen and restore a backup."
                )

                Spacer(Modifier.height(MoneBieTheme.spacing.extraExtraLarge))
            }
        }

        MoneBieSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    pinSheet?.let { reason ->
        ModalBottomSheet(
            onDismissRequest = { pinSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            SetPinSheet(
                title = if (reason == PinSheetReason.ENABLE) "Choose a PIN" else "Choose a new PIN",
                onDone = { pin ->
                    AppLockSettings.enable(context, pin)
                    enabled = true
                    pinSheet = null
                    message = if (reason == PinSheetReason.ENABLE) "App lock is on." else "PIN changed."
                }
            )
        }
    }

    if (confirmDisable) {
        ConfirmDialog(
            title = "Turn off the app lock?",
            message = "MoneBie will open without asking for anything.",
            confirmLabel = "Turn off",
            isDestructive = true,
            onConfirm = {
                AppLockSettings.disable(context)
                enabled = false
                biometric = false
                confirmDisable = false
                message = "App lock is off."
            },
            onDismiss = { confirmDisable = false }
        )
    }
}

private enum class PinSheetReason { ENABLE, CHANGE }

/**
 * Enter, then repeat.
 *
 * Two passes rather than one because a mistyped PIN on a lock with no recovery
 * is a lock-out, and the second pass is the only thing standing between the
 * user and that.
 */
@Composable
private fun SetPinSheet(
    title: String,
    onDone: (String) -> Unit
) {
    var first by remember { mutableStateOf<String?>(null) }
    var entered by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }

    val confirming = first != null
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MoneBieTheme.spacing.large)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (confirming) "Enter it again" else title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(MoneBieTheme.spacing.small))

        Text(
            text = when {
                mismatch -> "Those did not match. Start again."
                confirming -> "Just to be sure."
                else -> "${AppLockSettings.PIN_MIN_LENGTH} to ${AppLockSettings.PIN_MAX_LENGTH} digits."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (mismatch) scheme.error else scheme.onSurfaceVariant
        )

        Spacer(Modifier.height(MoneBieTheme.spacing.large))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(AppLockSettings.PIN_MAX_LENGTH) { index ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < entered.length) scheme.primary else scheme.onSurface.copy(alpha = 0.12f)
                        )
                )
            }
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.large))

        PinPad(
            onDigit = { digit ->
                mismatch = false
                if (entered.length < AppLockSettings.PIN_MAX_LENGTH) entered += digit
            },
            onBackspace = { entered = entered.dropLast(1) }
        )

        Spacer(Modifier.height(MoneBieTheme.spacing.medium))

        val canContinue = entered.length >= AppLockSettings.PIN_MIN_LENGTH
        MoneBieButton(
            text = if (confirming) "Set PIN" else "Continue",
            enabled = canContinue,
            onClick = {
                val original = first
                when {
                    original == null -> {
                        first = entered
                        entered = ""
                    }
                    original == entered -> onDone(entered)
                    else -> {
                        mismatch = true
                        first = null
                        entered = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(MoneBieTheme.spacing.large))
    }
}
