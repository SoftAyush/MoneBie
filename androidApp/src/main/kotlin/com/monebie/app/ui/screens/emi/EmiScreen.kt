package com.aurex.app.ui.screens.emi

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventRepeat
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurex.app.AurexApplication
import com.aurex.app.core.MoneyFormatter
import com.aurex.app.domain.model.EmiStatus
import com.aurex.app.domain.model.displayName
import com.aurex.app.presentation.emi.EmiListItem
import com.aurex.app.presentation.emi.EmiListUiState
import com.aurex.app.presentation.emi.EmiListViewModel
import com.aurex.app.ui.components.AmbientBackdrop
import com.aurex.app.ui.components.CountUpMoneyText
import com.aurex.app.ui.components.EmptyState
import com.aurex.app.ui.components.FilterPill
import com.aurex.app.ui.components.HeroCardShape
import com.aurex.app.ui.components.HeroDecoration
import com.aurex.app.ui.components.HeroFinance
import com.aurex.app.ui.components.HeroNote
import com.aurex.app.ui.components.HeroTile
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
 * Every EMI, most urgent first.
 *
 * The hero leads with what is still payable and pairs it with the ring - an EMI
 * is a countdown of installments, and the ring is the only thing on the page
 * that shows how far through that countdown you are. What falls due this month
 * sits beside it as a tile: it is worth knowing, but it is zero in any month
 * nothing happens to be scheduled, and a zero is a poor headline.
 *
 * The hero always describes every EMI: filtering narrows the list but never the
 * headline, so an active filter adds a line for the subset instead of rewriting
 * the totals.
 */
@Composable
fun EmiScreen(
    onAddEmi: () -> Unit,
    onOpenEmi: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AurexApplication

    val viewModel: EmiListViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                EmiListViewModel(
                    getEmisUseCase = app.appContainer.getEmisUseCase,
                    getSelectedCurrencySymbolUseCase = app.appContainer.getSelectedCurrencySymbolUseCase
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            EmiBar(uiState = uiState)

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
                    item(key = "skeleton") { EmiSkeleton() }
                } else {
                    item(key = "hero") { EmiHero(uiState = uiState) }
                }

                if (uiState.isEmpty) {
                    item(key = "empty") {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            if (uiState.isEmptyBecauseOfFilter) {
                                EmptyState(
                                    icon = Icons.Rounded.FilterAltOff,
                                    title = "Nothing here",
                                    message = "No EMIs match this status right now."
                                )
                            } else {
                                EmptyState(
                                    icon = Icons.Rounded.EventRepeat,
                                    title = "No EMIs tracked",
                                    message = "Add a loan or instalment plan and mark each month " +
                                        "paid as it goes out.",
                                    actionLabel = "Add your first EMI",
                                    onAction = onAddEmi
                                )
                            }
                        }
                    }
                }

                items(uiState.items, key = { it.emi.id }) { item ->
                    EmiCard(
                        item = item,
                        currencySymbol = uiState.currencySymbol,
                        onClick = { onOpenEmi(item.emi.id) }
                    )
                }
            }
        }

        // The shell hides its own button on this tab, so adding an EMI is this
        // screen's to offer.
        FloatingActionButton(
            onClick = onAddEmi,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AurexTheme.spacing.medium)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add EMI")
        }
    }
}

// -----------------------------------------------------------------------------
// Pinned: title and filters
// -----------------------------------------------------------------------------

