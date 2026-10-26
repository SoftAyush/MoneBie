package com.aurex.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurex.app.core.Money
import com.aurex.app.core.MoneyFormatter
import com.aurex.app.ui.theme.DarkFinanceColors
import com.aurex.app.ui.theme.FinanceColors
import com.aurex.app.ui.theme.Palette500
import com.aurex.app.ui.theme.Palette700
import com.aurex.app.ui.theme.Palette900

/*
 * The elevated visual layer.
 *
 * The plain [AurexCard] surfaces still carry the dense working screens, where a
 * user is scanning for one figure. The screens a user opens every day - the
 * dashboard, the ledger - are allowed the depth those are not: an ambient
 * backdrop, a deep brand-filled hero, and cards that sit above the page rather
 * than being drawn onto it.
 *
 * It lives here, defined once, rather than inside the first screen that wanted
 * it: the effect only reads as deliberate when the light falls the same way on
 * every surface of every screen that uses it.
 */

// -----------------------------------------------------------------------------
// Surfaces
// -----------------------------------------------------------------------------

/** Corner radius shared by every elevated card, so the rhythm never breaks. */
val PremiumCardShape = RoundedCornerShape(24.dp)

/** The hero carries a larger radius - it is the one surface that leads the page. */
val HeroCardShape = RoundedCornerShape(30.dp)

/**
 * A soft, unmoving wash of brand light behind the whole page.
 *
 * Two low-alpha radial lights over the scheme background. It is deliberately
 * not animated: the dashboard is a scrolling surface, and a drifting backdrop
 * would repaint on every frame of every scroll for an effect nobody looks
 * straight at.
 */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val dark = isDarkScheme()

    // A glow tuned for a near-white page blows out on a navy one.
    val primaryAlpha = if (dark) 0.22f else 0.13f
    val secondaryAlpha = if (dark) 0.16f else 0.10f

    Canvas(modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(scheme.surfaceContainerLow, scheme.background, scheme.background)
            )
        )

        val top = Offset(size.width * 0.14f, size.height * -0.04f)
        val topRadius = size.width * 1.05f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(scheme.primary.copy(alpha = primaryAlpha), Color.Transparent),
                center = top,
                radius = topRadius
            ),
            radius = topRadius,
            center = top
        )

        val right = Offset(size.width * 1.02f, size.height * 0.10f)
        val rightRadius = size.width * 0.85f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(scheme.secondary.copy(alpha = secondaryAlpha), Color.Transparent),
                center = right,
                radius = rightRadius
            ),
            radius = rightRadius,
            center = right
        )
    }
}

/**
 * The elevated card.
 *
 * Unlike the flat outlined card the module screens use, this one is lifted: a
 * top-lit gradient fill, a hairline border, and a wide, faint brand-tinted
 * shadow. The lift is what separates a card from the ambient backdrop without
 * needing a heavy border to do it. On the dark scheme the shadow is dropped -
 * a black shadow on a near-black page is only wasted work.
 */
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 18.dp,
    shape: RoundedCornerShape = PremiumCardShape,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val dark = isDarkScheme()

    val fill = if (dark) {
        Brush.verticalGradient(listOf(scheme.surfaceContainer, scheme.surface))
    } else {
        Brush.verticalGradient(listOf(scheme.surface, scheme.surfaceContainerLow))
    }

    Column(
        modifier = modifier
            .shadow(
                elevation = if (dark) 0.dp else 10.dp,
                shape = shape,
                ambientColor = Palette900.copy(alpha = 0.5f),
                spotColor = Palette700.copy(alpha = 0.5f)
            )
            .clip(shape)
            .background(fill)
            .border(1.dp, scheme.outline.copy(alpha = if (dark) 0.26f else 0.14f), shape)
            .then(if (onClick != null) Modifier.pressable(onClick) else Modifier)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Tap handling with a spring under the thumb.
 *
 * These screens are almost entirely tappable cards, and without the press they
 * feel like a picture of an app rather than an app.
 */
@Composable
fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press_scale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            onClick = onClick
        )
}

