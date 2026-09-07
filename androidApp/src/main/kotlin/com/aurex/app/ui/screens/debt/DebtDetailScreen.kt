package com.aurex.app.ui.screens.debt

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
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventAvailable
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
import com.aurex.app.AurexApplication
import com.aurex.app.core.Money
import com.aurex.app.core.MoneyFormatter
import com.aurex.app.core.formatDisplayDate
import com.aurex.app.core.relativeDueLabel
import com.aurex.app.domain.model.DebtStatus
import com.aurex.app.domain.model.PaymentMethod
import com.aurex.app.domain.model.displayName
import com.aurex.app.presentation.debt.DebtDetailUiState
import com.aurex.app.presentation.debt.DebtDetailViewModel
import com.aurex.app.presentation.debt.RecordPaymentUiState
import com.aurex.app.ui.components.AccountPicker
import com.aurex.app.ui.components.AmbientBackdrop
import com.aurex.app.ui.components.AurexButton
import com.aurex.app.ui.components.AurexDateField
import com.aurex.app.ui.components.AurexTextField
import com.aurex.app.ui.components.CardDivider
import com.aurex.app.ui.components.ConfirmDialog
import com.aurex.app.ui.components.CountUpMoneyText
import com.aurex.app.ui.components.DetailTopBar
import com.aurex.app.ui.components.EmptyState
import com.aurex.app.ui.components.FilterPill
import com.aurex.app.ui.components.FormLabel
import com.aurex.app.ui.components.GlassIconButton
import com.aurex.app.ui.components.GlowBadge
import com.aurex.app.ui.components.HeroCardShape
import com.aurex.app.ui.components.HeroDecoration
import com.aurex.app.ui.components.HeroFinance
import com.aurex.app.ui.components.HeroNote
import com.aurex.app.ui.components.HeroPill
import com.aurex.app.ui.components.HeroTile
import com.aurex.app.ui.components.HeroTrack
import com.aurex.app.ui.components.MicroLabel
import com.aurex.app.ui.components.MoneyText
import com.aurex.app.ui.components.OnHero
import com.aurex.app.ui.components.PremiumCard
import com.aurex.app.ui.components.PremiumSectionHeader
import com.aurex.app.ui.components.heroBrush
import com.aurex.app.ui.theme.AurexTheme
import com.aurex.app.ui.theme.Palette300
import com.aurex.app.ui.theme.Palette500
import com.aurex.app.ui.theme.Palette700
import com.aurex.app.ui.theme.Palette900

