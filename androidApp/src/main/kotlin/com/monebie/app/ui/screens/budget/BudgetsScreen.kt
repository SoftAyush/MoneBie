package com.monebie.app.ui.screens.budget

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Savings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.monebie.app.MoneBieApplication
import com.monebie.app.core.MoneyFormatter
import com.monebie.app.domain.model.BudgetLevel
import com.monebie.app.domain.model.BudgetOverview
import com.monebie.app.domain.model.BudgetStatus
import com.monebie.app.domain.model.TransactionCategory
import com.monebie.app.presentation.budget.BudgetEvent
import com.monebie.app.presentation.budget.BudgetFormState
import com.monebie.app.presentation.budget.BudgetsViewModel
import com.monebie.app.ui.components.AmbientBackdrop
import com.monebie.app.ui.components.MoneBieButton
import com.monebie.app.ui.components.MoneBieIcons
import com.monebie.app.ui.components.MoneBieSnackbarHost
import com.monebie.app.ui.components.CategoryPicker
import com.monebie.app.ui.components.ConfirmDialog
import com.monebie.app.ui.components.CountUpMoneyText
import com.monebie.app.ui.components.DetailTopBar
import com.monebie.app.ui.components.EmptyState
import com.monebie.app.ui.components.FormField
import com.monebie.app.ui.components.GlassIconButton
import com.monebie.app.ui.components.GlowBadge
import com.monebie.app.ui.components.HeroCardShape
import com.monebie.app.ui.components.HeroDecoration
import com.monebie.app.ui.components.HeroFinance
import com.monebie.app.ui.components.HeroTile
import com.monebie.app.ui.components.MicroLabel
import com.monebie.app.ui.components.MoneyInputField
import com.monebie.app.ui.components.MoneyText
import com.monebie.app.ui.components.OnHero
import com.monebie.app.ui.components.PremiumCard
import com.monebie.app.ui.components.PremiumSectionHeader
import com.monebie.app.ui.components.ShimmerBlock
import com.monebie.app.ui.components.heroBrush
import com.monebie.app.ui.components.pressable
import com.monebie.app.ui.components.showMoneBieMessage
import com.monebie.app.ui.theme.MoneBieTheme
import com.monebie.app.ui.theme.Palette700
import com.monebie.app.ui.theme.Palette900

