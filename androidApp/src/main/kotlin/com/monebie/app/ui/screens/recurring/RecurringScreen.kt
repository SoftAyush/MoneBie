package com.aurex.app.ui.screens.recurring

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurex.app.AurexApplication
import com.aurex.app.core.MoneyFormatter
import com.aurex.app.core.formatDisplayDate
import com.aurex.app.core.relativeDueLabel
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.domain.model.displayName
import com.aurex.app.domain.model.perLabel
import com.aurex.app.presentation.recurring.RecurringEvent
import com.aurex.app.presentation.recurring.RecurringListItem
import com.aurex.app.presentation.recurring.RecurringListUiState
import com.aurex.app.presentation.recurring.RecurringListViewModel
import com.aurex.app.ui.components.AmbientBackdrop
import com.aurex.app.ui.components.AurexIcons
import com.aurex.app.ui.components.AurexSnackbarHost
import com.aurex.app.ui.components.CardDivider
import com.aurex.app.ui.components.ConfirmDialog
import com.aurex.app.ui.components.CountUpMoneyText
import com.aurex.app.ui.components.DetailTopBar
import com.aurex.app.ui.components.EmptyState
import com.aurex.app.ui.components.GlassIconButton
import com.aurex.app.ui.components.GlowBadge
import com.aurex.app.ui.components.HeroCardShape
import com.aurex.app.ui.components.HeroDecoration
import com.aurex.app.ui.components.HeroFinance
import com.aurex.app.ui.components.HeroTile
import com.aurex.app.ui.components.MicroLabel
import com.aurex.app.ui.components.MoneyText
import com.aurex.app.ui.components.OnHero
import com.aurex.app.ui.components.PremiumCard
import com.aurex.app.ui.components.PremiumSectionHeader
import com.aurex.app.ui.components.ShimmerBlock
import com.aurex.app.ui.components.StatusPill
import com.aurex.app.ui.components.heroBrush
import com.aurex.app.ui.components.pressable
import com.aurex.app.ui.components.showAurexMessage
import com.aurex.app.ui.theme.AurexTheme
import com.aurex.app.ui.theme.Palette700
import com.aurex.app.ui.theme.Palette900

