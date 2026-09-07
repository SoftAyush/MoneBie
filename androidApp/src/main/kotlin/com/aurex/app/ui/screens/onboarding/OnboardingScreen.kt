package com.aurex.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aurex.app.presentation.onboarding.OnboardingViewModel
import com.aurex.app.ui.components.AurexBrandBar
import com.aurex.app.ui.components.AurexGradientButton
import com.aurex.app.ui.components.AurexStepDots
import com.aurex.app.ui.components.AuroraBackground
import com.aurex.app.ui.components.brandBrush
import com.aurex.app.ui.components.onBrandColor
import com.aurex.app.ui.theme.AurexTheme
import kotlin.math.absoluteValue

/**
 * The first thing anyone sees.
 *
 * Three swipeable pages over a slowly drifting aurora, each fronted by a
 * miniature of a real Aurex screen that draws itself in on arrival. The pager
 * and the view model stay in step in both directions, so swiping, tapping Next
 * and Skip all move the same single source of truth.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pages = uiState.pages
    if (pages.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = uiState.currentPageIndex) { pages.size }

    // Swipe drives the view model...
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect(viewModel::goToPage)
    }
    // ...and the view model drives the pager, so Next and Skip animate too.
    LaunchedEffect(uiState.currentPageIndex) {
        if (pagerState.currentPage != uiState.currentPageIndex) {
            pagerState.animateScrollToPage(uiState.currentPageIndex)
        }
    }

    val isLastPage = uiState.currentPageIndex == pages.lastIndex

    AurexTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AuroraBackground(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeContentPadding()
            ) {
                TopBar(
                    skipVisible = !isLastPage,
                    onSkip = { viewModel.skip(onFinished) }
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    // 0 for the settled page, +-1 for its neighbours, fractional
                    // mid-drag - the one number every parallax below reads from.
                    val offset =
                        (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    PageContent(
                        title = pages[page].title,
                        description = pages[page].description,
                        imageResName = pages[page].imageResName,
                        offset = offset,
                        settled = pagerState.settledPage == page
                    )
                }

                AurexStepDots(
                    total = pages.size,
                    current = uiState.currentPageIndex,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = AurexTheme.spacing.large)
                )

                BottomBar(
                    stepLabel = "%02d / %02d".format(uiState.currentPageIndex + 1, pages.size),
                    fraction = (uiState.currentPageIndex + 1f) / pages.size,
                    isLastPage = isLastPage,
                    onNext = { viewModel.nextStep(onFinished) },
                    modifier = Modifier.padding(
                        horizontal = AurexTheme.spacing.large,
                        vertical = AurexTheme.spacing.medium
                    )
                )
            }
        }
    }
}

// ---- Page -----------------------------------------------------------------

@Composable
private fun PageContent(
    title: String,
    description: String,
    imageResName: String,
    offset: Float,
    settled: Boolean
) {
    val distance = offset.absoluteValue.coerceIn(0f, 1f)
    val progress = rememberHeroProgress(active = settled)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AurexTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // The hero trails the swipe; the copy keeps up with it. Two speeds is
        // what gives the stack depth instead of a flat slide.
        Box(
            // fill = false so a short screen shrinks the hero instead of
            // clipping the copy underneath it.
            modifier = Modifier
                .weight(1f, fill = false)
                .graphicsLayer {
                    translationX = offset * size.width * 0.28f
                    alpha = 1f - distance
                    val scale = lerp(0.86f, 1f, 1f - distance)
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            OnboardingHero(imageResName = imageResName, progress = progress)
        }

        Spacer(Modifier.height(AurexTheme.spacing.extraLarge))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                translationX = offset * size.width * 0.06f
                alpha = (1f - distance * 1.8f).coerceIn(0f, 1f)
            }
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(AurexTheme.spacing.medium))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = AurexTheme.spacing.small)
            )
        }
    }
}

// ---- Chrome ---------------------------------------------------------------

@Composable
private fun TopBar(skipVisible: Boolean, onSkip: () -> Unit) {
    val skipAlpha by animateFloatAsState(
        targetValue = if (skipVisible) 1f else 0f,
        animationSpec = tween(220),
        label = "skip_alpha"
    )

    AurexBrandBar(
        trailing = {
            Text(
                text = "Skip",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .graphicsLayer { alpha = skipAlpha }
                    .clip(CircleShape)
                    .then(if (skipVisible) Modifier.clickable(onClick = onSkip) else Modifier)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    )
}

@Composable
private fun BottomBar(
    stepLabel: String,
    fraction: Float,
    isLastPage: Boolean,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = isLastPage,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = {
            (fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.92f))
                .togetherWith(fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.92f))
        },
        label = "cta_morph"
    ) { last ->
        if (last) {
            AurexGradientButton(
                text = "Get Started",
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                onClick = onNext
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                NextRingButton(fraction = fraction, onClick = onNext)
            }
        }
    }
}

/**
 * The circular Next control: how far through onboarding you are, drawn as an
 * arc around the button you press to go further.
 */
@Composable
private fun NextRingButton(fraction: Float, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val sweep by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ring_sweep"
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press_scale"
    )

    val trackColor = scheme.onSurfaceVariant.copy(alpha = 0.20f)
    val arcBrush = brandBrush()

    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2f
            val arcSize = androidx.compose.ui.geometry.Size(
                size.width - stroke,
                size.height - stroke
            )
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
            drawArc(
                brush = arcBrush,
                startAngle = -90f,
                sweepAngle = 360f * sweep.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(arcBrush)
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Next",
                tint = onBrandColor()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    OnboardingScreen(onFinished = {})
}
