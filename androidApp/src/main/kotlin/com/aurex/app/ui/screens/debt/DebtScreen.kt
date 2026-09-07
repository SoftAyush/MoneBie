package com.aurex.app.ui.screens.debt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.aurex.app.AurexApplication
import com.aurex.app.core.MoneyFormatter
import com.aurex.app.domain.model.DebtStatus
import com.aurex.app.domain.model.displayName
import com.aurex.app.presentation.debt.DebtListItem
import com.aurex.app.presentation.debt.DebtListUiState
import com.aurex.app.presentation.debt.DebtListViewModel
import com.aurex.app.ui.components.AmbientBackdrop
import com.aurex.app.ui.components.CountUpMoneyText
import com.aurex.app.ui.components.EmptyState
import com.aurex.app.ui.components.FilterPill
import com.aurex.app.ui.components.HeroCardShape
import com.aurex.app.ui.components.HeroDecoration
import com.aurex.app.ui.components.HeroNote
import com.aurex.app.ui.components.HeroFinance
import com.aurex.app.ui.components.HeroTile
import com.aurex.app.ui.components.HeroTrack
import com.aurex.app.ui.components.MicroLabel
import com.aurex.app.ui.components.OnHero
import com.aurex.app.ui.components.PremiumCard
import com.aurex.app.ui.components.ShimmerBlock
import com.aurex.app.ui.components.heroBrush
import com.aurex.app.ui.theme.AurexTheme
import com.aurex.app.ui.theme.Palette300
import com.aurex.app.ui.theme.Palette500
import com.aurex.app.ui.theme.Palette700
import com.aurex.app.ui.theme.Palette900

/**
 * Every debt the user owes, most urgent first.
 *
 * The hero always describes every debt. Filtering narrows the list but never
 * the headline: what you owe does not change because you looked at a subset of
 * it, so an active filter adds a line for the subset rather than rewriting the
 * total.
 *
 * The title and the status filters are pinned, because this is a screen you
 * open to find the one debt that needs paying; everything that only summarises
 * scrolls away and gives the list the room.
 */
@Composable
fun DebtScreen(
    onAddDebt: () -> Unit,
    onOpenDebt: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AurexApplication

    val viewModel: DebtListViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                DebtListViewModel(
                    getDebtsUseCase = app.appContainer.getDebtsUseCase,
                    getSelectedCurrencySymbolUseCase = app.appContainer.getSelectedCurrencySymbolUseCase
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DebtBar(uiState = uiState)

            StatusFilterBar(
                uiState = uiState,
                onStatusSelected = viewModel::setStatusFilter
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
                    item(key = "skeleton") { DebtSkeleton() }
                } else {
                    item(key = "hero") { DebtHero(uiState = uiState) }
                }

                if (uiState.isEmpty) {
                    item(key = "empty") {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            if (uiState.isEmptyBecauseOfFilter) {
                                EmptyState(
                                    icon = Icons.Rounded.FilterAltOff,
                                    title = "Nothing here",
                                    message = "No debts match this status right now."
                                )
                            } else {
                                EmptyState(
                                    icon = Icons.Rounded.AccountBalanceWallet,
                                    title = "No debts tracked",
                                    message = "Add a loan, a bill or anything you owe, and record " +
                                        "payments against it as you pay it down.",
                                    actionLabel = "Add your first debt",
                                    onAction = onAddDebt
                                )
                            }
                        }
                    }
                }

                items(uiState.items, key = { it.debt.id }) { item ->
                    DebtCard(
                        item = item,
                        currencySymbol = uiState.currencySymbol,
                        onClick = { onOpenDebt(item.debt.id) }
                    )
                }
            }
        }

        // The shell hides its own button on this tab, so adding a debt is this
        // screen's to offer.
        FloatingActionButton(
            onClick = onAddDebt,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AurexTheme.spacing.medium)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add debt")
        }
    }
}

