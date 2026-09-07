package com.aurex.app.ui.screens.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.aurex.app.core.formatWeekday
import com.aurex.app.core.getCurrentDay
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.presentation.transaction.TransactionsUiState
import com.aurex.app.presentation.transaction.TransactionsViewModel
import com.aurex.app.ui.components.AmbientBackdrop
import com.aurex.app.ui.components.CardDivider
import com.aurex.app.ui.components.CountUpMoneyText
import com.aurex.app.ui.components.EmptyState
import com.aurex.app.ui.components.FilterPill
import com.aurex.app.ui.components.GlassIconButton
import com.aurex.app.ui.components.HeroCardShape
import com.aurex.app.ui.components.HeroDecoration
import com.aurex.app.ui.components.HeroFinance
import com.aurex.app.ui.components.HeroTile
import com.aurex.app.ui.components.HeroTrack
import com.aurex.app.ui.components.MicroLabel
import com.aurex.app.ui.components.MoneyText
import com.aurex.app.ui.components.OnHero
import com.aurex.app.ui.components.PremiumCard
import com.aurex.app.ui.components.ShimmerBlock
import com.aurex.app.ui.components.heroBrush
import com.aurex.app.ui.components.pressable
import com.aurex.app.ui.components.signedMoneyColor
import com.aurex.app.ui.screens.dashboard.TransactionRow
import com.aurex.app.ui.theme.AurexTheme
import com.aurex.app.ui.theme.Palette300
import com.aurex.app.ui.theme.Palette500
import com.aurex.app.ui.theme.Palette700
import com.aurex.app.ui.theme.Palette900

