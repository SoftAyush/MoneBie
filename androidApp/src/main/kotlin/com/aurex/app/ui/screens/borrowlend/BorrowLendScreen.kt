package com.aurex.app.ui.screens.borrowlend

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Handshake
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurex.app.AurexApplication
import com.aurex.app.core.MoneyFormatter
import com.aurex.app.domain.model.LoanDirection
import com.aurex.app.presentation.borrowlend.BorrowLendListViewModel
import com.aurex.app.presentation.borrowlend.LoanListItem
import com.aurex.app.presentation.borrowlend.BorrowLendUiState
import com.aurex.app.ui.components.AmbientBackdrop
import com.aurex.app.ui.components.CountUpMoneyText
import com.aurex.app.ui.components.EmptyState
import com.aurex.app.ui.components.FilterPill
import com.aurex.app.ui.components.Segment
import com.aurex.app.ui.components.SegmentedSelector
import com.aurex.app.ui.components.HeroCardShape
import com.aurex.app.ui.components.HeroDecoration
import com.aurex.app.ui.components.HeroFinance
import com.aurex.app.ui.components.HeroNote
import com.aurex.app.ui.components.HeroPill
import com.aurex.app.ui.components.HeroSplitBar
import com.aurex.app.ui.components.HeroTile
import com.aurex.app.ui.components.MicroLabel
import com.aurex.app.ui.components.OnHero
import com.aurex.app.ui.components.PremiumCard
import com.aurex.app.ui.components.ShimmerBlock
import com.aurex.app.ui.components.heroBrush
import com.aurex.app.ui.theme.AurexTheme
import com.aurex.app.ui.theme.Palette700
import com.aurex.app.ui.theme.Palette900

/**
 * Money between the user and other people.
 *
 * The two directions get one list each rather than a single mixed one: reading
 * the wrong way round is the failure mode this module exists to prevent. Which
 * side you are reading is a pinned control, and the hero keeps both totals and
 * the net between them on screen whichever side is open - knowing one without
 * the other tells you almost nothing about your position.
 */
