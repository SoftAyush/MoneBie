package com.monebie.app.ui.screens.overview

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.monebie.app.MoneBieApplication
import com.monebie.app.core.Money
import com.monebie.app.core.MoneyFormatter
import com.monebie.app.domain.model.FinancialOverview
import com.monebie.app.domain.model.PositionGroup
import com.monebie.app.domain.model.PositionGroupKey
import com.monebie.app.presentation.overview.FinancialOverviewUiState
import com.monebie.app.presentation.overview.FinancialOverviewViewModel
import com.monebie.app.ui.components.AmbientBackdrop
import com.monebie.app.ui.components.CardDivider
import com.monebie.app.ui.components.CountUpMoneyText
import com.monebie.app.ui.components.DetailTopBar
import com.monebie.app.ui.components.EmptyState
import com.monebie.app.ui.components.ErrorState
import com.monebie.app.ui.components.FormNotice
import com.monebie.app.ui.components.GlowBadge
import com.monebie.app.ui.components.GradientTrack
import com.monebie.app.ui.components.HeroCardShape
import com.monebie.app.ui.components.HeroDecoration
import com.monebie.app.ui.components.HeroFinance
import com.monebie.app.ui.components.HeroPill
import com.monebie.app.ui.components.HeroSplitBar
import com.monebie.app.ui.components.HeroTile
import com.monebie.app.ui.components.MicroLabel
import com.monebie.app.ui.components.MoneyText
import com.monebie.app.ui.components.OnHero
import com.monebie.app.ui.components.PremiumCard
import com.monebie.app.ui.components.PremiumSectionHeader
import com.monebie.app.ui.components.ShimmerBlock
import com.monebie.app.ui.components.heroBrush
import com.monebie.app.ui.theme.MoneBieTheme
import com.monebie.app.ui.theme.Palette700
import com.monebie.app.ui.theme.Palette900

/**
 * The complete balance sheet: what the user owns, what they owe, and the
 * difference.
 *
 * Every group can be expanded to the individual records behind it, so a figure
 * is never something the app merely asserts - the user can see exactly which
 * accounts, debts and people add up to it.
 */
