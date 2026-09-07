package com.aurex.app.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurex.app.AurexApplication
import com.aurex.app.core.formatMonthYear
import com.aurex.app.core.getCurrentHour
import com.aurex.app.domain.model.DashboardSnapshot
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.presentation.dashboard.DashboardUiState
import com.aurex.app.presentation.dashboard.DashboardViewModel
import com.aurex.app.ui.components.AmbientBackdrop
import com.aurex.app.ui.components.CardDivider
import com.aurex.app.ui.components.EmptyState
import com.aurex.app.ui.components.PremiumCard
import com.aurex.app.ui.components.PremiumSectionHeader
import com.aurex.app.ui.components.ErrorState
import com.aurex.app.ui.components.ShimmerBlock
import com.aurex.app.ui.components.entrance
import com.aurex.app.ui.components.rememberEntrance
import com.aurex.app.ui.theme.AurexTheme

/**
 * The Finance Dashboard.
 *
 * Reads a single pre-computed DashboardSnapshot and performs no arithmetic of
 * its own - every figure here was produced by FinancialCalculator, so this
 * screen cannot disagree with the module screens that show the same numbers.
 *
 * Ordering follows how often a figure is needed: balance and net position, then
 * the actions worth one tap, then today, then the month, then obligations, then
 * history. Everything sits over one ambient backdrop rather than a flat fill,
 * which is what lets the cards read as floating above the page.
 */
@Composable
fun DashboardScreen(
    onOpenAccounts: () -> Unit,
    onOpenOverview: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenBorrowLend: () -> Unit,
    onAddTransaction: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AurexApplication

    val viewModel: DashboardViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    getDashboardSnapshotUseCase = app.appContainer.getDashboardSnapshotUseCase
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Resolved once per composition rather than per recomposition; the greeting
    // does not need to track the clock.
    val greeting = remember { greetingForHour(getCurrentHour()) }

    Box(modifier = modifier.fillMaxSize()) {
        // Fixed behind the list, so the light stays put while the content moves
        // over it - the effect reads as a page lit from above rather than as
        // decoration glued to the first card.
        AmbientBackdrop(Modifier.matchParentSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = AurexTheme.spacing.medium,
                end = AurexTheme.spacing.medium,
                top = AurexTheme.spacing.medium,
                // Clears the bottom bar and the floating action button.
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)
        ) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> item { DashboardSkeleton() }

                is DashboardUiState.Error -> item {
                    ErrorState(message = state.message, icon = Icons.Rounded.CloudOff)
                }

                is DashboardUiState.Content -> dashboardContent(
                    snapshot = state.snapshot,
                    greeting = greeting,
                    onOpenAccounts = onOpenAccounts,
                    onOpenOverview = onOpenOverview,
                    onOpenInsights = onOpenInsights,
                    onOpenReminders = onOpenReminders,
                    onOpenProfile = onOpenProfile,
                    onOpenTransactions = onOpenTransactions,
                    onOpenDebts = onOpenDebts,
                    onOpenBorrowLend = onOpenBorrowLend,
                    onAddTransaction = onAddTransaction
                )
            }
        }
    }
}

