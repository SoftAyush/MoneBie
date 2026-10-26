package com.aurex.app.ui.screens.insights

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurex.app.AurexApplication
import com.aurex.app.core.Money
import com.aurex.app.core.MoneyFormatter
import com.aurex.app.core.formatDisplayDate
import com.aurex.app.domain.model.CategorySpend
import com.aurex.app.domain.model.PeriodChange
import com.aurex.app.domain.model.SpendingInsights
import com.aurex.app.presentation.insights.InsightsViewModel
import com.aurex.app.ui.components.AmbientBackdrop
import com.aurex.app.ui.components.AurexIcons
import com.aurex.app.ui.components.CardDivider
import com.aurex.app.ui.components.CountUpMoneyText
import com.aurex.app.ui.components.DetailTopBar
import com.aurex.app.ui.components.EmptyState
import com.aurex.app.ui.components.GlassIconButton
import com.aurex.app.ui.components.GlowBadge
import com.aurex.app.ui.components.HeroCardShape
import com.aurex.app.ui.components.HeroDecoration
import com.aurex.app.ui.components.HeroFinance
import com.aurex.app.ui.components.HeroNote
import com.aurex.app.ui.components.HeroTile
import com.aurex.app.ui.components.MicroLabel
import com.aurex.app.ui.components.MoneyText
import com.aurex.app.ui.components.OnHero
import com.aurex.app.ui.components.PremiumCard
import com.aurex.app.ui.components.PremiumSectionHeader
import com.aurex.app.ui.components.ShimmerBlock
import com.aurex.app.ui.components.heroBrush
import com.aurex.app.ui.theme.AurexTheme
import com.aurex.app.ui.theme.Palette700
import com.aurex.app.ui.theme.Palette900

/**
 * Where the money went, one month at a time.
 *
 * Every figure is comparative: a spend total on its own says nothing, but the
 * same total against last month, against the daily rate, and split by category
 * is something the user can act on.
 *
 * The categories are ranked bars in one colour rather than a pie of eight
 * hues - the question they answer is "which is biggest and by how much", and
 * length answers that where angle and hue do not.
 */