// -----------------------------------------------------------------------------
// The hero fill
// -----------------------------------------------------------------------------

/**
 * The deep fill behind the balance.
 *
 * Runs navy to mid blue diagonally and stops there: the palette's cyan is a
 * light colour, and white on it lands near 2.4:1. Cyan appears only as a glow
 * in the corner opposite the figures, where no text ever sits on it.
 */
@Composable
fun heroBrush(): Brush = if (isDarkScheme()) {
    Brush.linearGradient(listOf(Color(0xFF0A2141), Color(0xFF00618E)))
} else {
    Brush.linearGradient(listOf(Palette900, Palette700))
}

/** The one text colour legible across the whole of [heroBrush]. */
val OnHero = Color(0xFFFFFFFF)

/**
 * Money colours for use *on* the hero.
 *
 * The hero is a dark surface in both themes, so it always takes the dark
 * scheme's brighter green and red - the light scheme's #0F7A4A on navy is
 * unreadable.
 */
val HeroFinance: FinanceColors = DarkFinanceColors

/**
 * Light and geometry on the hero: a cyan glow off the top-right corner and
 * concentric rings running out of the bottom-right, which is what stops the
 * card reading as a flat rectangle of blue.
 */
@Composable
fun HeroDecoration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val glow = Offset(size.width * 0.94f, size.height * -0.06f)
        val glowRadius = size.minDimension * 1.25f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Palette500.copy(alpha = 0.40f), Color.Transparent),
                center = glow,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = glow
        )

        val rings = Offset(size.width * 1.04f, size.height * 0.94f)
        val stroke = Stroke(width = 1.5.dp.toPx())
        listOf(0.34f, 0.52f, 0.72f, 0.94f).forEach { factor ->
            drawCircle(
                color = Color.White.copy(alpha = 0.07f),
                radius = size.minDimension * factor,
                center = rings,
                style = stroke
            )
        }
    }
}

/** A frosted panel carrying one supporting figure inside the hero. */
@Composable
fun HeroTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnHero.copy(alpha = 0.70f),
            maxLines = 1
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// -----------------------------------------------------------------------------
// Figures
// -----------------------------------------------------------------------------

/**
 * Counts a figure up from where it was to where it is.
 *
 * Interpolation runs through Double rather than Float: a Float carries about
 * seven digits, and a balance in the minor units of a low-denomination currency
 * passes that inside six figures - which would make the trailing digits of a
 * real balance visibly wrong mid-count.
 */
@Composable
private fun animatedMinorUnits(target: Long): Long {
    val progress = remember { Animatable(0f) }
    val from = remember { mutableStateOf(0L) }
    val to = remember { mutableStateOf(target) }

    val displayed = from.value + ((to.value - from.value).toDouble() * progress.value).toLong()

    LaunchedEffect(target) {
        // Runs on first appearance (0 -> value) and on every later change,
        // always starting from whatever is on screen at that moment.
        from.value = displayed
        to.value = target
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }

    return displayed
}

/**
 * A monetary figure that counts into place, and can be masked.
 *
 * Masking is one tap on the hero's eye: a balance is the single thing on this
 * screen a user may not want visible to whoever is sitting beside them.
 */
