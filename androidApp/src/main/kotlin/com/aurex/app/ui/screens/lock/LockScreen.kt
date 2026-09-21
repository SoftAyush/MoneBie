package com.aurex.app.ui.screens.lock

import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aurex.app.security.AppLock
import com.aurex.app.security.AppLockSettings
import com.aurex.app.ui.components.AmbientBackdrop
import com.aurex.app.ui.components.AurexBrandMark
import com.aurex.app.ui.components.pressable
import com.aurex.app.ui.theme.AurexTheme

/**
 * The PIN gate.
 *
 * Drawn over the whole app rather than as a destination, so nothing behind it
 * is composed into a state the user could reach by dismissing it. There is no
 * back action: the only way through is the PIN or a fingerprint, and the only
 * way around is the home button.
 */
@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val biometricsAvailable = remember {
        AppLockSettings.isBiometricEnabled(context) && AppLock.canUseBiometrics(context)
    }

    val pinLength = remember { AppLockSettings.pinLength(context) }

    var entered by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var attempts by remember { mutableStateOf(0) }
    val shake = remember { Animatable(0f) }

    // Checked the moment the last digit lands: no confirm button to find.
    LaunchedEffect(entered) {
        if (entered.length < pinLength) return@LaunchedEffect
        if (AppLockSettings.verify(context, entered)) {
            onUnlocked()
            return@LaunchedEffect
        }
        wrong = true
        attempts++
        shake.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 360
                -14f at 60
                12f at 120
                -8f at 180
                6f at 240
                0f at 360
            }
        )
        entered = ""
    }

    val activity = context as? FragmentActivity
    val promptBiometric: () -> Unit = prompt@{
        val host = activity ?: return@prompt
        val prompt = BiometricPrompt(
            host,
            ContextCompat.getMainExecutor(host),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Aurex")
                .setSubtitle("Use your fingerprint or face, or enter your PIN")
                .setNegativeButtonText("Use PIN")
                .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
                .build()
        )
    }

    // Offered straight away: a user who set up a fingerprint should not have
    // to tap a button to be asked for it.
    LaunchedEffect(biometricsAvailable) {
        if (biometricsAvailable) promptBiometric()
    }

    // Back does not dismiss the gate; it sends the whole app away instead, the
    // same as the home button, so there is no route to the ledger through it.
    BackHandler { activity?.moveTaskToBack(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Drawn over live content: every touch stops here rather than
            // scrolling a list the user cannot see.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).consume()
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = AurexTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            AurexBrandMark(size = 56.dp)

            Spacer(Modifier.height(AurexTheme.spacing.large))

            Text(
                text = "Enter your PIN",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(AurexTheme.spacing.small))

            Text(
                text = when {
                    wrong && attempts >= 3 -> "Still not right. Take a moment and try again."
                    wrong -> "That PIN is not right."
                    else -> "Your finances stay private until you do."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (wrong) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(Modifier.height(AurexTheme.spacing.extraLarge))

            PinDots(
                filled = entered.length,
                total = pinLength,
                wrong = wrong,
                modifier = Modifier.graphicsLayer { translationX = shake.value }
            )

            Spacer(Modifier.weight(1f))

            PinPad(
                onDigit = { digit ->
                    wrong = false
                    if (entered.length < pinLength) entered += digit
                },
                onBackspace = {
                    wrong = false
                    entered = entered.dropLast(1)
                },
                onBiometric = if (biometricsAvailable) promptBiometric else null
            )

            Spacer(Modifier.height(AurexTheme.spacing.large))
        }
    }
}

@Composable
private fun PinDots(
    filled: Int,
    total: Int,
    wrong: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(total) { index ->
            val on = index < filled
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            wrong -> scheme.error.copy(alpha = if (on) 1f else 0.25f)
                            on -> scheme.primary
                            else -> scheme.onSurface.copy(alpha = 0.12f)
                        }
                    )
            )
        }
    }
}

/**
 * A phone-style keypad.
 *
 * Three columns of large targets rather than a text field: the keyboard would
 * cover half the screen, offer letters that cannot be used, and let the
 * system's autocorrect learn the PIN.
 */
@Composable
internal fun PinPad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    onBiometric: (() -> Unit)? = null
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9')
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)) {
                row.forEach { digit -> PinKey(label = digit.toString(), onClick = { onDigit(digit) }) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)) {
            if (onBiometric != null) {
                PinKey(icon = Icons.Rounded.Fingerprint, onClick = onBiometric, contentDescription = "Use fingerprint")
            } else {
                Spacer(Modifier.size(KEY_SIZE))
            }
            PinKey(label = "0", onClick = { onDigit('0') })
            PinKey(icon = Icons.AutoMirrored.Rounded.Backspace, onClick = onBackspace, contentDescription = "Delete")
        }
    }
}

private val KEY_SIZE = 72.dp

@Composable
private fun PinKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null,
    contentDescription: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(KEY_SIZE)
            .clip(CircleShape)
            .background(scheme.onSurface.copy(alpha = 0.05f))
            .pressable(onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurface
            )
        }
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = scheme.primary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
