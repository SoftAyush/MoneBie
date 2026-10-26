package com.monebie.app.ui.screens.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.monebie.app.MoneBieApplication
import com.monebie.app.core.formatDisplayDate
import com.monebie.app.domain.model.TransactionType
import com.monebie.app.domain.model.displayName
import com.monebie.app.presentation.recurring.AddEditRecurringViewModel
import com.monebie.app.ui.components.AccountPicker
import com.monebie.app.ui.components.AmbientBackdrop
import com.monebie.app.ui.components.MoneBieButton
import com.monebie.app.ui.components.MoneBieDateField
import com.monebie.app.ui.components.MoneBieTextField
import com.monebie.app.ui.components.CategoryPicker
import com.monebie.app.ui.components.DetailTopBar
import com.monebie.app.ui.components.EmptyState
import com.monebie.app.ui.components.FieldError
import com.monebie.app.ui.components.FormField
import com.monebie.app.ui.components.FormLabel
import com.monebie.app.ui.components.FormNotice
import com.monebie.app.ui.components.FormSection
import com.monebie.app.ui.components.MoneyInputField
import com.monebie.app.ui.components.PremiumCard
import com.monebie.app.ui.components.Segment
import com.monebie.app.ui.components.SegmentedSelector
import com.monebie.app.ui.components.SelectableChip
import com.monebie.app.ui.theme.MoneBieTheme

