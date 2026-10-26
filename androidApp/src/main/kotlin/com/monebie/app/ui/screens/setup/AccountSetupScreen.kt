package com.monebie.app.ui.screens.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Wallet
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.monebie.app.MoneBieApplication
import com.monebie.app.core.Money
import com.monebie.app.domain.model.AccountType
import com.monebie.app.domain.model.displayName
import com.monebie.app.presentation.account.AccountSetupViewModel
import com.monebie.app.ui.components.MoneBieBrandBar
import com.monebie.app.ui.components.MoneBieGlassCard
import com.monebie.app.ui.components.MoneBieGradientButton
import com.monebie.app.ui.components.MoneBieStepPill
import com.monebie.app.ui.components.MoneBieTextField
import com.monebie.app.ui.components.AuroraBackground
import com.monebie.app.ui.components.FormField
import com.monebie.app.ui.components.MicroLabel
import com.monebie.app.ui.components.MoneyInputField
import com.monebie.app.ui.components.MoneyText
import com.monebie.app.ui.components.PremiumCard
import com.monebie.app.ui.components.brandBrush
import com.monebie.app.ui.components.entrance
import com.monebie.app.ui.components.onBrandColor
import com.monebie.app.ui.components.rememberEntrance
import com.monebie.app.ui.theme.MoneBieTheme

@Composable
fun AccountSetupScreen(
    onFinished: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneBieApplication

    val viewModel: AccountSetupViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AccountSetupViewModel(
                    getSelectedCurrencySymbolUseCase = app.appContainer.getSelectedCurrencySymbolUseCase,
                    getCurrentUserUseCase = app.appContainer.getCurrentUserUseCase,
                    createAccountUseCase = app.appContainer.createAccountUseCase
                )
            }
        }
    )

    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var accountName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.BANK) }
    var initialBalance by remember { mutableStateOf("") }

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
                    trailing = { MoneBieStepPill(current = 2, total = 2) }
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
                            text = "Set up your\nfirst account",
                            fontSize = 44.sp,
                            lineHeight = 50.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(MoneBieTheme.spacing.medium))
                        Text(
                            text = "Where your money sits today. Every transaction " +
                                "you record will move a balance like this one.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Spacer(Modifier.height(MoneBieTheme.spacing.large))

                    AccountPreview(
                        name = accountName,
                        type = selectedType,
                        balance = initialBalance,
                        currencySymbol = currencySymbol,
                        modifier = Modifier.entrance(previewIn)
                    )

                    Spacer(Modifier.height(MoneBieTheme.spacing.large))

                    MoneBieGlassCard(modifier = Modifier.entrance(formIn)) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.large)
                        ) {
                            FormField(label = "Account name", error = uiState.nameError) {
                                MoneBieTextField(
                                    value = accountName,
                                    onValueChange = { accountName = it },
                                    placeholder = "e.g. Main Bank Account",
                                    leadingIcon = Icons.Rounded.Edit
                                )
                            }

                            FormField(label = "Account type") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        MoneBieTheme.spacing.small + 4.dp
                                    )
                                ) {
                                    // Only the three asset types: an opening
                                    // account holds money, it does not owe it.
                                    AccountTypeTile(
                                        title = "Bank",
                                        icon = Icons.Rounded.AccountBalance,
                                        isSelected = selectedType == AccountType.BANK,
                                        onClick = { selectedType = AccountType.BANK },
                                        modifier = Modifier.weight(1f)
                                    )
                                    AccountTypeTile(
                                        title = "Digital",
                                        icon = Icons.Rounded.Wallet,
                                        isSelected = selectedType == AccountType.DIGITAL_WALLET,
                                        onClick = { selectedType = AccountType.DIGITAL_WALLET },
                                        modifier = Modifier.weight(1f)
                                    )
                                    AccountTypeTile(
                                        title = "Cash",
                                        icon = Icons.Rounded.Payments,
                                        isSelected = selectedType == AccountType.CASH,
                                        onClick = { selectedType = AccountType.CASH },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            FormField(
                                label = "Opening balance",
                                error = uiState.balanceError,
                                hint = "Leave it empty to start from zero."
                            ) {
                                MoneyInputField(
                                    value = initialBalance,
                                    currencySymbol = currencySymbol,
                                    onValueChange = { initialBalance = it }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(MoneBieTheme.spacing.extraLarge))

                    MoneBieGradientButton(
                        text = "Finish Setup",
                        icon = Icons.Rounded.RocketLaunch,
                        loading = uiState.isSaving,
                        modifier = Modifier.entrance(ctaIn),
                        onClick = {
                            viewModel.createAccount(
                                name = accountName,
                                type = selectedType,
                                balanceInput = initialBalance,
                                onComplete = onFinished
                            )
                        }
                    )

                    Spacer(Modifier.height(MoneBieTheme.spacing.medium))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .entrance(ctaIn),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(MoneBieTheme.spacing.small))
                        Text(
                            text = "Everything stays on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(MoneBieTheme.spacing.large))
                }
            }
        }
    }
}

/**
 * The account as the dashboard will show it, updating while the form is filled.
 *
 * Same idea as the onboarding heroes: let people see the thing they are about
 * to create rather than describing it to them.
 */
@Composable
private fun AccountPreview(
    name: String,
    type: AccountType,
    balance: String,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val trimmedName = name.trim()
    val icon = when (type) {
        AccountType.DIGITAL_WALLET -> Icons.Rounded.Wallet
        AccountType.CASH -> Icons.Rounded.Payments
        else -> Icons.Rounded.AccountBalance
    }

    // The figure goes through the same formatter every other screen uses, so
    // "1000" previews as the "1,000.00" the account list will actually show
    // rather than as the raw keystrokes.
    val parsed = Money.parse(balance.trim().ifEmpty { "0" }) ?: Money.ZERO

    // A real PremiumCard rather than a flat panel: the promise this preview
    // makes is "this is what you will see", and the surface is part of that.
    PremiumCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = MoneBieTheme.spacing.medium
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(brandBrush()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onBrandColor(),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(MoneBieTheme.spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trimmedName.ifEmpty { "Your first account" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (trimmedName.isEmpty()) {
                        scheme.onSurfaceVariant
                    } else {
                        scheme.onSurface
                    },
                    maxLines = 1
                )
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                MoneyText(
                    money = parsed,
                    currencySymbol = currencySymbol,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(2.dp))
                MicroLabel(text = "available")
            }
        }
    }
}

@Composable
private fun AccountTypeTile(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        label = "tile_border_width"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) scheme.primary else scheme.outline.copy(alpha = 0.3f),
        label = "tile_border_color"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) scheme.primaryContainer else scheme.surfaceContainerLow,
        label = "tile_container"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        label = "tile_content"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.96f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tile_scale"
    )

    Box(
        modifier = modifier
            .height(94.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(containerColor)
            .border(borderWidth, borderColor, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(MoneBieTheme.spacing.small))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }

        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut() + scaleOut(targetScale = 0.5f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(brandBrush()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = onBrandColor(),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