/**
 * The full transaction history, one month at a time.
 *
 * Month totals stay visible above the list and are computed from the whole
 * month, while the summary under an active filter reflects only what is shown -
 * so narrowing the list never makes it look like the month itself changed.
 *
 * The month you are standing in and the filters that shape the list are pinned;
 * everything that only summarises scrolls away, because on a screen whose job
 * is scanning history, the list should get the room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AurexApplication

    val viewModel: TransactionsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TransactionsViewModel(
                    getTransactionsInRangeUseCase = app.appContainer.getTransactionsInRangeUseCase,
                    getAccountsUseCase = app.appContainer.getAccountsUseCase,
                    getActiveCategoriesUseCase = app.appContainer.getActiveCategoriesUseCase,
                    getSelectedCurrencySymbolUseCase = app.appContainer.getSelectedCurrencySymbolUseCase,
                    deleteTransactionUseCase = app.appContainer.deleteTransactionUseCase
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showFilters by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            MonthBar(
                uiState = uiState,
                onPrevious = viewModel::showPreviousMonth,
                onNext = viewModel::showNextMonth
            )

            FilterBar(
                uiState = uiState,
                onTypeSelected = viewModel::setTypeFilter,
                onOpenFilters = { showFilters = true },
                onClearFilters = viewModel::clearFilters
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = AurexTheme.spacing.medium,
                    end = AurexTheme.spacing.medium,
                    top = AurexTheme.spacing.small,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
            ) {
                if (uiState.isLoading) {
                    item(key = "skeleton") { TransactionsSkeleton() }
                } else {
                    item(key = "hero") {
                        MonthSummaryHero(uiState = uiState)
                    }
                }

                if (uiState.isEmpty) {
                    item(key = "empty") {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            if (uiState.isEmptyBecauseOfFilter) {
                                EmptyState(
                                    icon = Icons.Rounded.FilterAltOff,
                                    title = "Nothing matches",
                                    message = "No transactions in ${uiState.monthLabel} match the filters you have set."
                                )
                            } else {
                                EmptyState(
                                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                                    title = "No transactions in ${uiState.monthLabel}",
                                    message = "Record an income or expense and it will appear here, grouped by day.",
                                    actionLabel = "Add a transaction",
                                    onAction = onAddTransaction
                                )
                            }
                        }
                    }
                }

                uiState.groups.forEach { group ->
                    item(key = "day_" + group.day) {
                        DayHeader(
                            day = group.day,
                            count = group.transactions.size,
                            net = group.summary.net,
                            currencySymbol = uiState.currencySymbol
                        )
                    }

                    item(key = "group_" + group.day) {
                        PremiumCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = AurexTheme.spacing.medium
                        ) {
                            group.transactions.forEachIndexed { index, transaction ->
                                if (index > 0) CardDivider(alpha = 0.07f)
                                TransactionRow(
                                    transaction = transaction,
                                    accountName = uiState.accountName(transaction.accountId),
                                    currencySymbol = uiState.currencySymbol,
                                    category = uiState.category(transaction.categoryId),
                                    // Only manual entries open the editor. A debt
                                    // payment's transaction is managed from the debt
                                    // it belongs to, so editing it here would leave
                                    // the payment history and the ledger disagreeing.
                                    modifier = if (transaction.source.isSystemGenerated) {
                                        Modifier
                                    } else {
                                        Modifier.pressable { onEditTransaction(transaction.id) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            FilterSheet(
                uiState = uiState,
                onAccountSelected = viewModel::setAccountFilter,
                onCategorySelected = viewModel::setCategoryFilter,
                onClear = viewModel::clearFilters
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Pinned: the month you are standing in
// -----------------------------------------------------------------------------

@Composable
private fun MonthBar(
    uiState: TransactionsUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleCount = uiState.groups.sumOf { it.transactions.size }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AurexTheme.spacing.medium,
                vertical = AurexTheme.spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = "Previous month",
            onClick = onPrevious
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = uiState.monthLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            MicroLabel(
                text = when {
                    uiState.isLoading -> "Loading"
                    visibleCount == 1 -> "1 transaction"
                    else -> "$visibleCount transactions"
                },
                color = MaterialTheme.colorScheme.primary
            )
        }

        GlassIconButton(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "Next month",
            onClick = onNext,
            enabled = uiState.canGoToNextMonth
        )
    }
}

// -----------------------------------------------------------------------------
// Pinned: filters
// -----------------------------------------------------------------------------

@Composable
private fun FilterBar(
    uiState: TransactionsUiState,
    onTypeSelected: (TransactionType?) -> Unit,
    onOpenFilters: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Filters set inside the sheet, which the bar cannot show individually.
    val extraFilters = uiState.filter.activeCount - (if (uiState.filter.type != null) 1 else 0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(
                horizontal = AurexTheme.spacing.medium,
                vertical = AurexTheme.spacing.small
            ),
        horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
    ) {
        FilterPill(
            label = "All",
            selected = uiState.filter.type == null,
            onClick = { onTypeSelected(null) }
        )
        FilterPill(
            label = "Income",
            selected = uiState.filter.type == TransactionType.INCOME,
            onClick = { onTypeSelected(TransactionType.INCOME) }
        )
        FilterPill(
            label = "Expense",
            selected = uiState.filter.type == TransactionType.EXPENSE,
            onClick = { onTypeSelected(TransactionType.EXPENSE) }
        )
        FilterPill(
            label = "Transfers",
            selected = uiState.filter.type == TransactionType.TRANSFER,
            onClick = { onTypeSelected(TransactionType.TRANSFER) }
        )
        FilterPill(
            label = if (extraFilters > 0) "Filters ($extraFilters)" else "Filters",
            selected = extraFilters > 0,
            onClick = onOpenFilters,
            icon = Icons.Rounded.Tune
        )

        if (uiState.filter.isActive) {
            FilterPill(
                label = "Clear",
                selected = false,
                onClick = onClearFilters,
                icon = Icons.Rounded.Close,
                tint = AurexTheme.finance.negative
            )
        }
    }
}

// -----------------------------------------------------------------------------
// The month summary
// -----------------------------------------------------------------------------

/**
 * What the month came to.
 *
 * Net leads because it is the one figure that answers "did this month work",
 * and the two sides that produced it sit under it. Under an active filter the
 * hero keeps showing the whole month and adds a line for the subset, so a
 * filter can never be mistaken for the month having changed.
 */
@Composable
private fun MonthSummaryHero(
    uiState: TransactionsUiState,
    modifier: Modifier = Modifier
) {
    val summary = uiState.monthSummary
    val positive = !summary.net.isNegative

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
                MicroLabel(text = "Net this month", color = OnHero.copy(alpha = 0.75f))
                Spacer(Modifier.weight(1f))
                // A month with nothing in it has no direction, and an up arrow
                // beside a zero reads as a claim that it went well.
                if (summary.hasActivity) {
                    Icon(
                        imageVector = if (positive) {
                            Icons.Rounded.ArrowUpward
                        } else {
                            Icons.Rounded.ArrowDownward
                        },
                        contentDescription = null,
                        tint = if (positive) HeroFinance.positive else HeroFinance.negative,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            CountUpMoneyText(
                money = summary.net,
                currencySymbol = uiState.currencySymbol,
                style = MaterialTheme.typography.displaySmall,
                color = if (positive) OnHero else HeroFinance.negative,
                signed = summary.hasActivity
            )

            Spacer(Modifier.height(16.dp))

            HeroRatioBar(progress = summary.expenseRatio)

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (summary.income.isZero) {
                    "No income recorded in ${uiState.monthLabel}"
                } else {
                    "${MoneyFormatter.percent(summary.expenseRatio)} of this month's income spent"
                },
                style = MaterialTheme.typography.bodySmall,
                color = OnHero.copy(alpha = 0.75f)
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile(
                    label = "Income",
                    value = MoneyFormatter.compact(summary.income, uiState.currencySymbol),
                    valueColor = HeroFinance.positive,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Expenses",
                    value = MoneyFormatter.compact(summary.expense, uiState.currencySymbol),
                    valueColor = HeroFinance.negative,
                    modifier = Modifier.weight(1f)
                )
            }

            if (uiState.filter.isActive) {
                Spacer(Modifier.height(14.dp))
                FilteredNote(
                    net = uiState.visibleSummary.net,
                    currencySymbol = uiState.currencySymbol
                )
            }
        }
    }
}

