package com.monebie.app.ui.screens.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.monebie.app.MoneBieApplication
import com.monebie.app.domain.model.Currency
import com.monebie.app.presentation.profile.ProfileSetupViewModel
import com.monebie.app.ui.components.MoneBieBrandBar
import com.monebie.app.ui.components.MoneBieGlassCard
import com.monebie.app.ui.components.MoneBieGradientButton
import com.monebie.app.ui.components.MoneBieStepPill
import com.monebie.app.ui.components.MoneBieTextField
import com.monebie.app.ui.components.AuroraBackground
import com.monebie.app.ui.components.CurrencySelector
import com.monebie.app.ui.components.FormField
import com.monebie.app.ui.components.PremiumCard
import com.monebie.app.ui.components.brandBrush
import com.monebie.app.ui.components.entrance
import com.monebie.app.ui.components.onBrandColor
import com.monebie.app.ui.components.rememberEntrance
import com.monebie.app.ui.theme.MoneBieTheme

@Composable
fun ProfileSetupScreen(
    onFinished: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneBieApplication

    val viewModel: ProfileSetupViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ProfileSetupViewModel(
                    getCurrenciesUseCase = app.appContainer.getCurrenciesUseCase,
                    saveUserUseCase = app.appContainer.saveUserUseCase
                )
            }
        })

    val currencies by viewModel.currencies.collectAsStateWithLifecycle()

    ProfileSetupContent(
        currencies = currencies,
        onSaveProfile = { name, currencyCode ->
            viewModel.saveUserProfile(
                name = name,
                currencyCode = currencyCode,
                onComplete = onFinished
            )
        }
    )
}

@Composable
fun ProfileSetupContent(
    currencies: List<Currency>,
    onSaveProfile: (name: String, currencyCode: String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }

    // Default selection: first DB currency, or a safe placeholder while loading
    var selectedCurrency by remember(currencies) {
        mutableStateOf(
            currencies.firstOrNull() ?: Currency(
                code = "NPR", symbol = "रु", name = "Nepalese Rupee", flag = "🇳🇵", popular = true
            ),
        )
    }

    val headerIn = rememberEntrance(delayMillis = 80)
    val previewIn = rememberEntrance(delayMillis = 200)
    val formIn = rememberEntrance(delayMillis = 320)
    val ctaIn = rememberEntrance(delayMillis = 440)

    MoneBieTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AuroraBackground(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeContentPadding()
                    .imePadding()
            ) {
                MoneBieBrandBar(
                    trailing = { MoneBieStepPill(current = 1, total = 2) }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = MoneBieTheme.spacing.large)
                        .navigationBarsPadding()
                ) {
                    Spacer(Modifier.height(MoneBieTheme.spacing.large))

                    Column(modifier = Modifier.entrance(headerIn)) {
                        Text(
                            text = "Let's get to\nknow you",
                            fontSize = 44.sp,
                            lineHeight = 50.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(MoneBieTheme.spacing.medium))
                        Text(
                            text = "Your name and base currency shape every figure " +
                                "MoneBie shows you from here on.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Spacer(Modifier.height(MoneBieTheme.spacing.large))

                    GreetingPreview(
                        name = fullName,
                        currency = selectedCurrency,
                        modifier = Modifier.entrance(previewIn)
                    )

                    Spacer(Modifier.height(MoneBieTheme.spacing.large))

                    MoneBieGlassCard(modifier = Modifier.entrance(formIn)) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.large)
                        ) {
                            FormField(label = "Full name", error = nameError) {
                                MoneBieTextField(
                                    value = fullName,
                                    onValueChange = {
                                        fullName = it
                                        if (nameError != null) nameError = null
                                    },
                                    placeholder = "e.g. Jane Doe"
                                )
                            }

                            FormField(
                                label = "Preferred currency",
                                hint = "You can change your base currency later in settings."
                            ) {
                                CurrencySelector(
                                    currencies = currencies,
                                    selected = selectedCurrency,
                                    onCurrencySelected = { selectedCurrency = it }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(MoneBieTheme.spacing.extraLarge))

                    MoneBieGradientButton(
                        text = "Continue",
                        icon = Icons.AutoMirrored.Rounded.ArrowForward,
                        modifier = Modifier.entrance(ctaIn),
                        onClick = {
                            val trimmed = fullName.trim()
                            if (trimmed.isEmpty()) {
                                nameError = "Tell us what to call you"
                            } else {
                                onSaveProfile(trimmed, selectedCurrency.code)
                            }
                        }
                    )

                    Spacer(Modifier.height(MoneBieTheme.spacing.large))
                }
            }
        }
    }
}

/**
 * A live echo of what the user is typing.
 *
 * The point is reassurance before commitment: the greeting and the currency
 * symbol here are exactly what the dashboard will greet them with, so the two
 * fields stop being a form and start being a preview.
 */
@Composable
private fun GreetingPreview(
    name: String,
    currency: Currency,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val trimmed = name.trim()

    // A slow breath, so an untouched screen still feels alive.
    val breathe = rememberInfiniteTransition(label = "greeting_breathe")
    val glow by breathe.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "greeting_glow"
    )

    // A real PremiumCard rather than a flat panel: the promise this preview
    // makes is "this is what you will see", and the surface is part of that.
    PremiumCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = MoneBieTheme.spacing.medium
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer { alpha = glow }
                    .clip(CircleShape)
                    .background(scheme.primary.copy(alpha = 0.18f))
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(brandBrush()),
                contentAlignment = Alignment.Center
            ) {
                if (trimmed.isEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.PersonOutline,
                        contentDescription = null,
                        tint = onBrandColor(),
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = trimmed.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onBrandColor()
                    )
                }
            }
        }

        Spacer(Modifier.width(MoneBieTheme.spacing.medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (trimmed.isEmpty()) "Hello there" else "Hi, $trimmed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 1
            )
            Text(
                text = "Amounts shown in ${currency.code}",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(scheme.primaryContainer)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = currency.flag, fontSize = 14.sp)
            Text(
                text = currency.symbol,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onPrimaryContainer
            )
        }
      }
    }
}


@Preview(showBackground = true)
@Composable
private fun ProfileSetupScreenPreview() {
    ProfileSetupContent(
        currencies = listOf(
            Currency("NPR", "रु", "Nepalese Rupee", "🇳🇵", true),
            Currency("USD", "$", "US Dollar", "🇺🇸", true)
        ),
        onSaveProfile = { _, _ -> }
    )
}