/**
 * One debt: where it stands, and everything that has been paid against it.
 *
 * The payment history is the point of this screen. The remaining balance shown
 * at the top is derived from exactly the rows listed below it, so the two can
 * never tell different stories.
 *
 * The bar carrying back, edit and delete is pinned; everything else scrolls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailScreen(
    debtId: Long,
    onBack: () -> Unit,
    onEditDebt: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AurexApplication

    val viewModel: DebtDetailViewModel = viewModel(
        key = "debt_detail_$debtId",
        factory = viewModelFactory {
            initializer {
                DebtDetailViewModel(
                    debtId = debtId,
                    getDebtUseCase = app.appContainer.getDebtUseCase,
                    getDebtPaymentsUseCase = app.appContainer.getDebtPaymentsUseCase,
                    getAccountsUseCase = app.appContainer.getAccountsUseCase,
                    getSelectedCurrencySymbolUseCase = app.appContainer.getSelectedCurrencySymbolUseCase,
                    recordDebtPaymentUseCase = app.appContainer.recordDebtPaymentUseCase,
                    deleteDebtPaymentUseCase = app.appContainer.deleteDebtPaymentUseCase,
                    deleteDebtUseCase = app.appContainer.deleteDebtUseCase
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val paymentForm by viewModel.paymentForm.collectAsStateWithLifecycle()

    var showDeleteDebtConfirm by remember { mutableStateOf(false) }
    var pendingPaymentDeleteId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBack()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                title = uiState.debt?.name ?: "Debt",
                onBack = onBack,
                subtitle = uiState.debt?.lender
            ) {
                if (uiState.debt != null) {
                    GlassIconButton(
                        icon = Icons.Rounded.Edit,
                        contentDescription = "Edit debt",
                        onClick = { onEditDebt(debtId) }
                    )
                    GlassIconButton(
                        icon = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete debt",
                        onClick = { showDeleteDebtConfirm = true },
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            val debt = uiState.debt

            if (debt == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AurexTheme.spacing.medium),
                    contentAlignment = Alignment.Center
                ) {
                    if (!uiState.isLoading) {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                                title = "Debt not found",
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
                    start = AurexTheme.spacing.medium,
                    end = AurexTheme.spacing.medium,
                    top = AurexTheme.spacing.small,
                    bottom = AurexTheme.spacing.extraExtraLarge
                ),
                verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
            ) {
                item(key = "hero") { DebtDetailHero(uiState = uiState) }

                item(key = "record") {
                    NextPaymentCard(
                        uiState = uiState,
                        onRecordPayment = viewModel::openPaymentSheet
                    )
                }

                item(key = "terms_header") {
                    PremiumSectionHeader(title = "Debt terms")
                }

                item(key = "terms") { DebtTermsCard(uiState) }

                item(key = "history_header") {
                    PremiumSectionHeader(title = "Payment history")
                }

                if (uiState.payments.isEmpty()) {
                    item(key = "history_empty") {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                                title = "No payments yet",
                                message = "Record a payment and it will appear here, linked to " +
                                    "the account it came from."
                            )
                        }
                    }
                } else {
                    item(key = "history_list") {
                        PremiumCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = AurexTheme.spacing.medium
                        ) {
                            uiState.payments.forEachIndexed { index, payment ->
                                if (index > 0) CardDivider(alpha = 0.07f)
                                PaymentHistoryRow(
                                    amount = MoneyFormatter.format(
                                        payment.amount,
                                        uiState.currencySymbol,
                                        showDecimals = false
                                    ),
                                    day = payment.date,
                                    accountName = uiState.accountName(payment.accountId),
                                    note = payment.note,
                                    isLinkedToAccount = payment.transactionId != null,
                                    onDelete = { pendingPaymentDeleteId = payment.id }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (paymentForm.isVisible) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissPaymentSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            RecordPaymentSheet(
                form = paymentForm,
                uiState = uiState,
                onAmountChange = viewModel::setPaymentAmount,
                onDayChange = viewModel::setPaymentDay,
                onAccountChange = viewModel::setPaymentAccount,
                onMethodChange = viewModel::setPaymentMethod,
                onNoteChange = viewModel::setPaymentNote,
                onSave = viewModel::recordPayment
            )
        }
    }

    if (showDeleteDebtConfirm) {
        ConfirmDialog(
            title = "Delete this debt?",
            message = "Its payment records will be removed. The transactions those payments created stay " +
                "in your history, because that money really did leave your account.",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                showDeleteDebtConfirm = false
                viewModel.deleteDebt()
            },
            onDismiss = { showDeleteDebtConfirm = false }
        )
    }

    pendingPaymentDeleteId?.let { paymentId ->
        ConfirmDialog(
            title = "Remove this payment?",
            message = "The amount goes back onto the debt, and back into the account it was paid from.",
            confirmLabel = "Remove",
            isDestructive = true,
            onConfirm = {
                viewModel.deletePayment(paymentId)
                pendingPaymentDeleteId = null
            },
            onDismiss = { pendingPaymentDeleteId = null }
        )
    }
}

// -----------------------------------------------------------------------------
// The hero
// -----------------------------------------------------------------------------

/**
 * Where this debt stands.
 *
 * The remaining balance leads and the bar under it carries the repayment, so
 * the two facts a user opens this screen for - how much is left, how far they
 * have come - are the first two things on it.
 */