@Composable
fun InsightsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AurexApplication

    val viewModel: InsightsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                InsightsViewModel(
                    getSpendingInsightsUseCase = app.appContainer.getSpendingInsightsUseCase
                )
            }
        }
    )

    val insights by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                title = "Insights",
                onBack = onBack,
                subtitle = insights.monthLabel
            ) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    onClick = viewModel::showPreviousMonth
                )
                GlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Next month",
                    onClick = viewModel::showNextMonth,
                    enabled = insights.canGoToNextMonth
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = AurexTheme.spacing.medium,
                    end = AurexTheme.spacing.medium,
                    top = AurexTheme.spacing.small,
                    bottom = AurexTheme.spacing.extraExtraLarge
                ),
                verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
            ) {
                if (insights.isLoading) {
                    item(key = "skeleton") { InsightsSkeleton() }
                    return@LazyColumn
                }

                if (!insights.hasActivity) {
                    item(key = "empty") {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                icon = Icons.Rounded.QueryStats,
                                title = "Nothing recorded in ${insights.monthLabel}",
                                message = "Once there are transactions in this month, you will " +
                                    "see where the money went and how it compares with the month " +
                                    "before."
                            )
                        }
                    }
                    return@LazyColumn
                }

                item(key = "hero") { SpendHero(insights) }

                item(key = "comparison_header") {
                    PremiumSectionHeader(title = "Against last month")
                }

                item(key = "comparison") { ComparisonCard(insights) }

                item(key = "rhythm_header") {
                    PremiumSectionHeader(title = "The month's rhythm")
                }

                item(key = "rhythm") { RhythmCard(insights) }

                item(key = "expenses_header") {
                    PremiumSectionHeader(title = "Where it went")
                }

                item(key = "expenses") {
                    if (insights.expenseCategories.isEmpty()) {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No spending recorded this month.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        CategoryBreakdown(
                            categories = insights.expenseCategories,
                            currencySymbol = insights.currencySymbol,
                            accent = AurexTheme.finance.negative
                        )
                    }
                }

                if (insights.incomeCategories.isNotEmpty()) {
                    item(key = "income_header") {
                        PremiumSectionHeader(title = "Where it came from")
                    }

                    item(key = "income") {
                        CategoryBreakdown(
                            categories = insights.incomeCategories,
                            currencySymbol = insights.currencySymbol,
                            accent = AurexTheme.finance.positive
                        )
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
 * What the month cost.
 *
 * Spending leads rather than net, because this is the screen a user opens to
 * ask where the money went; the three tiles carry the figures that put that
 * number in proportion.
 */
@Composable
private fun SpendHero(
    insights: SpendingInsights,
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
            MicroLabel(
                text = "Spent in ${insights.monthLabel}",
                color = OnHero.copy(alpha = 0.75f)
            )

            Spacer(Modifier.height(8.dp))

            CountUpMoneyText(
                money = insights.summary.expense,
                currencySymbol = insights.currencySymbol,
                style = MaterialTheme.typography.displaySmall,
                color = OnHero
            )

            Spacer(Modifier.height(14.dp))

            ChangeNote(
                change = insights.expenseChange,
                currencySymbol = insights.currencySymbol,
                // For spending, up is the bad direction. Stated explicitly
                // rather than inferred from the sign, which carries no meaning
                // on its own.
                increaseIsGood = false
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile(
                    label = "Earned",
                    value = MoneyFormatter.compact(
                        insights.summary.income,
                        insights.currencySymbol
                    ),
                    valueColor = HeroFinance.positive,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Net",
                    value = MoneyFormatter.compact(insights.summary.net, insights.currencySymbol),
                    valueColor = if (insights.isOverspending) {
                        HeroFinance.negative
                    } else {
                        OnHero
                    },
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Kept",
                    value = MoneyFormatter.percent(insights.savingsRate),
                    valueColor = OnHero,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * How a figure moved against last month.
 *
 * Colour follows meaning, not sign: spending less is good, earning less is not.
 */
@Composable
private fun ChangeNote(
    change: PeriodChange,
    currencySymbol: String,
    increaseIsGood: Boolean,
    modifier: Modifier = Modifier
) {
    if (!change.hasComparison) {
        HeroNote(
            icon = Icons.Rounded.DragHandle,
            text = "No spending recorded last month to compare with",
            modifier = modifier
        )
        return
    }

    if (change.unchanged) {
        HeroNote(
            icon = Icons.Rounded.DragHandle,
            text = "Exactly the same as last month",
            modifier = modifier
        )
        return
    }

    val isGood = change.increased == increaseIsGood

    HeroNote(
        icon = if (change.increased) {
            Icons.AutoMirrored.Rounded.TrendingUp
        } else {
            Icons.AutoMirrored.Rounded.TrendingDown
        },
        text = MoneyFormatter.format(
            change.difference.absolute,
            currencySymbol,
            showDecimals = false
        ) + (if (change.increased) " more" else " less") + " than last month",
        accent = if (isGood) HeroFinance.positive else HeroFinance.negative,
        modifier = modifier
    )
}

// -----------------------------------------------------------------------------
// Against last month
// -----------------------------------------------------------------------------

/**
 * Two months side by side, as two bars each.
 *
 * Spending and earning get their own pair rather than sharing one scale: they
 * are different measures, and one axis holding both would make the smaller of
 * them unreadable to save a card.
 */
@Composable
private fun ComparisonCard(
    insights: SpendingInsights,
    modifier: Modifier = Modifier
) {
    PremiumCard(modifier = modifier.fillMaxWidth()) {
        ComparisonPair(
            title = "Spending",
            change = insights.expenseChange,
            currencySymbol = insights.currencySymbol,
            accent = AurexTheme.finance.negative
        )

        Spacer(Modifier.height(AurexTheme.spacing.medium))
        CardDivider()
        Spacer(Modifier.height(AurexTheme.spacing.medium))

        ComparisonPair(
            title = "Earning",
            change = insights.incomeChange,
            currencySymbol = insights.currencySymbol,
            accent = AurexTheme.finance.positive
        )
    }
}

@Composable
private fun ComparisonPair(
    title: String,
    change: PeriodChange,
    currencySymbol: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    // Both bars share one scale - the larger of the two months - so their
    // lengths can be compared directly rather than each filling its own row.
    val peak = maxOf(change.current.minorUnits, change.previous.minorUnits)

    Column(modifier = modifier.fillMaxWidth()) {
        MicroLabel(text = title)

        Spacer(Modifier.height(AurexTheme.spacing.small))

        ComparisonBar(
            label = "Last month",
            money = change.previous,
            fraction = share(change.previous, peak),
            currencySymbol = currencySymbol,
            // The month being read is the one at full strength; the one behind
            // it is context, and recedes.
            color = accent.copy(alpha = 0.35f)
        )

        Spacer(Modifier.height(AurexTheme.spacing.small))

        ComparisonBar(
            label = "This month",
            money = change.current,
            fraction = share(change.current, peak),
            currencySymbol = currencySymbol,
            color = accent
        )
    }
}

private fun share(money: Money, peak: Long): Float =
    if (peak <= 0L) 0f else (money.minorUnits.toDouble() / peak).toFloat().coerceIn(0f, 1f)

@Composable
private fun ComparisonBar(
    label: String,
    money: Money,
    fraction: Float,
    currencySymbol: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val width by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "comparison_bar"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            MoneyText(
                money = money,
                currencySymbol = currencySymbol,
                style = MaterialTheme.typography.labelLarge,
                // The figure wears text ink, not the bar's colour: the bar
                // beside it already carries which series this is.
                color = MaterialTheme.colorScheme.onSurface,
                showDecimals = false
            )
        }

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(width)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Rhythm
// -----------------------------------------------------------------------------

@Composable
private fun RhythmCard(
    insights: SpendingInsights,
    modifier: Modifier = Modifier
) {
    PremiumCard(modifier = modifier.fillMaxWidth()) {
        RhythmRow(
            label = "Average per day so far",
            value = MoneyFormatter.format(
                insights.dailyAverageSpend,
                insights.currencySymbol,
                showDecimals = false
            )
        )

        insights.heaviestDay?.let { day ->
            CardDivider(alpha = 0.07f)
            RhythmRow(
                label = "Heaviest day · " + formatDisplayDate(day.day),
                value = MoneyFormatter.format(
                    day.expense,
                    insights.currencySymbol,
                    showDecimals = false
                )
            )
        }

        CardDivider(alpha = 0.07f)

        RhythmRow(
            label = "Transactions recorded",
            value = insights.transactionCount.toString()
        )

        insights.topExpenseCategory?.let { top ->
            Spacer(Modifier.height(AurexTheme.spacing.medium))
            Text(
                text = "Most of your spending went on " + top.label + " - " +
                    MoneyFormatter.percent(top.share) + " of the month.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RhythmRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AurexTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(AurexTheme.spacing.small))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

// -----------------------------------------------------------------------------
// Categories
// -----------------------------------------------------------------------------

/**
 * The month split by category, ranked.
 *
 * One card rather than one card per row: this is a single ranked chart, and a
 * stack of separate cards would break the bars apart into unrelated facts. Every
 * bar is measured against the largest category, not against the month total, so
 * the shorter ones stay legible instead of collapsing into slivers - the share
 * of the month is stated in words on each row.
 */
@Composable
private fun CategoryBreakdown(
    categories: List<CategorySpend>,
    currencySymbol: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val peak = categories.maxOfOrNull { it.amount.minorUnits } ?: 0L

    PremiumCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = AurexTheme.spacing.medium
    ) {
        categories.forEachIndexed { index, category ->
            if (index > 0) CardDivider(alpha = 0.07f)
            CategoryRow(
                category = category,
                currencySymbol = currencySymbol,
                accent = accent,
                fraction = share(category.amount, peak)
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: CategorySpend,
    currencySymbol: String,
    accent: Color,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val width by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "category_bar"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AurexTheme.spacing.small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)
        ) {
            GlowBadge(
                icon = AurexIcons.forCategory(category.iconKey),
                tint = accent,
                size = 38.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = MoneyFormatter.percent(category.share) + " of the month · " +
                        category.transactionCount +
                        if (category.transactionCount == 1) " entry" else " entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            MoneyText(
                money = category.amount,
                currencySymbol = currencySymbol,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                showDecimals = false
            )
        }

        Spacer(Modifier.height(AurexTheme.spacing.small))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(width)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
    }
}

/** Skeleton in the shape of the real screen, so nothing jumps on first load. */
@Composable
private fun InsightsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
    ) {
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 236.dp, cornerRadius = 30.dp)
        Spacer(Modifier.height(AurexTheme.spacing.extraSmall))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.45f), height = 18.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 168.dp, cornerRadius = 24.dp)
        Spacer(Modifier.height(AurexTheme.spacing.extraSmall))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.35f), height = 18.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 132.dp, cornerRadius = 24.dp)
    }
}