/**
 * Monthly budgets.
 *
 * Paged by month like insights, because the question is "how did I do in
 * August" as often as "how am I doing now". Only the limits can be edited;
 * what was spent is the ledger's to say.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneBieApplication

    val viewModel: BudgetsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                BudgetsViewModel(
                    getBudgetOverviewUseCase = app.appContainer.getBudgetOverviewUseCase,
                    saveBudgetUseCase = app.appContainer.saveBudgetUseCase,
                    deleteBudgetUseCase = app.appContainer.deleteBudgetUseCase
                )
            }
        }
    )

    val overview by viewModel.uiState.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<BudgetStatus?>(null) }

    LaunchedEffect(event) {
        when (val current = event) {
            is BudgetEvent.Saved -> snackbarHostState.showMoneBieMessage("Budget for ${current.categoryName} saved.")
            is BudgetEvent.Deleted -> snackbarHostState.showMoneBieMessage("Removed the ${current.categoryName} budget.")
            is BudgetEvent.Failed -> snackbarHostState.showMoneBieMessage(current.message)
            null -> Unit
        }
        if (event != null) viewModel.consumeEvent()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                title = "Budgets",
                onBack = onBack,
                subtitle = overview.monthLabel
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
                    enabled = overview.canGoToNextMonth
                )
                GlassIconButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = "New budget",
                    onClick = { viewModel.startCreating() },
                    enabled = overview.availableCategories.isNotEmpty()
                )
            }

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
                if (overview.isLoading) {
                    item(key = "skeleton") {
                        Column(verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small)) {
                            ShimmerBlock(Modifier.fillMaxWidth().height(180.dp))
                            ShimmerBlock(Modifier.fillMaxWidth().height(72.dp))
                            ShimmerBlock(Modifier.fillMaxWidth().height(72.dp))
                        }
                    }
                    return@LazyColumn
                }

                if (!overview.hasBudgets) {
                    item(key = "empty") {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                icon = Icons.Rounded.Savings,
                                title = "No budgets yet",
                                message = "Set a monthly limit on a category and MoneBie will " +
                                    "show how much of it is left as the month goes on.",
                                actionLabel = if (overview.availableCategories.isNotEmpty()) "Set a budget" else null,
                                onAction = { viewModel.startCreating() }
                            )
                        }
                    }
                    return@LazyColumn
                }

                item(key = "hero") { BudgetHero(overview) }

                item(key = "list_header") {
                    PremiumSectionHeader(
                        title = "By category",
                        trailing = {
                            Text(
                                text = "${overview.statuses.size} " +
                                    if (overview.statuses.size == 1) "budget" else "budgets",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                items(overview.statuses, key = { it.budget.id }) { status ->
                    BudgetRow(
                        status = status,
                        currencySymbol = overview.currencySymbol,
                        onClick = { viewModel.startEditing(status) }
                    )
                }
            }
        }

        MoneBieSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (form.isVisible) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissForm,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            BudgetFormSheet(
                form = form,
                overview = overview,
                onCategoryChange = viewModel::setCategory,
                onAmountChange = viewModel::setAmount,
                onSave = viewModel::save,
                onDelete = if (form.isEditing) {
                    { pendingDelete = overview.statuses.firstOrNull { it.budget.id == form.editingId } }
                } else {
                    null
                }
            )
        }
    }

    pendingDelete?.let { status ->
        ConfirmDialog(
            title = "Remove this budget?",
            message = "The ${status.category.name} budget goes; nothing you spent does.",
            confirmLabel = "Remove",
            isDestructive = true,
            onConfirm = {
                viewModel.delete(status)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun BudgetHero(overview: BudgetOverview, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 22.dp, shape = HeroCardShape, ambientColor = Palette900, spotColor = Palette700)
            .clip(HeroCardShape)
            .background(heroBrush())
    ) {
        HeroDecoration(Modifier.matchParentSize())

        Column(modifier = Modifier.padding(20.dp)) {
            MicroLabel(
                text = "Left to spend in ${overview.monthLabel}",
                color = OnHero.copy(alpha = 0.75f)
            )

            Spacer(Modifier.height(8.dp))

            CountUpMoneyText(
                money = overview.totalRemaining.coerceAtLeastZero(),
                currencySymbol = overview.currencySymbol,
                style = MaterialTheme.typography.displaySmall,
                color = if (overview.totalRemaining.isNegative) HeroFinance.negative else OnHero
            )

            Spacer(Modifier.height(12.dp))

            HeroProgress(progress = overview.overallProgress, over = overview.totalRemaining.isNegative)

            Spacer(Modifier.height(6.dp))

            Text(
                text = when {
                    overview.overCount > 0 ->
                        "${overview.overCount} over the limit" +
                            if (overview.nearCount > 0) ", ${overview.nearCount} close to it" else ""
                    overview.nearCount > 0 -> "${overview.nearCount} close to the limit"
                    else -> "Everything within budget"
                },
                style = MaterialTheme.typography.bodySmall,
                color = OnHero.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile(
                    label = "Budgeted",
                    value = MoneyFormatter.compact(overview.totalLimit, overview.currencySymbol),
                    valueColor = OnHero,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Spent",
                    value = MoneyFormatter.compact(overview.totalSpent, overview.currencySymbol),
                    valueColor = if (overview.totalRemaining.isNegative) HeroFinance.negative else OnHero,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Used",
                    value = MoneyFormatter.percent(overview.overallProgress),
                    valueColor = OnHero,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroProgress(progress: Float, over: Boolean) {
    val width by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "budget_hero_bar"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(OnHero.copy(alpha = 0.18f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(width)
                .height(8.dp)
                .clip(CircleShape)
                .background(if (over) HeroFinance.negative else OnHero)
        )
    }
}

@Composable
private fun BudgetRow(
    status: BudgetStatus,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = budgetAccent(status.level)
    val width by animateFloatAsState(
        targetValue = status.progress,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "budget_bar"
    )

    PremiumCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
        ) {
            GlowBadge(
                icon = MoneBieIcons.forCategory(status.category.iconKey),
                tint = accent,
                size = 40.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = status.category.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (status.level) {
                        BudgetLevel.OVER ->
                            MoneyFormatter.format(status.remaining.absolute, currencySymbol, showDecimals = false) +
                                " over"
                        else ->
                            MoneyFormatter.format(status.remaining, currencySymbol, showDecimals = false) +
                                " left"
                    } + " · " + status.transactionCount +
                        if (status.transactionCount == 1) " entry" else " entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.isOver) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                MoneyText(
                    money = status.spent,
                    currencySymbol = currencySymbol,
                    style = MaterialTheme.typography.titleSmall,
                    showDecimals = false
                )
                Text(
                    text = "of " + MoneyFormatter.compact(status.limit, currencySymbol),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.small))

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

@Composable
private fun budgetAccent(level: BudgetLevel): Color = when (level) {
    BudgetLevel.OK -> MoneBieTheme.finance.positive
    BudgetLevel.NEAR -> MoneBieTheme.finance.caution
    BudgetLevel.OVER -> MoneBieTheme.finance.negative
}

@Composable
private fun BudgetFormSheet(
    form: BudgetFormState,
    overview: BudgetOverview,
    onCategoryChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?
) {
    // Editing keeps the category fixed: a budget is *for* a category, and
    // moving it is really deleting one and creating another.
    val editingCategory: TransactionCategory? =
        overview.statuses.firstOrNull { it.budget.id == form.editingId }?.category
    val choices = if (editingCategory != null) listOf(editingCategory) else overview.availableCategories

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MoneBieTheme.spacing.large)
            .imePadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
    ) {
        Text(
            text = if (form.isEditing) "Edit budget" else "New budget",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        FormField(label = "Category") {
            CategoryPicker(
                categories = choices,
                selectedId = form.categoryId,
                onSelect = onCategoryChange
            )
        }

        FormField(label = "Monthly limit", error = form.error) {
            MoneyInputField(
                value = form.amountInput,
                currencySymbol = overview.currencySymbol,
                onValueChange = onAmountChange
            )
        }

        MoneBieButton(
            text = if (form.isEditing) "Save changes" else "Set budget",
            onClick = onSave,
            loading = form.isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        if (onDelete != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .pressable(onDelete)
                    .padding(vertical = MoneBieTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small)
            ) {
                GlowBadge(
                    icon = Icons.Rounded.DeleteOutline,
                    tint = MaterialTheme.colorScheme.error,
                    size = 34.dp
                )
                Text(
                    text = "Remove this budget",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.small))
    }
}