@Composable
private fun EmiBar(
    uiState: EmiListUiState,
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
                text = "EMI",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            // Overdue takes the line whenever there is any: a missed
            // installment is the only thing here that asks to be acted on today.
            MicroLabel(
                text = when {
                    uiState.isLoading -> "Loading"
                    uiState.overdueCount == 1 -> "1 overdue"
                    uiState.overdueCount > 1 -> "${uiState.overdueCount} overdue"
                    uiState.activeCount == 1 -> "1 plan running"
                    uiState.activeCount > 1 -> "${uiState.activeCount} plans running"
                    else -> "All cleared"
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
    uiState: EmiListUiState,
    onStatusSelected: (EmiStatus?) -> Unit,
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
        EmiStatus.entries.forEach { status ->
            // An unselected Overdue pill wears the warning colour while
            // anything is overdue, so the way to the problem is visible from
            // the same glance that shows it exists.
            val urgent = status == EmiStatus.OVERDUE && uiState.overdueCount > 0
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
 * What this month costs, and how far through the schedule you are.
 *
 * The month's figure and the ring sit side by side because they answer the two
 * halves of the same question - what leaves this month, and how much of the
 * commitment is behind you.
 */
@Composable
private fun EmiHero(
    uiState: EmiListUiState,
    modifier: Modifier = Modifier
) {
    val finished = uiState.remainingInstallments == 0

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
                Column(modifier = Modifier.weight(1f)) {
                    /*
                     * The headline is the commitment, not the calendar month.
                     *
                     * "Due this month" led here, which meant that in any month
                     * with nothing scheduled - the common case on a plan billed
                     * on the 20th, or one two months out - the largest figure on
                     * the screen was a zero, while the 216,500 still owed sat in
                     * a supporting tile. The month keeps its place as a tile
                     * below; what is still payable takes the line.
                     */
                    MicroLabel(text = "Still payable", color = OnHero.copy(alpha = 0.75f))

                    Spacer(Modifier.height(8.dp))

                    CountUpMoneyText(
                        money = uiState.totalRemaining,
                        currencySymbol = uiState.currencySymbol,
                        style = MaterialTheme.typography.headlineLarge,
                        color = OnHero,
                        showDecimals = false
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = when {
                            finished -> "Every installment is paid"
                            uiState.remainingInstallments == 1 -> "1 installment still to run"
                            else -> "${uiState.remainingInstallments} installments still to run"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = OnHero.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(14.dp))

                ScheduleRing(
                    progress = uiState.overallProgress,
                    complete = finished,
                    paid = uiState.paidInstallments,
                    total = uiState.totalInstallments
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile(
                    label = "Due this month",
                    value = MoneyFormatter.compact(uiState.dueThisMonth, uiState.currencySymbol),
                    valueColor = OnHero,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Paid so far",
                    value = MoneyFormatter.compact(uiState.totalPaid, uiState.currencySymbol),
                    valueColor = HeroFinance.positive,
                    modifier = Modifier.weight(1f)
                )
            }

            val next = uiState.nextInstallment
            if (uiState.overdueCount == 0 && next != null) {
                Spacer(Modifier.height(14.dp))
                NextInstallmentNote(item = next, currencySymbol = uiState.currencySymbol)
            }

            when {
                uiState.overdueCount > 0 -> {
                    Spacer(Modifier.height(14.dp))
                    HeroNote(
                        icon = Icons.Rounded.PriorityHigh,
                        text = if (uiState.overdueCount == 1) {
                            "1 installment is past its due date"
                        } else {
                            "${uiState.overdueCount} installments are past their due date"
                        },
                        accent = HeroFinance.negative
                    )
                }
                finished && !uiState.totalPaid.isZero -> {
                    Spacer(Modifier.height(14.dp))
                    HeroNote(
                        icon = Icons.Rounded.TaskAlt,
                        text = "Every plan you track has run its course",
                        accent = HeroFinance.positive
                    )
                }
            }

            val filter = uiState.statusFilter
            if (filter != null) {
                Spacer(Modifier.height(10.dp))
                HeroNote(
                    icon = Icons.Rounded.FilterAltOff,
                    text = "Showing " + when (uiState.items.size) {
                        1 -> "1 " + filter.displayName.lowercase() + " plan"
                        else -> "${uiState.items.size} " + filter.displayName.lowercase() + " plans"
                    },
                    accent = null
                )
            }
        }
    }
}

/**
 * When the next installment falls due, and what it costs.
 *
 * Names the plan as well as the date: with more than one running, a date on its
 * own sends the user back into the list to find out whose it is.
 */
@Composable
private fun NextInstallmentNote(
    item: EmiListItem,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val days = item.daysUntilDue ?: return

    val when_ = when {
        days < 0L -> "overdue by ${-days}d"
        days == 0L -> "due today"
        days == 1L -> "due tomorrow"
        days <= 30L -> "due in ${days}d"
        else -> "due in ${days / 30L}mo"
    }

    HeroNote(
        icon = Icons.Rounded.EventAvailable,
        text = item.emi.name + " · " +
            // The next installment specifically, not the monthly figure: a
            // final installment collects whatever remainder is left, and this
            // note is about the one payment that is actually coming.
            MoneyFormatter.format(
                item.emi.nextInstallmentAmount,
                currencySymbol,
                showDecimals = false
            ) + " " + when_,
        modifier = modifier
    )
}

/**
 * Installments paid, as a ring.
 *
 * The sweep is drawn rather than composed from bars because the figure it
 * carries is a fraction of a whole term, and a closing circle is the one shape
 * that says "how much of this is behind me" without a label.
 *
 * The centre counts installments rather than showing a percentage. One payment
 * into a 120-month plan rounds to "0%", which reads as nothing done at all;
 * "1/120" is both true and the unit an EMI is actually lived in.
 */
@Composable
private fun ScheduleRing(
    progress: Float,
    complete: Boolean,
    paid: Int,
    total: Int,
    modifier: Modifier = Modifier,
    diameter: Dp = 92.dp,
    thickness: Dp = 9.dp
) {
    val sweep by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "schedule_ring"
    )
    val fill = if (complete) {
        listOf(HeroFinance.positive.copy(alpha = 0.7f), HeroFinance.positive)
    } else {
        listOf(Palette300, Palette500)
    }

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = thickness.toPx()
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arcSize = Size(size.width - stroke, size.height - stroke)

            drawArc(
                color = Color.White.copy(alpha = 0.18f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // A round cap on a zero-length arc still paints a dot, which would
            // read as progress on a schedule nobody has started.
            if (sweep > 0f) {
                drawArc(
                    brush = Brush.linearGradient(fill),
                    startAngle = -90f,
                    sweepAngle = 360f * sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (total > 0) "$paid/$total" else "--",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnHero,
                maxLines = 1
            )
            Text(
                text = "paid",
                style = MaterialTheme.typography.labelSmall,
                color = OnHero.copy(alpha = 0.70f),
                maxLines = 1
            )
        }
    }
}

/** Skeleton in the shape of the real screen, so nothing jumps on first load. */
@Composable
private fun EmiSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
    ) {
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 236.dp, cornerRadius = 30.dp)
        Spacer(Modifier.height(AurexTheme.spacing.extraSmall))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 160.dp, cornerRadius = 24.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 160.dp, cornerRadius = 24.dp)
    }
}