@Composable
fun FinancialOverviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneBieApplication

    val viewModel: FinancialOverviewViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                FinancialOverviewViewModel(
                    getFinancialOverviewUseCase = app.appContainer.getFinancialOverviewUseCase
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Expansion is view state and belongs to the screen, not the ViewModel.
    val expanded = remember { mutableStateMapOf<PositionGroupKey, Boolean>() }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                title = "Financial position",
                onBack = onBack,
                subtitle = "Estimated balance sheet"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = MoneBieTheme.spacing.medium,
                    end = MoneBieTheme.spacing.medium,
                    top = MoneBieTheme.spacing.small,
                    bottom = MoneBieTheme.spacing.extraExtraLarge
                ),
                verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small)
            ) {
                when (val state = uiState) {
                    is FinancialOverviewUiState.Loading -> item(key = "skeleton") {
                        OverviewSkeleton()
                    }

                    is FinancialOverviewUiState.Error -> item(key = "error") {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            ErrorState(message = state.message, icon = Icons.Rounded.CloudOff)
                        }
                    }

                    is FinancialOverviewUiState.Content -> {
                        val overview = state.overview

                        if (!overview.hasData) {
                            item(key = "empty") {
                                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                                    EmptyState(
                                        icon = Icons.Rounded.QueryStats,
                                        title = "Nothing to weigh up yet",
                                        message = "Once you have accounts, debts or loans " +
                                            "recorded, your full financial position will be " +
                                            "worked out here."
                                    )
                                }
                            }
                            return@LazyColumn
                        }

                        item(key = "hero") { NetPositionHero(overview) }

                        item(key = "assets_header") {
                            SideHeader(
                                title = "Assets",
                                total = overview.totalAssets,
                                currencySymbol = overview.currencySymbol,
                                accent = MoneBieTheme.finance.positive
                            )
                        }

                        items(overview.assetGroups, key = { "asset_" + it.key.name }) { group ->
                            PositionGroupCard(
                                group = group,
                                sideTotal = overview.totalAssets,
                                currencySymbol = overview.currencySymbol,
                                accent = MoneBieTheme.finance.positive,
                                isExpanded = expanded[group.key] == true,
                                onToggle = { expanded[group.key] = expanded[group.key] != true }
                            )
                        }

                        item(key = "liabilities_header") {
                            SideHeader(
                                title = "Liabilities",
                                total = overview.totalLiabilities,
                                currencySymbol = overview.currencySymbol,
                                accent = MoneBieTheme.finance.negative
                            )
                        }

                        if (overview.liabilityGroups.isEmpty()) {
                            item(key = "no_liabilities") {
                                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "You owe nothing. Everything you have is yours.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(
                            overview.liabilityGroups,
                            key = { "liability_" + it.key.name }
                        ) { group ->
                            PositionGroupCard(
                                group = group,
                                sideTotal = overview.totalLiabilities,
                                currencySymbol = overview.currencySymbol,
                                accent = MoneBieTheme.finance.negative,
                                isExpanded = expanded[group.key] == true,
                                onToggle = { expanded[group.key] = expanded[group.key] != true }
                            )
                        }

                        item(key = "footnote") {
                            Spacer(Modifier.height(MoneBieTheme.spacing.small))
                            MethodologyNote()
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// The hero
// -----------------------------------------------------------------------------

/**
 * What is left when everything is counted on both sides.
 *
 * The split bar carries what the net figure cannot: two people with the same
 * net position can be in very different shape, one holding nothing and owing
 * nothing, the other with large amounts running on both sides at once.
 */
@Composable
private fun NetPositionHero(
    overview: FinancialOverview,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 22.dp,
                shape = HeroCardShape,
                ambientColor = Palette900,
                spotColor = Palette700
            )
            .clip(HeroCardShape)
            .background(heroBrush())
    ) {
        HeroDecoration(Modifier.matchParentSize())

        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MicroLabel(
                    text = "Estimated net position",
                    color = OnHero.copy(alpha = 0.75f)
                )
                Spacer(Modifier.weight(1f))
                HeroPill(
                    text = if (overview.isPositive) "In the black" else "In the red",
                    accent = if (overview.isPositive) {
                        HeroFinance.positive
                    } else {
                        HeroFinance.negative
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            CountUpMoneyText(
                money = overview.net,
                currencySymbol = overview.currencySymbol,
                style = MaterialTheme.typography.displaySmall,
                color = if (overview.isPositive) OnHero else HeroFinance.negative
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Total assets minus everything you owe",
                style = MaterialTheme.typography.bodySmall,
                color = OnHero.copy(alpha = 0.75f)
            )

            Spacer(Modifier.height(18.dp))

            HeroSplitBar(
                leftValue = overview.totalAssets.minorUnits,
                rightValue = overview.totalLiabilities.minorUnits,
                leftColor = HeroFinance.positive,
                rightColor = HeroFinance.negative
            )

            Spacer(Modifier.height(8.dp))

            // Direct labels under their own ends of the bar, so the two sides
            // are named without a legend box to look up.
            Row(verticalAlignment = Alignment.CenterVertically) {
                SideLabel(
                    label = "Assets",
                    value = MoneyFormatter.compact(
                        overview.totalAssets,
                        overview.currencySymbol
                    ),
                    color = HeroFinance.positive
                )
                Spacer(Modifier.weight(1f))
                SideLabel(
                    label = "Liabilities",
                    value = MoneyFormatter.compact(
                        overview.totalLiabilities,
                        overview.currencySymbol
                    ),
                    color = HeroFinance.negative
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile(
                    label = "Available now",
                    value = MoneyFormatter.compact(
                        overview.liquidAssets,
                        overview.currencySymbol
                    ),
                    valueColor = OnHero,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Due this month",
                    value = MoneyFormatter.compact(
                        overview.obligationsThisMonth,
                        overview.currencySymbol
                    ),
                    valueColor = OnHero,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Owed vs owned",
                    value = MoneyFormatter.percent(overview.liabilityRatio),
                    valueColor = if (overview.liabilityRatio > 0.85f) {
                        HeroFinance.caution
                    } else {
                        OnHero
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SideLabel(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            // Text keeps the hero's ink; the dot beside it carries the side.
            color = OnHero.copy(alpha = 0.85f),
            maxLines = 1
        )
    }
}

// -----------------------------------------------------------------------------
// The two sides
// -----------------------------------------------------------------------------

/** A side of the sheet, with its total stated where the eye starts reading. */
@Composable
private fun SideHeader(
    title: String,
    total: Money,
    currencySymbol: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MoneBieTheme.spacing.small, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(CircleShape)
                .background(accent)
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.weight(1f))

        MoneyText(
            money = total,
            currencySymbol = currencySymbol,
            style = MaterialTheme.typography.titleSmall,
            color = accent,
            showDecimals = false
        )
    }
}

/**
 * One group on one side, and the records behind it.
 *
 * The bar is the group's share of its own side rather than of everything, so
 * the two sides can be read independently - and the header above states which
 * side's total that share is measured against.
 */
@Composable
private fun PositionGroupCard(
    group: PositionGroup,
    sideTotal: Money,
    currencySymbol: String,
    accent: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    PremiumCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        onClick = if (group.hasDetail) onToggle else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
        ) {
            GlowBadge(icon = iconFor(group.key), tint = accent)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = MoneyFormatter.percent(group.shareOf(sideTotal)) + " of this side" +
                        if (group.hasDetail) {
                            " · " + group.lines.size +
                                if (group.lines.size == 1) " item" else " items"
                        } else {
                            ""
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            MoneyText(
                money = group.total,
                currencySymbol = currencySymbol,
                style = MaterialTheme.typography.titleMedium,
                showDecimals = false
            )

            if (group.hasDetail) {
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Rounded.ExpandLess
                    } else {
                        Icons.Rounded.ExpandMore
                    },
                    contentDescription = if (isExpanded) "Hide detail" else "Show detail",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.medium))

        GradientTrack(
            progress = group.shareOf(sideTotal),
            colors = listOf(accent.copy(alpha = 0.55f), accent),
            height = 6.dp
        )

        if (isExpanded && group.hasDetail) {
            Spacer(Modifier.height(MoneBieTheme.spacing.medium))
            CardDivider(alpha = 0.07f)

            group.lines.forEach { line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = line.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        line.sublabel?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    MoneyText(
                        money = line.amount,
                        currencySymbol = currencySymbol,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        showDecimals = false
                    )
                }
            }
        }
    }
}

/**
 * States what the figure does and does not include.
 *
 * The spec calls it an *estimated* position; saying why keeps the number
 * honest rather than letting it read as an audited valuation.
 */
@Composable
private fun MethodologyNote(modifier: Modifier = Modifier) {
    FormNotice(
        icon = Icons.Rounded.Info,
        text = "Assets are your account balances plus anything still owed to you. Liabilities " +
            "are what is left on your debts and EMIs, money you have borrowed, and any " +
            "outstanding card balance. Interest that has not been charged yet, and anything you " +
            "have not recorded, is not included.",
        modifier = modifier
    )
}

/** Skeleton in the shape of the real screen, so nothing jumps on first load. */
@Composable
private fun OverviewSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small)
    ) {
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 268.dp, cornerRadius = 30.dp)
        Spacer(Modifier.height(MoneBieTheme.spacing.extraSmall))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.4f), height = 18.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 104.dp, cornerRadius = 24.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 104.dp, cornerRadius = 24.dp)
        Spacer(Modifier.height(MoneBieTheme.spacing.extraSmall))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.4f), height = 18.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 104.dp, cornerRadius = 24.dp)
    }
}

private fun iconFor(key: PositionGroupKey): ImageVector = when (key) {
    PositionGroupKey.CASH -> Icons.Rounded.Payments
    PositionGroupKey.BANK -> Icons.Rounded.AccountBalance
    PositionGroupKey.WALLET -> Icons.Rounded.Wallet
    PositionGroupKey.OTHER_ASSETS -> Icons.Rounded.Savings
    PositionGroupKey.MONEY_LENT -> Icons.Rounded.Handshake
    PositionGroupKey.DEBTS -> Icons.AutoMirrored.Rounded.ReceiptLong
    PositionGroupKey.EMI -> Icons.Rounded.EventRepeat
    PositionGroupKey.MONEY_BORROWED -> Icons.Rounded.Handshake
    PositionGroupKey.CREDIT_CARDS -> Icons.Rounded.CreditCard
}