/** Expense as a share of income, on the hero's own light-on-dark palette. */
@Composable
private fun HeroRatioBar(progress: Float, modifier: Modifier = Modifier) {
    HeroTrack(
        progress = progress,
        // A month that has spent nearly everything it earned says so in the
        // bar, before the figures are read.
        colors = if (progress > 0.85f) {
            listOf(HeroFinance.caution, HeroFinance.negative)
        } else {
            listOf(Palette300, Palette500)
        },
        modifier = modifier
    )
}

/** Says what the filtered subset comes to, without touching the month's own totals. */
@Composable
private fun FilteredNote(
    net: Money,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Tune,
            contentDescription = null,
            tint = OnHero.copy(alpha = 0.75f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Filtered results come to " +
                MoneyFormatter.format(net, currencySymbol, showDecimals = false, signed = true),
            style = MaterialTheme.typography.bodySmall,
            color = OnHero.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// -----------------------------------------------------------------------------
// Day groups
// -----------------------------------------------------------------------------

@Composable
private fun DayHeader(
    day: String,
    count: Int,
    net: Money,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val isToday = day == getCurrentDay()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AurexTheme.spacing.small, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The same brand tick the section headers elsewhere use, so a day break
        // here reads as the same kind of division.
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 16.dp)
                .clip(CircleShape)
                .background(
                    if (isToday) {
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        )
                    }
                )
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = if (isToday) "Today" else "${formatWeekday(day)} · ${formatDisplayDate(day)}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = if (count == 1) "1 entry" else "$count entries",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1
        )

        Spacer(Modifier.weight(1f))

        MoneyText(
            money = net,
            currencySymbol = currencySymbol,
            style = MaterialTheme.typography.labelLarge,
            color = signedMoneyColor(net),
            signed = true,
            showDecimals = false
        )
    }
}

/** Skeleton in the shape of the real screen, so nothing jumps on first load. */
@Composable
private fun TransactionsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
    ) {
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 236.dp, cornerRadius = 30.dp)
        Spacer(Modifier.height(AurexTheme.spacing.extraSmall))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.45f), height = 18.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 148.dp, cornerRadius = 24.dp)
        Spacer(Modifier.height(AurexTheme.spacing.extraSmall))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.35f), height = 18.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 96.dp, cornerRadius = 24.dp)
    }
}

// -----------------------------------------------------------------------------
// Filter sheet
// -----------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    uiState: TransactionsUiState,
    onAccountSelected: (Long?) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AurexTheme.spacing.medium)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                MicroLabel(
                    text = if (uiState.filter.isActive) {
                        "${uiState.filter.activeCount} active"
                    } else {
                        "Showing everything"
                    }
                )
            }

            if (uiState.filter.isActive) {
                Text(
                    text = "Clear all",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(AurexTheme.spacing.large))

        FilterGroup(title = "Account") {
            FilterPill(
                label = "Any",
                selected = uiState.filter.accountId == null,
                onClick = { onAccountSelected(null) }
            )
            uiState.accounts.forEach { account ->
                FilterPill(
                    label = account.name,
                    selected = uiState.filter.accountId == account.id,
                    onClick = { onAccountSelected(account.id) }
                )
            }
        }

        Spacer(Modifier.height(AurexTheme.spacing.large))

        FilterGroup(title = "Category") {
            FilterPill(
                label = "Any",
                selected = uiState.filter.categoryId == null,
                onClick = { onCategorySelected(null) }
            )
            // Both directions are offered because the category filter is
            // independent of whichever type filter is active.
            (
                uiState.categoriesFor(TransactionType.INCOME) +
                    uiState.categoriesFor(TransactionType.EXPENSE)
                ).forEach { category ->
                FilterPill(
                    label = category.name,
                    selected = uiState.filter.categoryId == category.id,
                    onClick = { onCategorySelected(category.id) }
                )
            }
        }

        Spacer(Modifier.height(AurexTheme.spacing.extraExtraLarge))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(title: String, content: @Composable () -> Unit) {
    MicroLabel(text = title)
    Spacer(Modifier.height(AurexTheme.spacing.small))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
    ) {
        content()
    }
}

