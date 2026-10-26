package com.monebie.app.ui.screens.emi

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.monebie.app.MoneBieApplication
import com.monebie.app.core.MoneyFormatter
import com.monebie.app.core.daysBetween
import com.monebie.app.core.formatDisplayDate
import com.monebie.app.core.relativeDueLabel
import com.monebie.app.domain.model.EmiStatus
import com.monebie.app.presentation.emi.EmiDetailUiState
import com.monebie.app.presentation.emi.EmiDetailViewModel
import com.monebie.app.presentation.emi.MarkPaidUiState
import com.monebie.app.ui.components.AccountPicker
import com.monebie.app.ui.components.AmbientBackdrop
import com.monebie.app.ui.components.MoneBieButton
import com.monebie.app.ui.components.MoneBieDateField
import com.monebie.app.ui.components.MoneBieTextField
import com.monebie.app.ui.components.CardDivider
import com.monebie.app.ui.components.ConfirmDialog
import com.monebie.app.ui.components.CountUpMoneyText
import com.monebie.app.ui.components.DetailTopBar
import com.monebie.app.ui.components.EmptyState
import com.monebie.app.ui.components.FilterPill
import com.monebie.app.ui.components.FormLabel
import com.monebie.app.ui.components.GlassIconButton
import com.monebie.app.ui.components.GlowBadge
import com.monebie.app.ui.components.HeroCardShape
import com.monebie.app.ui.components.HeroDecoration
import com.monebie.app.ui.components.HeroFinance
import com.monebie.app.ui.components.HeroNote
import com.monebie.app.ui.components.HeroPill
import com.monebie.app.ui.components.HeroTile
import com.monebie.app.ui.components.MicroLabel
import com.monebie.app.ui.components.MoneyText
import com.monebie.app.ui.components.OnHero
import com.monebie.app.ui.components.PremiumCard
import com.monebie.app.ui.components.PremiumSectionHeader
import com.monebie.app.ui.components.heroBrush
import com.monebie.app.ui.theme.MoneBieTheme
import com.monebie.app.ui.theme.Palette300
import com.monebie.app.ui.theme.Palette700
import com.monebie.app.ui.theme.Palette900

