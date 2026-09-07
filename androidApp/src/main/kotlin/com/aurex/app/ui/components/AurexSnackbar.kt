package com.aurex.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurex.app.ui.theme.AurexTheme
import com.aurex.app.ui.theme.Palette700
import com.aurex.app.ui.theme.Palette900

/**
 * The app's confirmation strip.
 *
 * Material's snackbar is a dark slab that belongs to a different application
 * than the one it lands on: square-ish, inverse-coloured, and unrelated to
 * every other surface here. This is the same raised card the rest of the app
 * uses, with a brand tick down its leading edge so a confirmation reads as one
 * before it is read at all.
 *
 * It stays a real SnackbarHost underneath, so timing, queueing and the
 * accessibility announcement are still Material's - only the surface changes.
 */
@Composable
fun AurexSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        // Enters from below rather than fading in place: it arrives from the
        // edge of the screen it is anchored to, which is where the eye is not.
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            AurexSnackbar(
                message = data.visuals.message,
                actionLabel = data.visuals.actionLabel,
                onAction = { data.performAction() }
            )
        }
    }
}

@Composable
private fun AurexSnackbar(
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val dark = isDarkScheme()
    val shape = RoundedCornerShape(20.dp)

    val fill = if (dark) {
        Brush.verticalGradient(listOf(scheme.surfaceContainerHigh, scheme.surfaceContainer))
    } else {
        Brush.verticalGradient(listOf(scheme.surface, scheme.surfaceContainerLow))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AurexTheme.spacing.medium, vertical = AurexTheme.spacing.small)
            .shadow(
                elevation = if (dark) 0.dp else 14.dp,
                shape = shape,
                ambientColor = Palette900.copy(alpha = 0.5f),
                spotColor = Palette700.copy(alpha = 0.5f)
            )
            .clip(shape)
            .background(fill)
            .border(1.dp, scheme.outline.copy(alpha = if (dark) 0.30f else 0.16f), shape)
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The same brand tick the section headers use, so a confirmation is
        // recognisably part of this app and not a system message.
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(28.dp)
                .clip(CircleShape)
                .background(brandBrush())
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (actionLabel != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.primary,
                maxLines = 1,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(scheme.primary.copy(alpha = 0.10f))
                    .pressable(onAction)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
}

/**
 * Shows a message, replacing anything already on screen.
 *
 * The actions these confirm - saving a name, adding a category, archiving an
 * account - come in quick succession, and a queue would leave a stale
 * confirmation up after the user has moved on to the next thing.
 */
suspend fun SnackbarHostState.showAurexMessage(message: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(message)
}
