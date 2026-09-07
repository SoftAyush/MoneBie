package com.aurex.app.ui.screens.reminders

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurex.app.AurexApplication
import com.aurex.app.core.Money
import com.aurex.app.core.MoneyFormatter
import com.aurex.app.core.totalOf
import com.aurex.app.domain.model.DueBucket
import com.aurex.app.domain.model.UpcomingPayment
import com.aurex.app.domain.usecase.reminders.ReminderBoard
import com.aurex.app.presentation.reminders.RemindersViewModel
import com.aurex.app.ui.components.AmbientBackdrop
import com.aurex.app.ui.components.CountUpMoneyText
import com.aurex.app.ui.components.DetailTopBar
import com.aurex.app.ui.components.EmptyState
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
import com.aurex.app.ui.screens.dashboard.UpcomingPaymentRow
import com.aurex.app.ui.theme.AurexTheme
import com.aurex.app.ui.theme.Palette300
import com.aurex.app.ui.theme.Palette700
import com.aurex.app.ui.theme.Palette900

/**
 * Everything with a due date, in one place.
 *
 * Grouped by urgency rather than listed flat: "overdue" and "due in three
 * weeks" call for completely different reactions, and a single sorted list
 * leaves the reader to work that out.
 */
@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AurexApplication

    val viewModel: RemindersViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                RemindersViewModel(
                    getUpcomingPaymentsUseCase = app.appContainer.getUpcomingPaymentsUseCase
                )
            }
        }
    )

    val board by viewModel.uiState.collectAsStateWithLifecycle()
    val total = board.buckets.values.sumOf { it.size }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                title = "Upcoming",
                onBack = onBack,
                subtitle = when {
                    board.isLoading -> "Loading"
                    board.overdueCount == 1 -> "1 overdue"
                    board.overdueCount > 1 -> "${board.overdueCount} overdue"
                    total == 1 -> "1 scheduled"
                    total > 1 -> "$total scheduled"
                    else -> "Nothing due"
                }
            )

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
                if (board.isLoading) {
                    item(key = "skeleton") { RemindersSkeleton() }
                    return@LazyColumn
                }

                if (board.isEmpty) {
                    item(key = "empty") {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                icon = Icons.Rounded.EventAvailable,
                                title = "Nothing due",
                                message = "No debts, EMIs or personal loans have an upcoming due " +
                                    "date. Anything with a date will show up here as it approaches."
                            )
                        }
                    }
                    return@LazyColumn
                }

                item(key = "hero") { DueHero(board) }

                board.buckets.forEach { (bucket, payments) ->
                    item(key = "bucket_" + bucket.name) {
                        BucketHeader(bucket = bucket, count = payments.size)
                    }

                    items(
                        count = payments.size,
                        key = { index -> "payment_" + payments[index].id }
                    ) { index ->
                        UpcomingPaymentRow(
                            payment = payments[index],
                            currencySymbol = board.currencySymbol
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
 * What has to be found, and when.
 *
 * The headline is whatever needs acting on first - anything already late, and
 * otherwise the week ahead - because a reminder screen that leads with a
 * distant total buries the only figure that is urgent.
 */
@Composable
private fun DueHero(
    board: ReminderBoard,
    modifier: Modifier = Modifier
) {
    val overdue = board.hasOverdue

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
                text = if (overdue) "Already overdue" else "Due within a week",
                color = OnHero.copy(alpha = 0.75f)
            )

            Spacer(Modifier.height(8.dp))

            CountUpMoneyText(
                money = if (overdue) board.overdueTotal else board.dueThisWeekTotal,
                currencySymbol = board.currencySymbol,
                style = MaterialTheme.typography.displaySmall,
                color = if (overdue) HeroFinance.negative else OnHero
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = when {
                    overdue && board.overdueCount == 1 -> "1 payment has passed its due date"
                    overdue -> "${board.overdueCount} payments have passed their due dates"
                    board.dueThisWeekTotal.isZero -> "Nothing falls due in the next seven days"
                    else -> "To find in the next seven days"
                },
                style = MaterialTheme.typography.bodySmall,
                color = OnHero.copy(alpha = 0.75f)
            )

            Spacer(Modifier.height(18.dp))

            DueProfile(buckets = board.buckets, currencySymbol = board.currencySymbol)

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile(
                    label = "Overdue",
                    value = MoneyFormatter.compact(board.overdueTotal, board.currencySymbol),
                    valueColor = if (overdue) HeroFinance.negative else OnHero,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Within a week",
                    value = MoneyFormatter.compact(board.dueThisWeekTotal, board.currencySymbol),
                    valueColor = OnHero,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            HeroNote(
                icon = if (overdue) Icons.Rounded.PriorityHigh else Icons.Rounded.TaskAlt,
                // Says what the totals cover, so a lending listed below is not
                // mistaken for something the user has to pay.
                text = "Totals cover money going out. Anything owed to you is listed but not " +
                    "counted here.",
                accent = if (overdue) HeroFinance.negative else null
            )
        }
    }
}

/**
 * What is coming, and when, as one column per window of time.
 *
 * Columns carry money rather than a count of rows: five small payments and one
 * large one need different plans, and only the amount says which is which. The
 * tallest is labelled and the rest are not - the shape is the point, and a
 * number on every column would bury it.
 */
@Composable
private fun DueProfile(
    buckets: Map<DueBucket, List<UpcomingPayment>>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    // Outgoing only, matching the totals above: money owed to the user is not
    // something to budget for.
    val amounts = DueBucket.entries.associateWith { bucket ->
        buckets[bucket].orEmpty().filterNot { it.isReceivable }.totalOf { it.amount }
    }
    val peak = amounts.values.maxOfOrNull { it.minorUnits } ?: 0L
    if (peak <= 0L) return

    val tallest = amounts.maxByOrNull { it.value.minorUnits }?.key

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        DueBucket.entries.forEach { bucket ->
            val amount = amounts[bucket] ?: Money.ZERO
            DueColumn(
                bucket = bucket,
                amount = amount,
                fraction = if (peak <= 0L) 0f else {
                    (amount.minorUnits.toDouble() / peak).toFloat().coerceIn(0f, 1f)
                },
                currencySymbol = currencySymbol,
                labelled = bucket == tallest,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DueColumn(
    bucket: DueBucket,
    amount: Money,
    fraction: Float,
    currencySymbol: String,
    labelled: Boolean,
    modifier: Modifier = Modifier
) {
    val height by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "due_column"
    )

    val colors = when {
        bucket == DueBucket.OVERDUE -> listOf(HeroFinance.negative, HeroFinance.negative)
        bucket == DueBucket.TODAY -> listOf(HeroFinance.caution, HeroFinance.negative)
        bucket == DueBucket.THIS_WEEK -> listOf(HeroFinance.caution, HeroFinance.caution)
        else -> listOf(Palette300, Palette700)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (labelled) MoneyFormatter.compact(amount, currencySymbol) else "",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = OnHero,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // A window with nothing in it keeps a visible floor rather
                    // than vanishing, so the row of windows stays readable.
                    .height((56.dp * height).coerceAtLeast(3.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.verticalGradient(colors))
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = bucket.shortLabel,
            style = MaterialTheme.typography.labelSmall,
            color = OnHero.copy(alpha = 0.70f),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/** Room for five labels across a phone means the bucket names have to be short. */
private val DueBucket.shortLabel: String
    get() = when (this) {
        DueBucket.OVERDUE -> "Late"
        DueBucket.TODAY -> "Today"
        DueBucket.THIS_WEEK -> "Week"
        DueBucket.THIS_MONTH -> "Month"
        DueBucket.LATER -> "Later"
    }

// -----------------------------------------------------------------------------
// The list
// -----------------------------------------------------------------------------

@Composable
private fun BucketHeader(
    bucket: DueBucket,
    count: Int,
    modifier: Modifier = Modifier
) {
    val accent = when (bucket) {
        DueBucket.OVERDUE -> AurexTheme.finance.negative
        DueBucket.TODAY -> AurexTheme.finance.caution
        DueBucket.THIS_WEEK -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AurexTheme.spacing.small, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The same brand tick the section headers elsewhere use, taking the
        // bucket's own colour so urgency is readable from the margin.
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.45f)))
                )
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = bucket.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (bucket.isUrgent) accent else MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

/** Skeleton in the shape of the real screen, so nothing jumps on first load. */
@Composable
private fun RemindersSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
    ) {
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 300.dp, cornerRadius = 30.dp)
        Spacer(Modifier.height(AurexTheme.spacing.extraSmall))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.35f), height = 18.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 96.dp, cornerRadius = 24.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 96.dp, cornerRadius = 24.dp)
    }
}