/**
 * One EMI: where the schedule stands, and every installment paid so far.
 *
 * The hero draws the whole term as installments, so the count at the top is the
 * same fact as the rows below it - the schedule and the record cannot disagree,
 * because they are the same numbers seen twice.
 *
 * The bar carrying back, edit and delete is pinned; everything else scrolls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmiDetailScreen(
    emiId: Long,
    onBack: () -> Unit,
    onEditEmi: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneBieApplication

    val viewModel: EmiDetailViewModel = viewModel(
        key = "emi_detail_$emiId",
        factory = viewModelFactory {
            initializer {
                EmiDetailViewModel(
                    emiId = emiId,
                    getEmiUseCase = app.appContainer.getEmiUseCase,
                    getEmiPaymentsUseCase = app.appContainer.getEmiPaymentsUseCase,
                    getAccountsUseCase = app.appContainer.getAccountsUseCase,
                    getSelectedCurrencySymbolUseCase = app.appContainer.getSelectedCurrencySymbolUseCase,
                    markInstallmentPaidUseCase = app.appContainer.markInstallmentPaidUseCase,
                    undoLastInstallmentUseCase = app.appContainer.undoLastInstallmentUseCase,
                    deleteEmiUseCase = app.appContainer.deleteEmiUseCase
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val markPaidForm by viewModel.markPaidForm.collectAsStateWithLifecycle()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showUndoConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBack()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                title = uiState.emi?.name ?: "EMI",
                onBack = onBack,
                subtitle = uiState.emi?.provider
            ) {
                if (uiState.emi != null) {
                    GlassIconButton(
                        icon = Icons.Rounded.Edit,
                        contentDescription = "Edit EMI",
                        onClick = { onEditEmi(emiId) }
                    )
                    GlassIconButton(
                        icon = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete EMI",
                        onClick = { showDeleteConfirm = true },
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            val emi = uiState.emi

            if (emi == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MoneBieTheme.spacing.medium),
                    contentAlignment = Alignment.Center
                ) {
                    if (!uiState.isLoading) {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                icon = Icons.Rounded.EventRepeat,
                                title = "EMI not found",
                                message = "It may have been deleted."
                            )
                        }
                    }
                }
                return@Column
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
                item(key = "hero") { EmiDetailHero(uiState = uiState) }

                item(key = "next") {
                    NextInstallmentCard(
                        uiState = uiState,
                        onMarkPaid = viewModel::openMarkPaidSheet
                    )
                }

                item(key = "terms_header") {
                    PremiumSectionHeader(title = "Loan terms")
                }

                item(key = "terms") { EmiTermsCard(uiState) }

                item(key = "history_header") {
                    PremiumSectionHeader(
                        title = "Installment history",
                        // Only the newest installment can be undone; a gap in the
                        // middle would leave the schedule pointing at a settled month.
                        actionLabel = if (uiState.payments.isNotEmpty()) "Undo last" else null,
                        onActionClick = if (uiState.payments.isNotEmpty()) {
                            { showUndoConfirm = true }
                        } else {
                            null
                        }
                    )
                }

                if (uiState.payments.isEmpty()) {
                    item(key = "history_empty") {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                icon = Icons.Rounded.EventRepeat,
                                title = "No installments paid yet",
                                message = "Mark one paid and it will appear here, linked to the " +
                                    "account it came from."
                            )
                        }
                    }
                } else {
                    item(key = "history_list") {
                        PremiumCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = MoneBieTheme.spacing.medium
                        ) {
                            uiState.payments.forEachIndexed { index, payment ->
                                if (index > 0) CardDivider(alpha = 0.07f)
                                InstallmentHistoryRow(
                                    installmentNumber = payment.installmentNumber,
                                    totalInstallments = emi.totalInstallments,
                                    amount = MoneyFormatter.format(
                                        payment.amount,
                                        uiState.currencySymbol,
                                        showDecimals = false
                                    ),
                                    paidDay = payment.paidDate,
                                    dueDay = payment.dueDate,
                                    accountName = uiState.accountName(payment.accountId),
                                    isLinkedToAccount = payment.transactionId != null,
                                    isLate = daysBetween(payment.dueDate, payment.paidDate) > 0L
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (markPaidForm.isVisible) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissMarkPaidSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            MarkPaidSheet(
                form = markPaidForm,
                uiState = uiState,
                onAmountChange = viewModel::setAmount,
                onDayChange = viewModel::setPaidDay,
                onAccountChange = viewModel::setAccount,
                onSave = viewModel::markPaid
            )
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete this EMI?",
            message = "Its installment records will be removed. The transactions those payments " +
                "created stay in your history, because that money really did leave your account.",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteEmi()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showUndoConfirm) {
        ConfirmDialog(
            title = "Undo the last installment?",
            message = "The schedule steps back one month, and the amount returns to the account it was paid from.",
            confirmLabel = "Undo",
            isDestructive = true,
            onConfirm = {
                showUndoConfirm = false
                viewModel.undoLastInstallment()
            },
            onDismiss = { showUndoConfirm = false }
        )
    }
}

// -----------------------------------------------------------------------------
// The hero
// -----------------------------------------------------------------------------

/**
 * Where this schedule stands.
 *
 * The strip is the whole term at actual length - paid months behind, the one
 * that comes next outlined, the rest waiting - which is the thing a single
 * percentage can never say.
 */
