package com.monebie.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.monebie.app.ui.theme.MoneBieTheme

/*
 * The shared visual language for the pre-login flow - onboarding, profile
 * setup, account setup.
 *
 * These live here rather than in one screen because all three screens are one
 * continuous first impression: if the aurora, the brand fill or the call to
 * action drift apart between them, the seams show. One definition, three
 * screens.
 */

/**
 * True when the running scheme is the dark one.
 *
 * Read from the scheme rather than [androidx.compose.foundation.isSystemInDarkTheme]
 * so a theme with an explicit override still gets the matching fill.
 */
@Composable
fun isDarkScheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/**
 * The gradient every primary control is filled with.
 *
 * The two ends are picked so white-on-light never happens: on the light scheme
 * it runs deep navy to mid blue under white text, and on the dark scheme light
 * cyan to cyan under navy text. Filling both with primary -> secondary would put
 * white on #00B4D8, which is 2.4:1.
 */
@Composable
fun brandBrush(): Brush {
    val scheme = MaterialTheme.colorScheme
    return if (isDarkScheme()) {
        Brush.linearGradient(listOf(scheme.primary, scheme.secondary))
    } else {
        Brush.linearGradient(listOf(scheme.tertiary, scheme.primary))
    }
}

/** The only colour that is legible across the whole of [brandBrush]. */
@Composable
fun onBrandColor(): Color =
    if (isDarkScheme()) MaterialTheme.colorScheme.onPrimary else Color.White

/**
 * Two brand-coloured lights drifting behind the content on long, mismatched
 * cycles, so the screen never repeats a frame the eye can catch.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val dark = isDarkScheme()
    val drift = rememberInfiniteTransition(label = "aurora")

    val a by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(11_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_a"
    )
    val b by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_b"
    )

    // A glow that reads on a near-white page would blow out on a navy one.
    val primaryAlpha = if (dark) 0.30f else 0.16f
    val secondaryAlpha = if (dark) 0.22f else 0.13f

    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(scheme.background, scheme.surfaceContainerLow, scheme.background)
            )
        )

        val first = Offset(
            x = size.width * lerp(0.10f, 0.45f, a),
            y = size.height * lerp(0.06f, 0.26f, a)
        )
        val firstRadius = size.maxDimension * 0.62f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(scheme.primary.copy(alpha = primaryAlpha), Color.Transparent),
                center = first,
                radius = firstRadius
            ),
            radius = firstRadius,
            center = first
        )

        val second = Offset(
            x = size.width * lerp(0.96f, 0.62f, b),
            y = size.height * lerp(0.78f, 0.56f, b)
        )
        val secondRadius = size.maxDimension * 0.55f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(scheme.secondary.copy(alpha = secondaryAlpha), Color.Transparent),
                center = second,
                radius = secondRadius
            ),
            radius = secondRadius,
            center = second
        )
    }
}

/** The gradient tile carrying the MoneBie initial. */
@Composable
fun MoneBieBrandMark(modifier: Modifier = Modifier, size: Dp = 34.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.medium)
            .background(brandBrush()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "A",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = onBrandColor()
        )
    }
}

/**
 * The header every pre-login screen opens with: the mark, the wordmark, and
 * whatever that screen wants on the right - Skip, a step count, nothing.
 */
@Composable
fun MoneBieBrandBar(
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MoneBieTheme.spacing.large,
                vertical = MoneBieTheme.spacing.medium
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MoneBieBrandMark()
        Spacer(Modifier.width(MoneBieTheme.spacing.small + 2.dp))
        Text(
            text = "MONEBIE",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** "Step 2 of 2", as a quiet pill. */
@Composable
fun MoneBieStepPill(current: Int, total: Int, modifier: Modifier = Modifier) {
    Text(
        text = "Step $current of $total",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** The morphing dots: the active one stretches into a pill. */
@Composable
fun MoneBieStepDots(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val isSelected = index == current

            val width by animateDpAsState(
                targetValue = if (isSelected) 30.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "dot_width"
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
                },
                label = "dot_color"
            )

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** The floating panel the pre-login screens put their content on. */
@Composable
fun MoneBieGlassCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 20.dp, shape = shape, spotColor = scheme.primary)
            .background(
                brush = Brush.verticalGradient(
                    listOf(scheme.surface, scheme.surfaceContainerLow)
                ),
                shape = shape
            )
            .border(1.dp, scheme.outline.copy(alpha = 0.18f), shape)
            .padding(contentPadding)
    ) {
        content()
    }
}

/**
 * The primary call to action: full width, gradient filled, springs under the
 * thumb. [loading] locks it so a double tap cannot submit twice.
 */
@Composable
fun MoneBieGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cta_press"
    )
    val active = enabled && !loading
    val content = onBrandColor()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (active) 1f else 0.55f
            }
            .clip(MaterialTheme.shapes.large)
            .background(brandBrush())
            .clickable(
                enabled = active,
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = content
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = content
            )
            icon?.let {
                Spacer(Modifier.width(MoneBieTheme.spacing.small))
                Icon(imageVector = it, contentDescription = null, tint = content)
            }
        }
    }
}

/**
 * Runs 0 -> 1 once, [delayMillis] after the composable first appears.
 *
 * Handing each block its own delay is what staggers a screen in rather than
 * flashing the whole thing at once.
 */
@Composable
fun rememberEntrance(delayMillis: Int = 0): Float {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(
            durationMillis = 550,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        ),
        label = "entrance"
    )
    return progress
}

/** Fades and lifts a block into place from an entrance [progress]. */
fun Modifier.entrance(progress: Float, rise: Dp = 24.dp): Modifier = graphicsLayer {
    alpha = progress
    translationY = (1f - progress) * rise.toPx()
}