@Composable
fun CountUpMoneyText(
    money: Money,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displaySmall,
    color: Color = MaterialTheme.colorScheme.onSurface,
    showDecimals: Boolean = true,
    signed: Boolean = false,
    masked: Boolean = false
) {
    val displayed = Money(animatedMinorUnits(money.minorUnits))

    Text(
        text = if (masked) {
            "$currencySymbol " + "•".repeat(6)
        } else {
            MoneyFormatter.format(
                displayed,
                currencySymbol,
                showDecimals = showDecimals,
                signed = signed
            )
        },
        style = style,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** The masked stand-in for a figure, so tiles and rows hide consistently. */
fun maskedValue(): String = "•".repeat(4)

// -----------------------------------------------------------------------------
// Section furniture
// -----------------------------------------------------------------------------

/**
 * Section title with a brand tick and an optional trailing action.
 *
 * Used instead of the plain SectionHeader wherever a section
 * break has to hold against an ambient backdrop: the tick and the pill do
 * that where a bare line of bold text does not.
 */
@Composable
fun PremiumSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    /**
     * Something to show at the end of the row that is not a place to tap - a
     * subtotal, a count. Ignored when an action pill is given, since the two
     * would compete for the same corner.
     */
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 18.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (actionLabel != null && onActionClick != null) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .clickable(onClick = onActionClick)
                    .padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else if (trailing != null) {
            trailing()
        }
    }
}

/** Uppercase micro-label used above the figures inside a card. */
@Composable
fun MicroLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = color,
        maxLines = 1,
        modifier = modifier
    )
}

/**
 * Progress track filled with a gradient rather than a flat colour, and animated
 * to its value so a ratio that moved is seen rather than merely found.
 */
@Composable
fun GradientTrack(
    progress: Float,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val fraction by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "track_fill"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(height)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(colors))
        )
    }
}

/** Hairline separating figures and rows inside a card. */
@Composable
fun CardDivider(modifier: Modifier = Modifier, alpha: Float = 0.10f) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
    )
}

/** Circular icon plate with a soft tinted ring, used as a row's leading mark. */
@Composable
fun GlowBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(tint.copy(alpha = 0.24f), tint.copy(alpha = 0.10f))
                )
            )
            .border(1.dp, tint.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

/**
 * Filter chip for the premium screens.
 *
 * A selected chip takes the brand gradient rather than a flat tint: on a page
 * with an ambient backdrop behind it, a filled pill is the only treatment that
 * still reads as switched on at a glance.
 */
@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color? = null,
    /** How many rows the filter would leave, shown after the label when given. */
    count: Int? = null,
    /**
     * A pill that leads nowhere stops being a button.
     *
     * Tapping a filter to find out it is empty is a wasted trip; greying it out
     * answers the question from the same glance that asked it.
     */
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val content = when {
        selected -> onBrandColor()
        !enabled -> scheme.onSurfaceVariant.copy(alpha = 0.38f)
        tint != null -> tint
        else -> scheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(brandBrush())
                } else {
                    Modifier
                        .background(scheme.surface.copy(alpha = if (enabled) 0.9f else 0.45f))
                        .border(
                            1.dp,
                            (tint ?: scheme.outline).copy(
                                alpha = when {
                                    !enabled -> 0.10f
                                    tint != null -> 0.35f
                                    else -> 0.20f
                                }
                            ),
                            CircleShape
                        )
                }
            )
            .then(if (enabled) Modifier.pressable(onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = content,
            maxLines = 1
        )
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                // Quieter than the label: the count qualifies the filter, it is
                // not a second thing to read.
                color = content.copy(alpha = if (selected) 0.85f else 0.60f),
                maxLines = 1
            )
        }
    }
}

/**
 * Circular glass control.
 *
 * The one button shape that holds up over an ambient backdrop or a photo-dark
 * hero: a translucent surface plate with a hairline ring, tinted by whatever it
 * does. Disabled rather than hidden, so a row of controls never shifts under
 * the thumb.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 42.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.3f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .border(1.dp, tint.copy(alpha = 0.22f), CircleShape)
            .then(if (enabled) Modifier.pressable(onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

/**
 * A frosted line inside a hero, carrying one thing worth saying about it.
 *
 * Lives on the hero rather than under it because the note qualifies the figure
 * above it - what is overdue, what a filter is hiding - and a line that has
 * left the card no longer reads as being about it.
 */
