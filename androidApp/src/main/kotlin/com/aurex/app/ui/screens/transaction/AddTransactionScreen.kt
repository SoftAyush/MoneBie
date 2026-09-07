package com.aurex.app.ui.screens.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurex.app.AurexApplication
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.presentation.transaction.AddTransactionViewModel
import com.aurex.app.ui.components.AccountPicker
import com.aurex.app.ui.components.AmbientBackdrop
import com.aurex.app.ui.components.AurexButton
import com.aurex.app.ui.components.AurexDateField
import com.aurex.app.ui.components.AurexTextField
import com.aurex.app.ui.components.CategoryPicker
import com.aurex.app.ui.components.ConfirmDialog
import com.aurex.app.ui.components.DetailTopBar
import com.aurex.app.ui.components.EmptyState
import com.aurex.app.ui.components.FieldError
import com.aurex.app.ui.components.FormField
import com.aurex.app.ui.components.FormLabel
import com.aurex.app.ui.components.FormSection
import com.aurex.app.ui.components.GlassIconButton
import com.aurex.app.ui.components.MoneyInputField
import com.aurex.app.ui.components.PremiumCard
import com.aurex.app.ui.components.Segment
import com.aurex.app.ui.components.SegmentedSelector
import com.aurex.app.ui.theme.AurexTheme

/**
 * Fast entry for income, expenses and transfers.
 *
 * The order of the form is the order of a real entry: type, then amount, then
 * category. Those first two share one card at the top, because they are the
 * only two things a user always has to think about - everything below them
 * arrives with a sensible default already filled in.
 */
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    onOpenAccounts: () -> Unit,
    modifier: Modifier = Modifier,
    transactionId: Long? = null,
    presetType: TransactionType = TransactionType.EXPENSE
) {
    val context = LocalContext.current
    val app = context.applicationContext as AurexApplication

    val viewModel: AddTransactionViewModel = viewModel(
        // Keyed so opening a different transaction builds a fresh form rather
        // than reusing the previous one's loaded state.
        key = "add_transaction_" + (transactionId ?: -1L) + "_" + presetType.name,
        factory = viewModelFactory {
            initializer {
                AddTransactionViewModel(
                    getAccountsUseCase = app.appContainer.getAccountsUseCase,
                    getActiveCategoriesUseCase = app.appContainer.getActiveCategoriesUseCase,
                    getSelectedCurrencySymbolUseCase = app.appContainer.getSelectedCurrencySymbolUseCase,
                    getTransactionUseCase = app.appContainer.getTransactionUseCase,
                    getTransactionDefaultsUseCase = app.appContainer.getTransactionDefaultsUseCase,
                    saveTransactionUseCase = app.appContainer.saveTransactionUseCase,
                    deleteTransactionUseCase = app.appContainer.deleteTransactionUseCase,
                    editingTransactionId = transactionId,
                    presetType = presetType
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showDeleteConfirm by remember { mutableStateOf(false) }

    // The ViewModel owns "done"; the screen only reacts to it.
    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onBack()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                title = if (uiState.isEditing) "Edit transaction" else "Add transaction",
                onBack = onBack,
                subtitle = when (uiState.type) {
                    TransactionType.INCOME -> "Money in"
                    TransactionType.EXPENSE -> "Money out"
                    TransactionType.TRANSFER -> "Between accounts"
                }
            ) {
                if (uiState.isEditing) {
                    GlassIconButton(
                        icon = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete transaction",
                        onClick = { showDeleteConfirm = true },
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (uiState.hasNoAccounts) {
                // Nothing can be recorded without somewhere to record it against.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AurexTheme.spacing.medium),
                    contentAlignment = Alignment.Center
                ) {
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Rounded.AccountBalanceWallet,
                            title = "No accounts yet",
                            message = "Every transaction has to move money somewhere, so " +
                                "there is nothing to record against yet.",
                            // Was a dead end before: the screen said to add an
                            // account without offering any way to do it.
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
                    .padding(horizontal = AurexTheme.spacing.medium)
                    .imePadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.large)
            ) {
                Spacer(Modifier.height(AurexTheme.spacing.extraSmall))

                // The entry block: what kind, and how much.
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    SegmentedSelector(
                        segments = listOf(
                            Segment(
                                value = TransactionType.INCOME,
                                label = "Income",
                                accent = AurexTheme.finance.positive
                            ),
                            Segment(
                                value = TransactionType.EXPENSE,
                                label = "Expense",
                                accent = AurexTheme.finance.negative
                            ),
                            Segment(
                                value = TransactionType.TRANSFER,
                                label = "Transfer",
                                accent = MaterialTheme.colorScheme.primary
                            )
                        ),
                        selected = uiState.type,
                        onSelect = viewModel::setType
                    )

                    Spacer(Modifier.height(AurexTheme.spacing.large))

                    FormLabel(text = "Amount")

                    Spacer(Modifier.height(AurexTheme.spacing.small))

                    MoneyInputField(
                        value = uiState.amountInput,
                        currencySymbol = uiState.currencySymbol,
                        onValueChange = viewModel::setAmount,
                        textStyle = MaterialTheme.typography.headlineMedium
                    )

                    FieldError(uiState.amountError)
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
                    FormField(label = "Date") {
                        AurexDateField(
                            day = uiState.day,
                            onDayChange = { day -> day?.let(viewModel::setDay) }
                        )
                    }

                    FormField(label = "Note (optional)") {
                        AurexTextField(
                            value = uiState.note,
                            onValueChange = viewModel::setNote,
                            placeholder = "What was this for?",
                            leadingIcon = Icons.Rounded.EditNote,
                            textStyle = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                AurexButton(
                    text = if (uiState.isEditing) "Save changes" else "Add transaction",
                    loading = uiState.isSaving,
                    onClick = viewModel::save
                )

                Spacer(Modifier.height(AurexTheme.spacing.extraExtraLarge))
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete this transaction?",
            message = "The amount will be added back to the account it came from. This cannot be undone.",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.delete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