// -----------------------------------------------------------------------------
// Pinned: title and filters
// -----------------------------------------------------------------------------

@Composable
private fun DebtBar(
    uiState: DebtListUiState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AurexTheme.spacing.medium,
                vertical = AurexTheme.spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Debt",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            // Overdue takes the line whenever there is any: it is the only
            // thing on this screen that asks to be acted on today.
            MicroLabel(
                text = when {
                    uiState.isLoading -> "Loading"
                    uiState.overdueCount == 1 -> "1 overdue"
                    uiState.overdueCount > 1 -> "${uiState.overdueCount} overdue"
                    uiState.activeCount == 1 -> "1 open debt"
                    uiState.activeCount > 1 -> "${uiState.activeCount} open debts"
                    else -> "All settled"
                },
                color = if (uiState.overdueCount > 0) {
                    AurexTheme.finance.negative
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

@Composable
private fun StatusFilterBar(
    uiState: DebtListUiState,
    onStatusSelected: (DebtStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
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
            selected = uiState.statusFilter == null,
            onClick = { onStatusSelected(null) },
            count = uiState.totalCount.takeIf { !uiState.isLoading }
        )
        DebtStatus.entries.forEach { status ->
            // An unselected Overdue pill wears the warning colour while
            // anything is overdue, so the way out of the problem is visible
            // from the same glance that shows the problem.
            val urgent = status == DebtStatus.OVERDUE && uiState.overdueCount > 0
            val count = uiState.countFor(status)
            FilterPill(
                label = status.displayName,
                selected = uiState.statusFilter == status,
                onClick = { onStatusSelected(status) },
                tint = if (urgent) AurexTheme.finance.negative else null,
                icon = if (urgent) Icons.Rounded.PriorityHigh else null,
                count = count.takeIf { !uiState.isLoading },
                // A filter already showing is never greyed out, however empty
                // it turned out to be - the way back is through the pill.
                enabled = uiState.isLoading || count > 0 || uiState.statusFilter == status
            )
        }
    }
}

// -----------------------------------------------------------------------------
// The hero
// -----------------------------------------------------------------------------

/**
 * What is still owed, across everything.
 *
 * The remaining balance leads because it is the figure that moves as payments
 * are recorded, and the repayment bar under it is the progress the module
 * exists to show. The two sides that produced it - repaid and borrowed - sit
 * below as supporting tiles.
 */
@Composable
private fun DebtHero(
    uiState: DebtListUiState,
    modifier: Modifier = Modifier
) {
    val settled = uiState.totalRemaining.isZero

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
                MicroLabel(text = "Still owed", color = OnHero.copy(alpha = 0.75f))
                Spacer(Modifier.weight(1f))
                when {
                    uiState.overdueCount > 0 -> HeroStatusPill(
                        icon = Icons.Rounded.PriorityHigh,
                        label = if (uiState.overdueCount == 1) {
                            "1 overdue"
                        } else {
                            "${uiState.overdueCount} overdue"
                        },
                        accent = HeroFinance.negative
                    )
                    settled && !uiState.totalPaid.isZero -> HeroStatusPill(
                        icon = Icons.Rounded.TaskAlt,
                        label = "All clear",
                        accent = HeroFinance.positive
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            CountUpMoneyText(
                money = uiState.totalRemaining,
                currencySymbol = uiState.currencySymbol,
                style = MaterialTheme.typography.displaySmall,
                color = OnHero
            )

            Spacer(Modifier.height(16.dp))

            HeroRepaymentBar(progress = uiState.overallProgress)

            Spacer(Modifier.height(12.dp))

            /*
             * What to do next, not how far you have come.
             *
             * The bar above already carries the progress, and every card below
             * repeats its own share of it. The figure this screen could not
             * previously answer at a glance is the one that has a deadline.
             */
            val next = uiState.nextPayment
            if (next != null) {
                NextPaymentNote(item = next, currencySymbol = uiState.currencySymbol)
            } else {
                Text(
                    text = when {
                        uiState.totalPaid.isZero && settled -> "Nothing owed right now"
                        settled -> "Every debt you have tracked is repaid"
                        else -> "No payment dates set on what is left"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = OnHero.copy(alpha = 0.75f)
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile(
                    label = "Repaid",
                    value = MoneyFormatter.compact(uiState.totalPaid, uiState.currencySymbol),
                    valueColor = HeroFinance.positive,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Borrowed",
                    value = MoneyFormatter.compact(uiState.totalBorrowed, uiState.currencySymbol),
                    valueColor = OnHero,
                    modifier = Modifier.weight(1f)
                )
            }

            val filter = uiState.statusFilter
            if (filter != null) {
                Spacer(Modifier.height(14.dp))
                FilteredNote(status = filter, count = uiState.items.size)
            }
        }
    }
}

/**
 * The next payment that falls due, on the hero.
 *
 * Names the debt as well as the date: with more than one being tracked, "in 6
 * days" without a name sends the user back into the list to find out which.
 * The installment is included when the debt carries one, because the amount is
 * half of what the user needs in order to act on the date.
 */
@Composable
private fun NextPaymentNote(
    item: DebtListItem,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val days = item.daysUntilDue ?: return
    val overdue = item.status == DebtStatus.OVERDUE || days < 0L

    // A debt need not carry an installment - a one-off loan has a date and no
    // schedule - so the amount drops out of the sentence rather than reading
    // as zero due.
    val installment = item.debt.installmentAmount?.takeIf { it.isPositive }
    val amount = installment
        ?.let { MoneyFormatter.format(it, currencySymbol, showDecimals = false) + " " }
        .orEmpty()

    val when_ = when {
        days < 0L -> "overdue by ${-days}d"
        days == 0L -> "due today"
        days == 1L -> "due tomorrow"
        days <= 30L -> "due in ${days}d"
        else -> "due in ${days / 30L}mo"
    }

    HeroNote(
        icon = if (overdue) Icons.Rounded.PriorityHigh else Icons.Rounded.EventAvailable,
        text = item.debt.name + " · " + amount + when_,
        accent = if (overdue) HeroFinance.negative else null,
        modifier = modifier
    )
}

/** A short state marker on the hero: overdue, or nothing left to pay. */
@Composable
private fun HeroStatusPill(
    icon: ImageVector,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.35f), CircleShape)
            .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1
        )
    }
}