@Composable
fun HeroNote(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    accent: Color? = null
) {
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, (accent ?: Color.White).copy(alpha = 0.25f), shape)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent ?: OnHero.copy(alpha = 0.75f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (accent != null) FontWeight.SemiBold else FontWeight.Normal,
            color = accent ?: OnHero.copy(alpha = 0.85f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * The pinned bar on a detail screen: back, what you are looking at, and what
 * you can do to it.
 *
 * Glass controls rather than a filled app bar - the page beneath is an ambient
 * wash, and a solid bar across the top of it would cut the light in half.
 */
@Composable
fun DetailTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GlassIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            onClick = onBack
        )

        Spacer(Modifier.width(4.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                MicroLabel(text = subtitle, color = MaterialTheme.colorScheme.primary)
            }
        }

        actions()
    }
}

/**
 * Progress on a hero: a white track with a gradient fill, animated to its value
 * so a ratio that moved is seen rather than merely found.
 *
 * Separate from [GradientTrack] because the hero is a dark surface in both
 * themes - an onSurface track would vanish into it.
 */
@Composable
fun HeroTrack(
    progress: Float,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val fraction by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "hero_track"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(height)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(colors))
        )
    }
}

/**
 * A short state marker for the hero's top corner.
 *
 * Frosted rather than filled: the hero already carries a gradient, and a solid
 * pill on top of it reads as a second surface rather than a label on this one.
 */
@Composable
fun HeroPill(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color? = null
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = accent ?: OnHero.copy(alpha = 0.85f),
        maxLines = 1,
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .then(
                if (accent != null) {
                    Modifier.border(1.dp, accent.copy(alpha = 0.35f), CircleShape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/**
 * A small frosted control on the hero itself - hide a balance, open something
 * the hero is about.
 *
 * White-on-white rather than themed: the hero is a dark surface in both
 * schemes, and a scheme-coloured button on it would flip with the theme while
 * the surface under it does not.
 */
@Composable
fun HeroIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .pressable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = OnHero.copy(alpha = 0.9f),
            modifier = Modifier.size(17.dp)
        )
    }
}

/**
 * Two opposed quantities as one bar, meeting where they balance.
 *
 * Each side takes the share of the width its value represents, and the tick
 * marks the halfway point - so a left band past the centre means that side is
 * the larger one, read before either figure is. Both sides are summed to size
 * it rather than the larger one alone, which would make an empty side look
 * like a full one.
 */
@Composable
fun HeroSplitBar(
    leftValue: Long,
    rightValue: Long,
    leftColor: Color,
    rightColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp
) {
    val total = leftValue.coerceAtLeast(0L) + rightValue.coerceAtLeast(0L)
    val target = if (total <= 0L) 0.5f else (leftValue.toDouble() / total).toFloat()

    val share by animateFloatAsState(
        targetValue = target.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "hero_split"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val radius = CornerRadius(size.height / 2f)
        val gap = 3.dp.toPx()

        drawRoundRect(
            color = Color.White.copy(alpha = 0.18f),
            size = size,
            cornerRadius = radius
        )

        if (total > 0L) {
            val split = size.width * share

            if (split > gap) {
                drawRoundRect(
                    color = leftColor,
                    topLeft = Offset.Zero,
                    size = Size((split - gap / 2f).coerceAtLeast(0f), size.height),
                    cornerRadius = radius
                )
            }
            if (size.width - split > gap) {
                drawRoundRect(
                    color = rightColor,
                    topLeft = Offset(split + gap / 2f, 0f),
                    size = Size(size.width - split - gap / 2f, size.height),
                    cornerRadius = radius
                )
            }
        }

        // The balance point. Drawn last so neither side can cover it.
        val tick = 2.dp.toPx()
        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(size.width / 2f - tick / 2f, -1.dp.toPx()),
            size = Size(tick, size.height + 2.dp.toPx()),
            cornerRadius = CornerRadius(tick / 2f)
        )
    }
}