@Composable
private fun EmiDetailHero(
    uiState: EmiDetailUiState,
    modifier: Modifier = Modifier
) {
    val emi = uiState.emi ?: return
    val completed = uiState.status == EmiStatus.COMPLETED
    val overdue = uiState.status == EmiStatus.OVERDUE

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
                MicroLabel(text = "Still payable", color = OnHero.copy(alpha = 0.75f))
                Spacer(Modifier.weight(1f))
                HeroPill(
                    text = when {
                        completed -> "Completed"
                        overdue -> "Overdue"
                        else -> "${emi.remainingInstallments} to go"
                    },
                    accent = when {
                        completed -> HeroFinance.positive
                        overdue -> HeroFinance.negative
                        else -> null
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            CountUpMoneyText(
                money = emi.remainingAmount,
                currencySymbol = uiState.currencySymbol,
                style = MaterialTheme.typography.displaySmall,
                color = OnHero
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "of " + MoneyFormatter.format(
                    emi.totalPayableAmount,
                    uiState.currencySymbol,
                    showDecimals = false
                ) + " total payable",
                style = MaterialTheme.typography.bodySmall,
                color = OnHero.copy(alpha = 0.75f)
            )

            Spacer(Modifier.height(18.dp))

            InstallmentStrip(
                paid = emi.installmentsPaid,
                total = emi.totalInstallments,
                accent = if (completed) HeroFinance.positive else Palette300,
                trackColor = Color.White.copy(alpha = 0.18f)
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${emi.installmentsPaid} of ${emi.totalInstallments} installments paid",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnHero.copy(alpha = 0.75f)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = MoneyFormatter.percent(emi.progress),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnHero
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile(
                    label = "Monthly",
                    value = MoneyFormatter.compact(emi.monthlyAmount, uiState.currencySymbol),
                    valueColor = OnHero,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Paid so far",
                    value = MoneyFormatter.compact(emi.amountPaid, uiState.currencySymbol),
                    valueColor = HeroFinance.positive,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            val nextDue = emi.nextDueDate
            if (nextDue == null) {
                HeroNote(
                    icon = Icons.Rounded.TaskAlt,
                    text = "Finished on " + formatDisplayDate(emi.endDate),
                    accent = HeroFinance.positive
                )
            } else {
                HeroNote(
                    icon = if (overdue) Icons.Rounded.PriorityHigh else Icons.Rounded.EventAvailable,
                    text = "Next on " + formatDisplayDate(nextDue) +
                        " · " + relativeDueLabel(uiState.daysUntilDue ?: 0L),
                    accent = if (overdue) HeroFinance.negative else null
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// The action
// -----------------------------------------------------------------------------

/**
 * The installment that comes next, and the one button this screen is for.
 *
 * The number and its due date sit above the action rather than inside its
 * label, because "mark paid" is a commitment to a specific month and the user
 * should see which one before pressing it.
 */
@Composable
private fun NextInstallmentCard(
    uiState: EmiDetailUiState,
    onMarkPaid: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emi = uiState.emi ?: return
    val (accent, _) = emiStatusColors(uiState.status)
    val next = emi.nextInstallmentNumber

    PremiumCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.medium)
        ) {
            GlowBadge(
                icon = if (next == null) Icons.Rounded.TaskAlt else Icons.Rounded.EventAvailable,
                tint = if (next == null) MoneBieTheme.finance.positive else accent
            )

            Column(modifier = Modifier.weight(1f)) {
                MicroLabel(
                    text = if (next == null) "Schedule complete" else "Next installment"
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (next == null) {
                        "Every installment is paid"
                    } else {
                        "#$next of ${emi.totalInstallments} · due " +
                            formatDisplayDate(emi.nextDueDate.orEmpty())
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (next != null) {
                MoneyText(
                    money = emi.nextInstallmentAmount,
                    currencySymbol = uiState.currencySymbol,
                    style = MaterialTheme.typography.titleMedium,
                    showDecimals = false
                )
            }
        }

        if (next != null) {
            Spacer(Modifier.height(MoneBieTheme.spacing.medium))
            MoneBieButton(
                text = "Mark installment $next paid",
                onClick = onMarkPaid
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Terms
// -----------------------------------------------------------------------------

@Composable
private fun EmiTermsCard(uiState: EmiDetailUiState, modifier: Modifier = Modifier) {
    val emi = uiState.emi ?: return

    PremiumCard(modifier = modifier.fillMaxWidth()) {
        TermRow(label = "Provider", value = emi.provider)
        TermRow(
            label = "Principal",
            value = MoneyFormatter.format(emi.principalAmount, uiState.currencySymbol)
        )
        TermRow(
            label = "Total payable",
            value = MoneyFormatter.format(emi.totalPayableAmount, uiState.currencySymbol)
        )
        TermRow(
            label = "Interest cost",
            value = MoneyFormatter.format(emi.totalInterest, uiState.currencySymbol),
            valueColor = MoneBieTheme.finance.caution
        )
        emi.interestRate?.let { TermRow(label = "Interest rate", value = "$it%") }
        TermRow(label = "Started", value = formatDisplayDate(emi.startDate))
        // Both derived from start date and tenure, never stored.
        TermRow(label = "Ends", value = formatDisplayDate(emi.endDate))
        TermRow(
            label = "Tenure",
            value = "${emi.tenureYears} years (${emi.totalInstallments} installments)"
        )
        emi.paymentAccountId?.let {
            TermRow(label = "Paid from", value = uiState.accountName(it))
        }
        if (!emi.notes.isNullOrBlank()) {
            TermRow(label = "Notes", value = emi.notes!!)
        }
    }
}

@Composable
private fun TermRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = MoneBieTheme.spacing.medium)
        )
    }
}

// -----------------------------------------------------------------------------
// Mark paid
// -----------------------------------------------------------------------------

@Composable
private fun MarkPaidSheet(
    form: MarkPaidUiState,
    uiState: EmiDetailUiState,
    onAmountChange: (String) -> Unit,
    onDayChange: (String) -> Unit,
    onAccountChange: (Long?) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MoneBieTheme.spacing.medium)
            .imePadding()
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlowBadge(
                icon = Icons.Rounded.EventAvailable,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(MoneBieTheme.spacing.medium))
            Column {
                Text(
                    text = "Installment ${form.installmentNumber} of ${form.totalInstallments}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                form.dueDate?.let {
                    Spacer(Modifier.height(2.dp))
                    MicroLabel(text = "Scheduled for " + formatDisplayDate(it))
                }
            }
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.large))

        FormLabel("Amount")
        Spacer(Modifier.height(MoneBieTheme.spacing.small))
        MoneBieTextField(
            value = form.amountInput,
            onValueChange = onAmountChange,
            placeholder = "0.00",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = {
                Text(
                    text = uiState.currencySymbol,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        form.amountError?.let {
            Spacer(Modifier.height(MoneBieTheme.spacing.small))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.large))

        FormLabel("Paid on")
        Spacer(Modifier.height(MoneBieTheme.spacing.small))
        MoneBieDateField(
            day = form.paidDay,
            onDayChange = { day -> day?.let(onDayChange) }
        )

        Spacer(Modifier.height(MoneBieTheme.spacing.large))

        FormLabel("Paid from")
        Spacer(Modifier.height(MoneBieTheme.spacing.small))
        AccountPicker(
            accounts = uiState.accounts,
            selectedId = form.accountId,
            currencySymbol = uiState.currencySymbol,
            onSelect = { onAccountChange(it) },
            emptyMessage = "No accounts - this will be recorded as history only"
        )

        if (uiState.accounts.isNotEmpty()) {
            Spacer(Modifier.height(MoneBieTheme.spacing.small))
            FilterPill(
                label = "Not from an account",
                selected = form.accountId == null,
                onClick = { onAccountChange(null) }
            )
        }

        Spacer(Modifier.height(MoneBieTheme.spacing.extraLarge))

        MoneBieButton(
            text = "Mark as paid",
            loading = form.isSaving,
            onClick = onSave
        )

        Spacer(Modifier.height(MoneBieTheme.spacing.extraExtraLarge))
    }
}