/**
 * The recurring transaction form.
 *
 * Shaped like the transaction form on purpose - the fields are the same fields,
 * plus a name, a cycle and an optional end - so what was learned there carries
 * straight over.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditRecurringScreen(
    onBack: () -> Unit,
    onOpenAccounts: () -> Unit,
    modifier: Modifier = Modifier,
    recurringId: Long? = null,
    presetType: TransactionType = TransactionType.EXPENSE
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneBieApplication

    val viewModel: AddEditRecurringViewModel = viewModel(
        key = "recurring_$recurringId",
        factory = viewModelFactory {
            initializer {
                AddEditRecurringViewModel(
                    editingId = recurringId,
                    presetType = presetType,
                    getAccountsUseCase = app.appContainer.getAccountsUseCase,
                    getActiveCategoriesUseCase = app.appContainer.getActiveCategoriesUseCase,
                    getSelectedCurrencySymbolUseCase = app.appContainer.getSelectedCurrencySymbolUseCase,
                    getRecurringTransactionUseCase = app.appContainer.getRecurringTransactionUseCase,
                    saveRecurringTransactionUseCase = app.appContainer.saveRecurringTransactionUseCase
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onBack()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                title = if (uiState.isEditing) "Edit schedule" else "New recurring",
                onBack = onBack,
                subtitle = if (uiState.isEditing) uiState.name.ifBlank { null } else "Repeats on its own"
            )

            if (uiState.hasNoAccounts) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MoneBieTheme.spacing.medium),
                    contentAlignment = Alignment.Center
                ) {
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Rounded.AccountBalanceWallet,
                            title = "No accounts yet",
                            message = "A schedule needs an account to post to.",
                            actionLabel = "Add an account",
                            onAction = onOpenAccounts
                        )
                    }
                }
                return@Column
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MoneBieTheme.spacing.medium)
                    .imePadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.large)
            ) {
                Spacer(Modifier.height(MoneBieTheme.spacing.extraSmall))

                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    SegmentedSelector(
                        segments = listOf(
                            Segment(TransactionType.INCOME, "Income", MoneBieTheme.finance.positive),
                            Segment(TransactionType.EXPENSE, "Expense", MoneBieTheme.finance.negative),
                            Segment(TransactionType.TRANSFER, "Transfer", MaterialTheme.colorScheme.primary)
                        ),
                        selected = uiState.type,
                        onSelect = viewModel::setType
                    )

                    Spacer(Modifier.height(MoneBieTheme.spacing.large))

                    FormLabel(text = "Name")

                    Spacer(Modifier.height(MoneBieTheme.spacing.small))

                    MoneBieTextField(
                        value = uiState.name,
                        onValueChange = viewModel::setName,
                        placeholder = when (uiState.type) {
                            TransactionType.INCOME -> "Salary"
                            TransactionType.EXPENSE -> "Rent"
                            TransactionType.TRANSFER -> "Savings top-up"
                        },
                        leadingIcon = Icons.AutoMirrored.Rounded.Label,
                        textStyle = MaterialTheme.typography.titleMedium
                    )

                    FieldError(uiState.nameError)

                    Spacer(Modifier.height(MoneBieTheme.spacing.medium))

                    FormLabel(text = "Amount")

                    Spacer(Modifier.height(MoneBieTheme.spacing.small))

                    MoneyInputField(
                        value = uiState.amountInput,
                        currencySymbol = uiState.currencySymbol,
                        onValueChange = viewModel::setAmount,
                        textStyle = MaterialTheme.typography.headlineMedium
                    )

                    FieldError(uiState.amountError)
                }

                FormSection(title = "Repeats") {
                    FormField(label = "Every") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small),
                            verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small)
                        ) {
                            uiState.frequencies.forEach { frequency ->
                                SelectableChip(
                                    label = frequency.displayName,
                                    selected = uiState.frequency == frequency,
                                    onClick = { viewModel.setFrequency(frequency) }
                                )
                            }
                        }
                    }

                    FormField(label = if (uiState.isEditing) "Started" else "First on") {
                        MoneBieDateField(
                            day = uiState.startDate,
                            onDayChange = { day -> day?.let(viewModel::setStartDate) }
                        )
                    }

                    FormField(label = "Ends (optional)", error = uiState.dateError) {
                        MoneBieDateField(
                            day = uiState.endDate,
                            onDayChange = viewModel::setEndDate,
                            placeholder = "Never",
                            clearable = true
                        )
                    }

                    if (!uiState.isEditing) {
                        FormNotice(
                            icon = Icons.Rounded.EventRepeat,
                            text = if (uiState.startDate.isNotEmpty()) {
                                "First entry posts on " + formatDisplayDate(uiState.startDate) +
                                    ", then " + uiState.frequency.displayName.lowercase() + "."
                            } else {
                                "Each occurrence is recorded automatically on its day."
                            }
                        )
                    }
                }

                if (!uiState.isTransfer) {
                    FormSection(title = "Category") {
                        FormField(label = "Filed under") {
                            CategoryPicker(
                                categories = uiState.categories,
                                selectedId = uiState.categoryId,
                                onSelect = viewModel::setCategory
                            )
                        }
                    }
                }

                FormSection(title = if (uiState.isTransfer) "Accounts" else "Account") {
                    FormField(
                        label = if (uiState.isTransfer) "From account" else "Account",
                        error = uiState.accountError
                    ) {
                        AccountPicker(
                            accounts = uiState.accounts,
                            selectedId = uiState.accountId,
                            currencySymbol = uiState.currencySymbol,
                            onSelect = viewModel::setAccount
                        )
                    }

                    if (uiState.isTransfer) {
                        FormField(label = "To account", error = uiState.transferError) {
                            AccountPicker(
                                accounts = uiState.accounts.filter { it.id != uiState.accountId },
                                selectedId = uiState.transferAccountId,
                                currencySymbol = uiState.currencySymbol,
                                onSelect = viewModel::setTransferAccount
                            )
                        }
                    }
                }

                FormSection(title = "Details") {
                    FormField(label = "Note (optional)") {
                        MoneBieTextField(
                            value = uiState.note,
                            onValueChange = viewModel::setNote,
                            placeholder = "Shown on each entry",
                            leadingIcon = Icons.Rounded.EditNote,
                            textStyle = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                if (uiState.isEditing) {
                    Text(
                        text = "Entries already recorded keep the amount they were recorded at.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                MoneBieButton(
                    text = if (uiState.isEditing) "Save changes" else "Start schedule",
                    loading = uiState.isSaving,
                    onClick = viewModel::save
                )

                Spacer(Modifier.height(MoneBieTheme.spacing.extraExtraLarge))
            }
        }
    }
}