@Composable
private fun DebtDetailHero(
    uiState: DebtDetailUiState,
    modifier: Modifier = Modifier
) {
    val debt = uiState.debt ?: return
    val settled = uiState.status == DebtStatus.PAID
    val overdue = uiState.status == DebtStatus.OVERDUE

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
                HeroPill(
                    text = uiState.status.displayName,
                    accent = when {
                        settled -> HeroFinance.positive
                        overdue -> HeroFinance.negative
                        else -> null
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            CountUpMoneyText(
                money = debt.remainingAmount,
                currencySymbol = uiState.currencySymbol,
                style = MaterialTheme.typography.displaySmall,
                color = OnHero
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "of " + MoneyFormatter.format(
                    debt.originalAmount,
                    uiState.currencySymbol,
                    showDecimals = false
                ) + " borrowed",
                style = MaterialTheme.typography.bodySmall,
                color = OnHero.copy(alpha = 0.75f)
            )

            Spacer(Modifier.height(18.dp))

            HeroTrack(
                progress = debt.progress,
                // Clearing the last of it turns the bar green: this is the one
                // place the finish line is visible, and it should look like one.
                colors = if (settled) {
                    listOf(HeroFinance.positive.copy(alpha = 0.75f), HeroFinance.positive)
                } else {
                    listOf(Palette300, Palette500)
                }
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = MoneyFormatter.compact(debt.amountPaid, uiState.currencySymbol) +
                        " repaid so far",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnHero.copy(alpha = 0.75f)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = MoneyFormatter.percent(debt.progress),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnHero
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile(
                    label = "Repaid",
                    value = MoneyFormatter.compact(debt.amountPaid, uiState.currencySymbol),
                    valueColor = HeroFinance.positive,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = if (settled) "Payments" else "Next payment",
                    value = if (settled) {
                        uiState.payments.size.toString()
                    } else {
                        MoneyFormatter.compact(debt.nextPaymentAmount, uiState.currencySymbol)
                    },
                    valueColor = OnHero,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            val nextDue = uiState.nextDueDate
            when {
                settled -> HeroNote(
                    icon = Icons.Rounded.TaskAlt,
                    text = "Settled - nothing left to pay",
                    accent = HeroFinance.positive
                )
                nextDue != null -> HeroNote(
                    icon = if (overdue) Icons.Rounded.PriorityHigh else Icons.Rounded.EventAvailable,
                    text = "Next on " + formatDisplayDate(nextDue) +
                        " · " + relativeDueLabel(uiState.daysUntilDue ?: 0L),
                    accent = if (overdue) HeroFinance.negative else null
                )
                // A debt with no schedule is normal here - money owed to a
                // person often has no date on it - so it says so plainly
                // instead of leaving the row empty.
                else -> HeroNote(
                    icon = Icons.Rounded.EventAvailable,
                    text = "No due date set - pay it down whenever you can",
                    accent = null
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// The action
// -----------------------------------------------------------------------------

/**
 * What comes next, and the one button this screen is for.
 *
 * The expected amount and its date sit above the action rather than inside its
 * label: a payment is a decision about a number, and the number should be
 * visible before the button is pressed.
 */
@Composable
private fun NextPaymentCard(
    uiState: DebtDetailUiState,
    onRecordPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val debt = uiState.debt ?: return
    val (accent, _) = statusColors(uiState.status)
    val settled = uiState.status == DebtStatus.PAID

    PremiumCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)
        ) {
            GlowBadge(
                icon = if (settled) Icons.Rounded.TaskAlt else Icons.Rounded.EventAvailable,
                tint = if (settled) AurexTheme.finance.positive else accent
            )

            Column(modifier = Modifier.weight(1f)) {
                MicroLabel(text = if (settled) "Fully repaid" else "Next payment")
                Spacer(Modifier.height(3.dp))
                Text(
                    text = when {
                        settled -> "Every unit of it is paid back"
                        uiState.nextDueDate != null -> "Due " + formatDisplayDate(uiState.nextDueDate!!) +
                            " · " + debt.paymentFrequency.displayName.lowercase()
                        else -> "No date set - record a payment whenever you make one"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!settled) {
                MoneyText(
                    money = debt.nextPaymentAmount,
                    currencySymbol = uiState.currencySymbol,
                    style = MaterialTheme.typography.titleMedium,
                    showDecimals = false
                )
            }
        }

        if (!settled) {
            Spacer(Modifier.height(AurexTheme.spacing.medium))
            AurexButton(
                text = "Record a payment",
                onClick = onRecordPayment
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Terms
// -----------------------------------------------------------------------------

@Composable
private fun DebtTermsCard(uiState: DebtDetailUiState, modifier: Modifier = Modifier) {
    val debt = uiState.debt ?: return

    PremiumCard(modifier = modifier.fillMaxWidth()) {
        TermRow(label = "Lender", value = debt.lender)
        TermRow(label = "Started", value = formatDisplayDate(debt.startDate))
        debt.dueDate?.let { TermRow(label = "Due", value = formatDisplayDate(it)) }
        TermRow(label = "Frequency", value = debt.paymentFrequency.displayName)
        debt.installmentAmount?.let {
            TermRow(
                label = "Installment",
                value = MoneyFormatter.format(it, uiState.currencySymbol, showDecimals = false)
            )
        }
        debt.interestRate?.let {
            TermRow(
                label = "Interest",
                value = "$it%",
                valueColor = AurexTheme.finance.caution
            )
        }
        debt.defaultAccountId?.let {
            TermRow(label = "Paid from", value = uiState.accountName(it))
        }
        if (!debt.notes.isNullOrBlank()) {
            TermRow(label = "Notes", value = debt.notes!!)
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
            modifier = Modifier.padding(start = AurexTheme.spacing.medium)
        )
    }
}

// -----------------------------------------------------------------------------
// Record a payment
// -----------------------------------------------------------------------------

@Composable
private fun RecordPaymentSheet(
    form: RecordPaymentUiState,
    uiState: DebtDetailUiState,
    onAmountChange: (String) -> Unit,
    onDayChange: (String) -> Unit,
    onAccountChange: (Long?) -> Unit,
    onMethodChange: (PaymentMethod) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AurexTheme.spacing.medium)
            .imePadding()
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlowBadge(
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(AurexTheme.spacing.medium))
            Column {
                Text(
                    text = "Record a payment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                MicroLabel(
                    text = MoneyFormatter.format(
                        uiState.debt?.remainingAmount ?: Money.ZERO,
                        uiState.currencySymbol,
                        showDecimals = false
                    ) + " still owed"
                )
            }
        }

        Spacer(Modifier.height(AurexTheme.spacing.large))

        FormLabel("Amount")
        Spacer(Modifier.height(AurexTheme.spacing.small))
        AurexTextField(
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
            Spacer(Modifier.height(AurexTheme.spacing.small))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(AurexTheme.spacing.large))

        FormLabel("Date")
        Spacer(Modifier.height(AurexTheme.spacing.small))
        AurexDateField(
            day = form.day,
            onDayChange = { day -> day?.let(onDayChange) }
        )

        Spacer(Modifier.height(AurexTheme.spacing.large))

        FormLabel("Paid from")
        Spacer(Modifier.height(AurexTheme.spacing.small))
        AccountPicker(
            accounts = uiState.accounts,
            selectedId = form.accountId,
            currencySymbol = uiState.currencySymbol,
            onSelect = { onAccountChange(it) },
            emptyMessage = "No accounts - this will be recorded as history only"
        )

        if (uiState.accounts.isNotEmpty()) {
            Spacer(Modifier.height(AurexTheme.spacing.small))
            // Lets the user log a payment they made outside the app without it
            // silently moving a balance that was never actually touched.
            FilterPill(
                label = "Not from an account",
                selected = form.accountId == null,
                onClick = { onAccountChange(null) }
            )
        }

        Spacer(Modifier.height(AurexTheme.spacing.large))

        FormLabel("Note (optional)")
        Spacer(Modifier.height(AurexTheme.spacing.small))
        AurexTextField(
            value = form.note,
            onValueChange = onNoteChange,
            placeholder = "Reference or reminder",
            leadingIcon = Icons.Rounded.EditNote,
            textStyle = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(AurexTheme.spacing.extraLarge))

        AurexButton(
            text = "Record payment",
            loading = form.isSaving,
            onClick = onSave
        )

        Spacer(Modifier.height(AurexTheme.spacing.extraExtraLarge))
    }
}