private fun LazyListScope.dashboardContent(
    snapshot: DashboardSnapshot,
    greeting: String,
    onOpenAccounts: () -> Unit,
    onOpenOverview: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenBorrowLend: () -> Unit,
    onAddTransaction: (TransactionType) -> Unit
) {
    item(key = "header") {
        // The three blocks above the fold arrive staggered, so the screen
        // assembles itself instead of flashing in complete.
        val progress = rememberEntrance()
        DashboardHeader(
            greeting = greeting,
            userName = snapshot.userName,
            onOpenAccounts = onOpenAccounts,
            onOpenProfile = onOpenProfile,
            modifier = Modifier.entrance(progress, rise = 12.dp)
        )
    }

    item(key = "hero") {
        val progress = rememberEntrance(delayMillis = 90)
        BalanceHeroCard(
            totalBalance = snapshot.totalBalance,
            netPosition = snapshot.netPosition,
            currencySymbol = snapshot.currencySymbol,
            // The hero summarises the net position, so it opens the full
            // balance sheet rather than the account list.
            onClick = onOpenOverview,
            modifier = Modifier.entrance(progress)
        )
    }

    // Sits directly under the balance because it is the one thing on this
    // screen that is already costing the user something.
    if (snapshot.upcomingPayments.any { it.isOverdue }) {
        item(key = "overdue") {
            val progress = rememberEntrance(delayMillis = 130)
            OverduePrompt(
                onOpenReminders = onOpenReminders,
                modifier = Modifier.entrance(progress)
            )
        }
    }

    item(key = "quick_actions") {
        val progress = rememberEntrance(delayMillis = 170)
        QuickActionsRow(
            onAddIncome = { onAddTransaction(TransactionType.INCOME) },
            onAddExpense = { onAddTransaction(TransactionType.EXPENSE) },
            onOpenInsights = onOpenInsights,
            onOpenReminders = onOpenReminders,
            modifier = Modifier.entrance(progress)
        )
    }

    // The stagger carries on past the fold rather than stopping at the quick
    // actions: the cards below are the ones scrolled to within a second of the
    // page opening, and they used to arrive already in place mid-animation.
    item(key = "today") {
        val progress = rememberEntrance(delayMillis = 210)
        TodayCard(
            summary = snapshot.todaySummary,
            usualDailyExpense = snapshot.usualDailyExpense,
            today = snapshot.today,
            currencySymbol = snapshot.currencySymbol,
            modifier = Modifier.entrance(progress, rise = 16.dp)
        )
    }

    item(key = "month") {
        val progress = rememberEntrance(delayMillis = 250)
        MonthOverviewCard(
            summary = snapshot.monthSummary,
            trend = snapshot.weeklyTrend,
            monthLabel = formatMonthYear(snapshot.today),
            today = snapshot.today,
            currencySymbol = snapshot.currencySymbol,
            // The month card is the natural way into a deeper breakdown of it.
            onClick = onOpenInsights,
            modifier = Modifier.entrance(progress, rise = 16.dp)
        )
    }

    item(key = "owe_owed") {
        val progress = rememberEntrance(delayMillis = 290)
        OweAndOwedRow(
            snapshot = snapshot,
            onOpenDebts = onOpenDebts,
            onOpenBorrowLend = onOpenBorrowLend,
            modifier = Modifier.entrance(progress, rise = 16.dp)
        )
    }

    item(key = "upcoming_header") {
        PremiumSectionHeader(
            title = "Upcoming payments",
            actionLabel = if (snapshot.upcomingPayments.isNotEmpty()) "See all" else null,
            onActionClick = if (snapshot.upcomingPayments.isNotEmpty()) onOpenReminders else null
        )
    }

    if (snapshot.upcomingPayments.isEmpty()) {
        item(key = "upcoming_empty") { UpcomingPaymentsEmpty() }
    } else {
        items(snapshot.upcomingPayments, key = { "upcoming_" + it.id }) { payment ->
            UpcomingPaymentRow(payment = payment, currencySymbol = snapshot.currencySymbol)
        }
    }

    item(key = "recent_header") {
        PremiumSectionHeader(
            title = "Recent activity",
            actionLabel = "See all",
            onActionClick = onOpenTransactions
        )
    }

    item(key = "recent_list") {
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            if (snapshot.recentTransactions.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = "No transactions yet",
                    message = "Record your first income or expense to start building your history.",
                    actionLabel = "Add a transaction",
                    onAction = { onAddTransaction(TransactionType.EXPENSE) }
                )
            } else {
                snapshot.recentTransactions.forEachIndexed { index, transaction ->
                    if (index > 0) CardDivider(alpha = 0.07f)
                    TransactionRow(
                        transaction = transaction,
                        accountName = snapshot.accountName(transaction.accountId),
                        currencySymbol = snapshot.currencySymbol,
                        category = snapshot.category(transaction.categoryId)
                    )
                }
            }
        }
    }

    if (snapshot.accounts.isNotEmpty()) {
        item(key = "accounts_header") {
            PremiumSectionHeader(
                title = "Accounts",
                actionLabel = "Manage",
                onActionClick = onOpenAccounts
            )
        }

        item(key = "accounts_strip") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(snapshot.accounts, key = { it.id }) { account ->
                    AccountChip(account = account, currencySymbol = snapshot.currencySymbol)
                }
            }
        }
    }
}

/** Skeleton mirroring the real layout, so content does not jump on first load. */
@Composable
private fun DashboardSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)) {
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.55f), height = 44.dp, cornerRadius = 22.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 224.dp, cornerRadius = 30.dp)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) {
                ShimmerBlock(
                    modifier = Modifier.weight(1f),
                    height = 86.dp,
                    cornerRadius = 20.dp
                )
            }
        }

        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 108.dp, cornerRadius = 24.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 268.dp, cornerRadius = 24.dp)
        Spacer(Modifier.height(AurexTheme.spacing.extraSmall))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.4f), height = 20.dp)
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 84.dp, cornerRadius = 24.dp)
    }
}

private fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}