/**
 * Recurring transactions.
 *
 * A list of what runs on its own, headed by what it adds up to per month.
 * Tapping a row opens an action sheet rather than a detail screen: a schedule
 * has no history of its own to show - its history is in the ledger - so the
 * only things to do with one are edit, pause, skip, post now, or remove.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    onBack: () -> Unit,
    onAdd: (TransactionType) -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AurexApplication

    val viewModel: RecurringListViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                RecurringListViewModel(
                    getRecurringTransactionsUseCase = app.appContainer.getRecurringTransactionsUseCase,
                    getCategoriesUseCase = app.appContainer.getCategoriesUseCase,
                    getAccountsUseCase = app.appContainer.getAccountsUseCase,
                    getSelectedCurrencySymbolUseCase = app.appContainer.getSelectedCurrencySymbolUseCase,
                    postDueRecurringUseCase = app.appContainer.postDueRecurringUseCase,
                    postRecurringNowUseCase = app.appContainer.postRecurringNowUseCase,
                    skipRecurringOccurrenceUseCase = app.appContainer.skipRecurringOccurrenceUseCase,
                    setRecurringPausedUseCase = app.appContainer.setRecurringPausedUseCase,
                    deleteRecurringTransactionUseCase = app.appContainer.deleteRecurringTransactionUseCase
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle()

    // Every return to the front, not just the first: a schedule created a
    // moment ago on the add form may already be due.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.postDue() }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var selected by remember { mutableStateOf<RecurringListItem?>(null) }
    var pendingDelete by remember { mutableStateOf<RecurringListItem?>(null) }

    LaunchedEffect(event) {
        when (val current = event) {
            is RecurringEvent.Posted -> snackbarHostState.showAurexMessage(
                if (current.count == 1) {
                    "Recorded " + current.names.first() + "."
                } else {
                    "Recorded ${current.count} entries: " + current.names.distinct().joinToString(", ") + "."
                }
            )
            is RecurringEvent.PostedOne -> snackbarHostState.showAurexMessage("Recorded ${current.name} for today.")
            is RecurringEvent.Skipped -> snackbarHostState.showAurexMessage("Skipped this ${current.name}.")
            is RecurringEvent.Paused -> snackbarHostState.showAurexMessage(
                if (current.paused) "${current.name} is paused." else "${current.name} is running again."
            )
            is RecurringEvent.Deleted -> snackbarHostState.showAurexMessage(
                "Removed ${current.name}. Entries already recorded stay in your history."
            )
            null -> Unit
        }
        if (event != null) viewModel.consumeEvent()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                title = "Recurring",
                onBack = onBack,
                subtitle = when {
                    uiState.isLoading -> "Loading"
                    uiState.active.size == 1 -> "1 running"
                    else -> "${uiState.active.size} running"
                },
                actions = {
                    GlassIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "New recurring transaction",
                        onClick = { onAdd(TransactionType.EXPENSE) }
                    )
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
                if (uiState.isLoading) {
                    item(key = "skeleton") {
                        Column(verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)) {
                            ShimmerBlock(Modifier.fillMaxWidth().height(170.dp))
                            ShimmerBlock(Modifier.fillMaxWidth().height(72.dp))
                            ShimmerBlock(Modifier.fillMaxWidth().height(72.dp))
                        }
                    }
                    return@LazyColumn
                }

                if (uiState.isEmpty) {
                    item(key = "empty") {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                icon = Icons.Rounded.EventRepeat,
                                title = "Nothing on repeat yet",
                                message = "Rent, salary, subscriptions - set them up once and Aurex " +
                                    "records each one on its day.",
                                actionLabel = "Add one",
                                onAction = { onAdd(TransactionType.EXPENSE) }
                            )
                        }
                    }
                    return@LazyColumn
                }

                item(key = "hero") { RecurringHero(uiState) }

                if (uiState.active.isNotEmpty()) {
                    item(key = "active_header") { PremiumSectionHeader(title = "Running") }
                    items(uiState.active, key = { "active_" + it.schedule.id }) { item ->
                        RecurringRow(item, uiState.currencySymbol, onClick = { selected = item })
                    }
                }

                if (uiState.paused.isNotEmpty()) {
                    item(key = "paused_header") { PremiumSectionHeader(title = "Paused") }
                    items(uiState.paused, key = { "paused_" + it.schedule.id }) { item ->
                        RecurringRow(item, uiState.currencySymbol, onClick = { selected = item })
                    }
                }

                if (uiState.ended.isNotEmpty()) {
                    item(key = "ended_header") { PremiumSectionHeader(title = "Finished") }
                    items(uiState.ended, key = { "ended_" + it.schedule.id }) { item ->
                        RecurringRow(item, uiState.currencySymbol, onClick = { selected = item })
                    }
                }
            }
        }

        AurexSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    selected?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            RecurringActionSheet(
                item = item,
                currencySymbol = uiState.currencySymbol,
                onEdit = { selected = null; onEdit(item.schedule.id) },
                onPostNow = { selected = null; viewModel.postNow(item) },
                onSkip = { selected = null; viewModel.skipNext(item) },
                onTogglePause = { selected = null; viewModel.setPaused(item, !item.schedule.isPaused) },
                onDelete = { selected = null; pendingDelete = item }
            )
        }
    }

    pendingDelete?.let { item ->
        ConfirmDialog(
            title = "Remove ${item.schedule.name}?",
            message = "It stops repeating. Everything it already recorded stays in your history.",
            confirmLabel = "Remove",
            isDestructive = true,
            onConfirm = {
                viewModel.delete(item)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun RecurringHero(state: RecurringListUiState, modifier: Modifier = Modifier) {
    val net = state.monthlyIncoming - state.monthlyOutgoing
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 22.dp, shape = HeroCardShape, ambientColor = Palette900, spotColor = Palette700)
            .clip(HeroCardShape)
            .background(heroBrush())
    ) {
        HeroDecoration(Modifier.matchParentSize())

        Column(modifier = Modifier.padding(20.dp)) {
            MicroLabel(text = "Going out each month", color = OnHero.copy(alpha = 0.75f))

            Spacer(Modifier.height(8.dp))

            CountUpMoneyText(
                money = state.monthlyOutgoing,
                currencySymbol = state.currencySymbol,
                style = MaterialTheme.typography.displaySmall,
                color = OnHero
            )

            Spacer(Modifier.height(6.dp))

            state.nextUp?.let { next ->
                Text(
                    text = "Next: " + next.schedule.name + " · " +
                        relativeDueLabel(next.daysUntilNext).replace("Due today", "Today"),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnHero.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile(
                    label = "Coming in",
                    value = MoneyFormatter.compact(state.monthlyIncoming, state.currencySymbol),
                    valueColor = HeroFinance.positive,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Net / month",
                    value = MoneyFormatter.compact(net, state.currencySymbol),
                    valueColor = if (net.isNegative) HeroFinance.negative else OnHero,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Schedules",
                    value = state.active.size.toString(),
                    valueColor = OnHero,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RecurringRow(
    item: RecurringListItem,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val schedule = item.schedule
    val accent = when {
        schedule.isPaused || schedule.hasEnded -> MaterialTheme.colorScheme.onSurfaceVariant
        schedule.type == TransactionType.INCOME -> AurexTheme.finance.positive
        schedule.type == TransactionType.EXPENSE -> AurexTheme.finance.negative
        else -> MaterialTheme.colorScheme.primary
    }

    PremiumCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)
        ) {
            GlowBadge(
                icon = AurexIcons.forCategory(item.category.iconKey),
                tint = accent,
                size = 40.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = schedule.frequency.displayName + " · " + item.accountName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                MoneyText(
                    money = schedule.amount,
                    currencySymbol = currencySymbol,
                    style = MaterialTheme.typography.titleSmall,
                    color = accent,
                    showDecimals = false
                )
                Spacer(Modifier.height(2.dp))
                when {
                    schedule.isPaused -> StatusPill(
                        text = "Paused",
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                    schedule.hasEnded -> StatusPill(
                        text = "Finished",
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                    else -> {
                        val soon = item.daysUntilNext <= 3
                        StatusPill(
                            text = relativeDueLabel(item.daysUntilNext).replace("Due today", "Today"),
                            contentColor = if (soon) AurexTheme.finance.caution else MaterialTheme.colorScheme.primary,
                            containerColor = (if (soon) AurexTheme.finance.caution else MaterialTheme.colorScheme.primary)
                                .copy(alpha = 0.12f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringActionSheet(
    item: RecurringListItem,
    currencySymbol: String,
    onEdit: () -> Unit,
    onPostNow: () -> Unit,
    onSkip: () -> Unit,
    onTogglePause: () -> Unit,
    onDelete: () -> Unit
) {
    val schedule = item.schedule

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AurexTheme.spacing.large)
            .navigationBarsPadding()
    ) {
        Text(
            text = schedule.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = MoneyFormatter.format(schedule.amount, currencySymbol, showDecimals = false) +
                " " + schedule.frequency.perLabel + " · " + item.category.name +
                if (schedule.isActive) " · next " + formatDisplayDate(schedule.nextDueDate) else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(AurexTheme.spacing.medium))

        PremiumCard(modifier = Modifier.fillMaxWidth(), contentPadding = AurexTheme.spacing.small) {
            ActionRow(Icons.Rounded.Edit, "Edit", "Change the amount, cycle or account", onEdit)
            if (schedule.isActive) {
                CardDivider(alpha = 0.07f)
                ActionRow(
                    Icons.Rounded.Check,
                    "Record now",
                    "Post this one today and move the schedule on",
                    onPostNow
                )
                CardDivider(alpha = 0.07f)
                ActionRow(
                    Icons.Rounded.SkipNext,
                    "Skip next",
                    "Miss " + formatDisplayDate(schedule.nextDueDate) + " without recording it",
                    onSkip
                )
            }
            if (!schedule.hasEnded) {
                CardDivider(alpha = 0.07f)
                ActionRow(
                    if (schedule.isPaused) Icons.Rounded.PlayCircle else Icons.Rounded.PauseCircle,
                    if (schedule.isPaused) "Resume" else "Pause",
                    if (schedule.isPaused) "Pick up from the next due date" else "Stop recording until resumed",
                    onTogglePause
                )
            }
            CardDivider(alpha = 0.07f)
            ActionRow(
                Icons.Rounded.DeleteOutline,
                "Remove",
                "Stops the schedule; recorded entries stay",
                onDelete,
                tint = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(AurexTheme.spacing.medium))
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .pressable(onClick)
            .padding(horizontal = AurexTheme.spacing.small, vertical = AurexTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)
    ) {
        GlowBadge(icon = icon, tint = tint, size = 38.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (tint == MaterialTheme.colorScheme.primary) MaterialTheme.colorScheme.onSurface else tint
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