/**
 * How much of everything borrowed is repaid.
 *
 * Clearing the last of it turns the bar green: the bar is the only place the
 * finish line is visible, and it should look like one.
 */
@Composable
private fun HeroRepaymentBar(progress: Float, modifier: Modifier = Modifier) {
    HeroTrack(
        progress = progress,
        colors = if (progress >= 1f) {
            listOf(HeroFinance.positive.copy(alpha = 0.75f), HeroFinance.positive)
        } else {
            listOf(Palette300, Palette500)
        },
        modifier = modifier
    )
}

/** Says what the filter is showing, without touching the totals above it. */
@Composable
private fun FilteredNote(
    status: DebtStatus,
    count: Int,
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
            imageVector = Icons.Rounded.FilterAltOff,
            contentDescription = null,
            tint = OnHero.copy(alpha = 0.75f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Showing " + when (count) {
                1 -> "1 " + status.displayName.lowercase() + " debt"
                else -> "$count " + status.displayName.lowercase() + " debts"
            },
            style = MaterialTheme.typography.bodySmall,
            color = OnHero.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Skeleton in the shape of the real screen, so nothing jumps on first load. */
@Composable
private fun DebtSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
    ) {
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 236.dp, cornerRadius = 30.dp)
        Spacer(Modifier.height(AurexTheme.spacing.extraSmall))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 148.dp, cornerRadius = 24.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 148.dp, cornerRadius = 24.dp)
    }
}
