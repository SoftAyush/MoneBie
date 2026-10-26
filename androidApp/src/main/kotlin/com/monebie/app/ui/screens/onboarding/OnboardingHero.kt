package com.monebie.app.ui.screens.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monebie.app.ui.theme.MoneBieTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The artwork for each onboarding page.
 *
 * These are drawn from Compose primitives rather than shipped as illustrations
 * on purpose: they are miniatures of screens the user is about to meet, so the
 * pitch and the product look like the same application, and they animate on
 * arrival instead of sitting still.
 *
 * Every hero takes [progress] - 0f offscreen, 1f settled - so the caller can
 * drive the entrance from the pager rather than each hero running its own
 * clock.
 */
@Composable
fun OnboardingHero(
    imageResName: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    HeroFrame(modifier = modifier) {
        when (imageResName) {
            "agreement_debt" -> LedgerHero(progress)
            "payment_reminder" -> RepaymentHero(progress)
            else -> GrowthHero(progress)
        }
    }
}

/** The floating card every hero is drawn on. */
@Composable
private fun HeroFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(32.dp)
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .shadow(elevation = 24.dp, shape = shape, spotColor = scheme.primary)
            .background(
                brush = Brush.verticalGradient(
                    listOf(scheme.surface, scheme.surfaceContainerLow)
                ),
                shape = shape
            )
            .border(1.dp, scheme.outline.copy(alpha = 0.18f), shape)
            .padding(24.dp)
    ) {
        content()
    }
}

// ---- Page 1: track every penny -------------------------------------------

@Composable
private fun GrowthHero(progress: Float) {
    val scheme = MaterialTheme.colorScheme
    val bars = listOf(0.34f, 0.52f, 0.40f, 0.74f, 1f)
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total balance",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "84,250.00",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
            }
            HeroChip(
                text = "+12.4%",
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                content = MoneBieTheme.finance.positive,
                container = MoneBieTheme.finance.positiveContainer
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                // weight, not a fixed height: the chart should own whatever the
                // card has left rather than leaving a void above it.
                .weight(1f)
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEachIndexed { index, target ->
                val grown = target * stagger(progress, index)
                val isPeak = index == bars.lastIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(grown.coerceAtLeast(0.03f))
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .background(
                            Brush.verticalGradient(
                                if (isPeak) {
                                    listOf(scheme.secondary, scheme.primary)
                                } else {
                                    listOf(
                                        scheme.primary.copy(alpha = 0.52f),
                                        scheme.primary.copy(alpha = 0.26f)
                                    )
                                }
                            )
                        )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ---- Page 2: both sides of the ledger ------------------------------------

@Composable
private fun LedgerHero(progress: Float) {
    val scheme = MaterialTheme.colorScheme
    val finance = MoneBieTheme.finance

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        LedgerRow(
            label = "Owed to you",
            amount = "12,400.00",
            icon = Icons.Rounded.ArrowDownward,
            accent = finance.receivable,
            container = finance.receivableContainer,
            fraction = 0.74f * stagger(progress, 0)
        )
        LedgerRow(
            label = "You owe",
            amount = "8,150.00",
            icon = Icons.Rounded.ArrowUpward,
            accent = finance.negative,
            container = finance.negativeContainer,
            fraction = 0.48f * stagger(progress, 1)
        )

        HorizontalDivider(color = scheme.outline.copy(alpha = 0.20f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Net position",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant
            )
            Text(
                text = "+4,250.00",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = finance.positive
            )
        }
    }
}

@Composable
private fun LedgerRow(
    label: String,
    amount: String,
    icon: ImageVector,
    accent: Color,
    container: Color,
    fraction: Float
) {
    val scheme = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(accent)
            )
        }
    }
}

// ---- Page 3: the repayment ring ------------------------------------------

@Composable
private fun RepaymentHero(progress: Float) {
    val scheme = MaterialTheme.colorScheme
    val finance = MoneBieTheme.finance
    val sweep = 0.68f * stagger(progress, 0, step = 0f, window = 0.9f)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            ProgressRing(
                sweepFraction = sweep,
                track = scheme.surfaceContainerHighest,
                brush = Brush.linearGradient(listOf(scheme.secondary, scheme.primary)),
                tip = scheme.primary
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(sweep * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
                Text(
                    text = "8 of 12 paid",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        HeroChip(
            text = "Next EMI due in 3 days",
            icon = Icons.Rounded.NotificationsActive,
            content = finance.caution,
            container = finance.cautionContainer
        )
    }
}

@Composable
private fun ProgressRing(
    sweepFraction: Float,
    track: Color,
    brush: Brush,
    tip: Color
) {
    // A slow breath on the ring so the card is never completely static.
    val breathe = rememberInfiniteTransition(label = "ring_breathe")
    val pulse by breathe.animateFloat(
        initialValue = 0.94f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_pulse"
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.size(156.dp)) {
        val stroke = 14.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)

        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            brush = brush,
            startAngle = -90f,
            sweepAngle = 360f * sweepFraction,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // The head of the arc, marked so the eye lands on where progress stops.
        if (sweepFraction > 0.01f) {
            val angle = (-90f + 360f * sweepFraction) * (PI / 180f).toFloat()
            val radius = (size.minDimension - stroke) / 2f
            val head = center + Offset(cos(angle) * radius, sin(angle) * radius)
            drawCircle(color = tip.copy(alpha = 0.22f), radius = stroke * pulse, center = head)
            drawCircle(color = Color.White, radius = stroke / 4f, center = head)
        }
    }
}

// ---- Shared bits ----------------------------------------------------------

@Composable
private fun HeroChip(
    text: String,
    icon: ImageVector,
    content: Color,
    container: Color
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content
        )
    }
}

/**
 * Slices one 0..1 [progress] into overlapping windows so elements arrive one
 * after another instead of all at once.
 */
private fun stagger(
    progress: Float,
    index: Int,
    step: Float = 0.11f,
    window: Float = 0.6f
): Float = ((progress - index * step) / window).coerceIn(0f, 1f)

/**
 * Entrance driver for a hero: runs 0 -> 1 once the page is settled, and winds
 * back when it leaves so returning to a page replays the animation.
 */
@Composable
fun rememberHeroProgress(active: Boolean): Float {
    val progress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (active) 1100 else 250,
            easing = FastOutSlowInEasing
        ),
        label = "hero_progress"
    )
    return progress
}