@Composable
fun BorrowLendScreen(
    onAddRecord: (LoanDirection) -> Unit,
    onOpenRecord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AurexApplication

    val viewModel: BorrowLendListViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                BorrowLendListViewModel(
                    getLoanRecordsUseCase = app.appContainer.getLoanRecordsUseCase,
                    getSelectedCurrencySymbolUseCase = app.appContainer.getSelectedCurrencySymbolUseCase
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBorrowed = uiState.direction == LoanDirection.BORROWED

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            BorrowLendBar(uiState = uiState)

            DirectionBar(
                uiState = uiState,
                onDirectionSelected = viewModel::setDirection,
                onToggleSettled = { viewModel.setShowSettled(!uiState.showSettled) }
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
                    item(key = "skeleton") { BorrowLendSkeleton() }
                } else {
                    item(key = "hero") { PositionHero(uiState = uiState) }
                }

                if (uiState.isEmpty) {
                    item(key = "empty") {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                icon = Icons.Rounded.Handshake,
                                title = if (isBorrowed) "Nothing borrowed" else "Nothing lent out",
                                message = if (isBorrowed) {
                                    "Record money you have borrowed from someone, and log " +
                                        "repayments as you settle up."
                                } else {
                                    "Record money you have lent someone, and log it as they pay " +
                                        "you back."
                                },
                                actionLabel = if (isBorrowed) {
                                    "Record a borrowing"
                                } else {
                                    "Record a lending"
                                },
                                onAction = { onAddRecord(uiState.direction) }
                            )
                        }
                    }
                }

                items(uiState.items, key = { it.record.id }) { item ->
                    LoanCard(
                        item = item,
                        currencySymbol = uiState.currencySymbol,
                        onClick = { onOpenRecord(item.record.id) }
                    )
                }
            }
        }

        // Adds to whichever side is open, so the direction is already right.
        FloatingActionButton(
            onClick = { onAddRecord(uiState.direction) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AurexTheme.spacing.medium)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = if (isBorrowed) {
                    "Record a borrowing"
                } else {
                    "Record a lending"
                }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Pinned: title and direction
// -----------------------------------------------------------------------------

@Composable
private fun BorrowLendBar(
    uiState: BorrowLendUiState,
    modifier: Modifier = Modifier
) {
    val openCount = uiState.openBorrowCount + uiState.openLendCount

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
                text = "Borrow & Lend",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            // Overdue takes the line whenever there is any: a date that has
            // passed is the only thing here that needs a conversation today.
            MicroLabel(
                text = when {
                    uiState.isLoading -> "Loading"
                    uiState.overdueCount == 1 -> "1 overdue"
                    uiState.overdueCount > 1 -> "${uiState.overdueCount} overdue"
                    openCount == 1 -> "1 open arrangement"
                    openCount > 1 -> "$openCount open arrangements"
                    else -> "All squared up"
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

/**
 * Which side you are reading, and whether settled records come with it.
 *
 * Pinned rather than scrolling with the hero: on a screen whose whole risk is
 * reading the wrong direction, the control that sets the direction should never
 * be off screen while the list is.
 */
@Composable
private fun DirectionBar(
    uiState: BorrowLendUiState,
    onDirectionSelected: (LoanDirection) -> Unit,
    onToggleSettled: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AurexTheme.spacing.medium,
                vertical = AurexTheme.spacing.small
            ),
        verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
    ) {
        /*
         * A switch, not two filters.
         *
         * These were a pair of chips, each tinted with its own side's colour
         * even when unselected - which meant the one you were not looking at
         * was outlined in bright red and read as switched on. Only one side can
         * ever be showing, so it belongs in the control the app already uses
         * for exactly that: the selected segment takes its accent, the other
         * goes quiet, and the direction is unmistakable.
         */
        SegmentedSelector(
            segments = listOf(
                Segment(
                    value = LoanDirection.LENT,
                    label = countedLabel("Owed to you", uiState.openLendCount),
                    accent = AurexTheme.finance.receivable
                ),
                Segment(
                    value = LoanDirection.BORROWED,
                    label = countedLabel("You owe", uiState.openBorrowCount),
                    accent = AurexTheme.finance.negative
                )
            ),
            selected = uiState.direction,
            onSelect = onDirectionSelected
        )

        // Settled records are kept, never deleted, so they stay available as a
        // record of what was already squared up. On its own line because it is
        // a different kind of control: the switch above chooses what you are
        // looking at, this only widens it.
        Row(modifier = Modifier.fillMaxWidth()) {
            FilterPill(
                label = "Include settled",
                selected = uiState.showSettled,
                onClick = onToggleSettled,
                icon = if (uiState.showSettled) Icons.Rounded.Check else null
            )
        }
    }
}

private fun countedLabel(label: String, count: Int): String =
    if (count > 0) "$label · $count" else label

// -----------------------------------------------------------------------------
// The hero
// -----------------------------------------------------------------------------

/**
 * Both sides at once, and the tilt between them.
 *
 * The net leads because a single side is meaningless on its own - being owed
 * 500 matters only next to what you owe - and the scale under it shows which
 * way the arrangement leans before either figure is read.
 */
@Composable
private fun PositionHero(
    uiState: BorrowLendUiState,
    modifier: Modifier = Modifier
) {
    val net = uiState.netPosition
    val nothingOutstanding = uiState.totalOwedToUser.isZero && uiState.totalOwedByUser.isZero

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
                MicroLabel(text = "Net position", color = OnHero.copy(alpha = 0.75f))
                Spacer(Modifier.weight(1f))
                if (uiState.overdueCount > 0) {
                    HeroPill(
                        text = if (uiState.overdueCount == 1) {
                            "1 overdue"
                        } else {
                            "${uiState.overdueCount} overdue"
                        },
                        accent = HeroFinance.negative
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            CountUpMoneyText(
                money = net,
                currencySymbol = uiState.currencySymbol,
                style = MaterialTheme.typography.displaySmall,
                color = when {
                    net.isPositive -> HeroFinance.receivable
                    net.isNegative -> HeroFinance.negative
                    else -> OnHero
                },
                // The sign is the whole message here: which way the balance
                // between you and other people falls.
                signed = !nothingOutstanding
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = when {
                    nothingOutstanding -> "Nothing outstanding either way"
                    net.isPositive -> "More is owed to you than you owe"
                    net.isNegative -> "You owe more than is owed to you"
                    else -> "Both sides come to exactly the same"
                },
                style = MaterialTheme.typography.bodySmall,
                color = OnHero.copy(alpha = 0.75f)
            )

            Spacer(Modifier.height(18.dp))

            HeroSplitBar(
                leftValue = uiState.totalOwedToUser.minorUnits,
                rightValue = uiState.totalOwedByUser.minorUnits,
                leftColor = HeroFinance.receivable,
                rightColor = HeroFinance.negative
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile(
                    label = "Owed to you",
                    value = MoneyFormatter.compact(
                        uiState.totalOwedToUser,
                        uiState.currencySymbol
                    ),
                    valueColor = HeroFinance.receivable,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "You owe",
                    value = MoneyFormatter.compact(
                        uiState.totalOwedByUser,
                        uiState.currencySymbol
                    ),
                    valueColor = HeroFinance.negative,
                    modifier = Modifier.weight(1f)
                )
            }

            if (nothingOutstanding) {
                Spacer(Modifier.height(14.dp))
                HeroNote(
                    icon = Icons.Rounded.TaskAlt,
                    text = "Every arrangement is settled",
                    accent = HeroFinance.positive
                )
            } else if (uiState.overdueCount > 0) {
                Spacer(Modifier.height(14.dp))
                HeroNote(
                    icon = Icons.Rounded.PriorityHigh,
                    text = if (uiState.overdueCount == 1) {
                        "1 arrangement is past the date you agreed"
                    } else {
                        "${uiState.overdueCount} arrangements are past the dates you agreed"
                    },
                    accent = HeroFinance.negative
                )
            } else {
                // Nothing overdue, so the useful thing is what comes next.
                val next = uiState.nextDue
                if (next?.daysUntilDue != null) {
                    Spacer(Modifier.height(14.dp))
                    NextDueNote(item = next, currencySymbol = uiState.currencySymbol)
                }
            }
        }
    }
}

/**
 * Which arrangement comes up next, and which way it goes.
 *
 * Names the person and the direction in the same line: "in 12d" is useless on a
 * screen where half the entries are money coming to you and half is money going
 * out, and the direction is the one thing a reader must not have to infer.
 */
@Composable
private fun NextDueNote(
    item: LoanListItem,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val days = item.daysUntilDue ?: return
    val record = item.record
    val borrowed = record.direction == LoanDirection.BORROWED

    val when_ = when {
        days == 0L -> "due today"
        days == 1L -> "due tomorrow"
        days <= 30L -> "due in ${days}d"
        else -> "due in ${days / 30L}mo"
    }

    val amount = MoneyFormatter.format(
        record.remainingAmount,
        currencySymbol,
        showDecimals = false
    )

    HeroNote(
        icon = Icons.Rounded.EventAvailable,
        text = if (borrowed) {
            "You owe ${record.personName} $amount, $when_"
        } else {
            "${record.personName} owes you $amount, $when_"
        },
        accent = if (borrowed) HeroFinance.negative else HeroFinance.receivable,
        modifier = modifier
    )
}

/** Skeleton in the shape of the real screen, so nothing jumps on first load. */
@Composable
private fun BorrowLendSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
    ) {
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 228.dp, cornerRadius = 30.dp)
        Spacer(Modifier.height(AurexTheme.spacing.extraSmall))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 132.dp, cornerRadius = 24.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 132.dp, cornerRadius = 24.dp)
    }
}
