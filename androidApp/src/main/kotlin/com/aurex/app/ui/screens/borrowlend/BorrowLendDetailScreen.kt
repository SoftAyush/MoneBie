package com.aurex.app.ui.screens.borrowlend

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
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Handshake
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
import com.aurex.app.domain.model.LoanStatus
import com.aurex.app.domain.model.displayName
import com.aurex.app.presentation.borrowlend.BorrowLendDetailViewModel
import com.aurex.app.presentation.borrowlend.LoanDetailUiState
import com.aurex.app.presentation.borrowlend.LoanRepaymentUiState
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
 * One borrowing or lending, with its full repayment history.
 *
 * Every label is written from the user's side of the arrangement, so the
 * direction is unambiguous at a glance rather than something to work out - and
 * the headline figure keeps the direction's own colour, the same one the list
 * used to get here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowLendDetailScreen(
    recordId: Long,
    onBack: () -> Unit,
    onEditRecord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AurexApplication

    val viewModel: BorrowLendDetailViewModel = viewModel(
        key = "loan_detail_$recordId",
        factory = viewModelFactory {
            initializer {
                BorrowLendDetailViewModel(
                    recordId = recordId,
                    getLoanRecordUseCase = app.appContainer.getLoanRecordUseCase,
                    getLoanRepaymentsUseCase = app.appContainer.getLoanRepaymentsUseCase,
                    getAccountsUseCase = app.appContainer.getAccountsUseCase,
                    getSelectedCurrencySymbolUseCase = app.appContainer.getSelectedCurrencySymbolUseCase,
                    recordLoanRepaymentUseCase = app.appContainer.recordLoanRepaymentUseCase,
                    deleteLoanRepaymentUseCase = app.appContainer.deleteLoanRepaymentUseCase,
                    deleteLoanRecordUseCase = app.appContainer.deleteLoanRecordUseCase
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val repaymentForm by viewModel.repaymentForm.collectAsStateWithLifecycle()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingRepaymentDeleteId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBack()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                title = uiState.record?.personName ?: "Record",
                onBack = onBack,
                subtitle = uiState.record?.let {
                    if (uiState.isBorrowed) "You owe them" else "They owe you"
                }
            ) {
                if (uiState.record != null) {
                    GlassIconButton(
                        icon = Icons.Rounded.Edit,
                        contentDescription = "Edit record",
                        onClick = { onEditRecord(recordId) }
                    )
                    GlassIconButton(
                        icon = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete record",
                        onClick = { showDeleteConfirm = true },
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            val record = uiState.record

            if (record == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AurexTheme.spacing.medium),
                    contentAlignment = Alignment.Center
                ) {
                    if (!uiState.isLoading) {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                icon = Icons.Rounded.Handshake,
                                title = "Record not found",
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
                item(key = "hero") { LoanDetailHero(uiState = uiState) }

                item(key = "settle") {
                    SettleCard(
                        uiState = uiState,
                        onRecordRepayment = viewModel::openRepaymentSheet
                    )
                }

                item(key = "terms_header") {
                    PremiumSectionHeader(title = "The arrangement")
                }

                item(key = "terms") { LoanTermsCard(uiState) }

                item(key = "history_header") {
                    PremiumSectionHeader(title = "History")
                }

                if (uiState.repayments.isEmpty()) {
                    item(key = "history_empty") {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                icon = Icons.Rounded.Handshake,
                                title = "Nothing settled yet",
                                message = if (uiState.isBorrowed) {
                                    "Log a repayment and it will appear here, linked to the " +
                                        "account it came from."
                                } else {
                                    "Log money received and it will appear here, linked to the " +
                                        "account it went into."
                                }
                            )
                        }
                    }
                } else {
                    item(key = "history_list") {
                        PremiumCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = AurexTheme.spacing.medium
                        ) {
                            uiState.repayments.forEachIndexed { index, repayment ->
                                if (index > 0) CardDivider(alpha = 0.07f)
                                RepaymentHistoryRow(
                                    amount = MoneyFormatter.format(
                                        repayment.amount,
                                        uiState.currencySymbol,
                                        showDecimals = false
                                    ),
                                    day = repayment.date,
                                    accountName = uiState.accountName(repayment.accountId),
                                    note = repayment.note,
                                    isLinkedToAccount = repayment.transactionId != null,
                                    direction = record.direction,
                                    onDelete = { pendingRepaymentDeleteId = repayment.id }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (repaymentForm.isVisible) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissRepaymentSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            RepaymentSheet(
                form = repaymentForm,
                uiState = uiState,
                onAmountChange = viewModel::setAmount,
                onDayChange = viewModel::setDay,
                onAccountChange = viewModel::setAccount,
                onNoteChange = viewModel::setNote,
                onSave = viewModel::recordRepayment
            )
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete this record?",
            // Unlike a debt, this undoes the money movements too - see
            // BorrowLendRepository.deleteRecord for why the two differ.
            message = "The repayment history goes, and any amounts this app moved between your " +
                "accounts for it are put back - including the original amount.",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteRecord()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    pendingRepaymentDeleteId?.let { repaymentId ->
        ConfirmDialog(
            title = "Remove this entry?",
            message = if (uiState.isBorrowed) {
                "The amount goes back onto what you owe, and back into the account it was paid from."
            } else {
                "The amount goes back onto what you are owed, and out of the account it went into."
            },
            confirmLabel = "Remove",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteRepayment(repaymentId)
                pendingRepaymentDeleteId = null
            },
            onDismiss = { pendingRepaymentDeleteId = null }
        )
    }
}

// -----------------------------------------------------------------------------
// The hero
// -----------------------------------------------------------------------------

/**
 * Where this arrangement stands.
 *
 * The figure keeps the direction's colour rather than the hero's white: on this
 * module colour is the direction signal, and dropping it here would make the
 * one screen about a single person the one screen that does not say which way
 * the money goes.
 */
@Composable
private fun LoanDetailHero(
    uiState: LoanDetailUiState,
    modifier: Modifier = Modifier
) {
    val record = uiState.record ?: return
    val settled = uiState.status == LoanStatus.SETTLED
    val overdue = uiState.status == LoanStatus.OVERDUE
    val borrowed = uiState.isBorrowed

    // The hero is a dark surface in both themes, so the direction colours come
    // from the dark ramp rather than the scheme's.
    val accent = if (borrowed) HeroFinance.negative else HeroFinance.receivable

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
                MicroLabel(
                    text = if (borrowed) "You still owe" else "Still owed to you",
                    color = OnHero.copy(alpha = 0.75f)
                )
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
                money = record.remainingAmount,
                currencySymbol = uiState.currencySymbol,
                style = MaterialTheme.typography.displaySmall,
                color = if (settled) OnHero else accent
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "of " + MoneyFormatter.format(
                    record.principalAmount,
                    uiState.currencySymbol,
                    showDecimals = false
                ) + (if (borrowed) " borrowed on " else " lent on ") +
                    formatDisplayDate(record.date),
                style = MaterialTheme.typography.bodySmall,
                color = OnHero.copy(alpha = 0.75f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(18.dp))

            // A bar at zero says nothing on what is usually a single-payment
            // arrangement, so until something is settled the line says so in
            // words instead.
            if (record.amountSettled.isPositive) {
                HeroTrack(
                    progress = record.progress,
                    colors = if (settled) {
                        listOf(HeroFinance.positive.copy(alpha = 0.75f), HeroFinance.positive)
                    } else {
                        listOf(Palette300, Palette500)
                    }
                )

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = MoneyFormatter.compact(
                            record.amountSettled,
                            uiState.currencySymbol
                        ) + " settled so far",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnHero.copy(alpha = 0.75f)
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = MoneyFormatter.percent(record.progress),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnHero
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile(
                    label = if (borrowed) "Borrowed" else "Lent",
                    value = MoneyFormatter.compact(
                        record.principalAmount,
                        uiState.currencySymbol
                    ),
                    valueColor = OnHero,
                    modifier = Modifier.weight(1f)
                )
                HeroTile(
                    label = "Settled",
                    value = MoneyFormatter.compact(
                        record.amountSettled,
                        uiState.currencySymbol
                    ),
                    valueColor = HeroFinance.positive,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            val days = uiState.daysUntilDue
            when {
                settled -> HeroNote(
                    icon = Icons.Rounded.TaskAlt,
                    text = "Squared up - nothing outstanding either way",
                    accent = HeroFinance.positive
                )
                record.dueDate != null -> HeroNote(
                    icon = if (overdue) Icons.Rounded.PriorityHigh else Icons.Rounded.EventAvailable,
                    text = (if (borrowed) "Due " else "Expected back ") +
                        formatDisplayDate(record.dueDate!!) +
                        " · " + relativeDueLabel(days ?: 0L),
                    accent = if (overdue) HeroFinance.negative else null
                )
                // Money between people often has no date on it, and saying so
                // is better than an empty row that looks like missing data.
                else -> HeroNote(
                    icon = Icons.Rounded.EventAvailable,
                    text = "No date agreed - settle it whenever suits you both",
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
 * What is left to settle, and the one button this screen is for.
 *
 * The direction wording carries through to the action: on a borrowing you pay
 * out, on a lending you take money in, and the button should never leave that
 * ambiguous.
 */
@Composable
private fun SettleCard(
    uiState: LoanDetailUiState,
    onRecordRepayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val record = uiState.record ?: return
    val (accent, _) = directionColors(record.direction)
    val settled = uiState.status == LoanStatus.SETTLED
    val borrowed = uiState.isBorrowed

    PremiumCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)
        ) {
            GlowBadge(
                icon = when {
                    settled -> Icons.Rounded.TaskAlt
                    borrowed -> Icons.AutoMirrored.Rounded.CallMade
                    else -> Icons.AutoMirrored.Rounded.CallReceived
                },
                tint = if (settled) AurexTheme.finance.positive else accent
            )

            Column(modifier = Modifier.weight(1f)) {
                MicroLabel(text = if (settled) "Settled" else "Left to settle")
                Spacer(Modifier.height(3.dp))
                Text(
                    text = when {
                        settled -> "Nothing outstanding with ${record.personName}"
                        borrowed -> "You still owe ${record.personName}"
                        else -> "${record.personName} still owes you"
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
                    money = record.remainingAmount,
                    currencySymbol = uiState.currencySymbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    showDecimals = false
                )
            }
        }

        if (!settled) {
            Spacer(Modifier.height(AurexTheme.spacing.medium))
            AurexButton(
                text = uiState.actionLabel,
                onClick = onRecordRepayment
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Terms
// -----------------------------------------------------------------------------

@Composable
private fun LoanTermsCard(uiState: LoanDetailUiState, modifier: Modifier = Modifier) {
    val record = uiState.record ?: return

    PremiumCard(modifier = modifier.fillMaxWidth()) {
        TermRow(
            label = if (uiState.isBorrowed) "Borrowed on" else "Lent on",
            value = formatDisplayDate(record.date)
        )
        TermRow(
            label = if (uiState.isBorrowed) "Due" else "Expected back",
            value = record.dueDate?.let { formatDisplayDate(it) } ?: "No date agreed"
        )
        TermRow(
            label = "Money moved through",
            // States plainly whether a balance was touched, so a cash-in-hand
            // arrangement is never mistaken for one that hit an account.
            value = if (record.principalTransactionId != null) {
                uiState.accountName(record.accountId)
            } else {
                "No account - recorded only"
            }
        )
        if (!record.notes.isNullOrBlank()) {
            TermRow(label = "Notes", value = record.notes!!)
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
// Settling up
// -----------------------------------------------------------------------------

@Composable
private fun RepaymentSheet(
    form: LoanRepaymentUiState,
    uiState: LoanDetailUiState,
    onAmountChange: (String) -> Unit,
    onDayChange: (String) -> Unit,
    onAccountChange: (Long?) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val borrowed = uiState.isBorrowed

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
                icon = if (borrowed) {
                    Icons.AutoMirrored.Rounded.CallMade
                } else {
                    Icons.AutoMirrored.Rounded.CallReceived
                },
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(AurexTheme.spacing.medium))
            Column {
                Text(
                    text = uiState.actionLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                MicroLabel(
                    text = MoneyFormatter.format(
                        uiState.record?.remainingAmount ?: Money.ZERO,
                        uiState.currencySymbol,
                        showDecimals = false
                    ) + if (borrowed) " still owed" else " still outstanding"
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

        FormLabel(if (borrowed) "Paid from" else "Received into")
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
            FilterPill(
                label = "Not through an account",
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
            placeholder = "Anything worth remembering",
            leadingIcon = Icons.Rounded.EditNote,
            textStyle = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(AurexTheme.spacing.extraLarge))

        AurexButton(
            text = "Save",
            loading = form.isSaving,
            onClick = onSave
        )

        Spacer(Modifier.height(AurexTheme.spacing.extraExtraLarge))
    }
}
