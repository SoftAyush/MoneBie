package com.aurex.app.ui.screens.debt

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurex.app.AurexApplication
import com.aurex.app.domain.model.PaymentFrequency
import com.aurex.app.domain.model.displayName
import com.aurex.app.presentation.debt.AddEditDebtViewModel
import com.aurex.app.ui.components.AccountPicker
import com.aurex.app.ui.components.AmbientBackdrop
import com.aurex.app.ui.components.AurexButton
import com.aurex.app.ui.components.AurexDateField
import com.aurex.app.ui.components.AurexTextField
import com.aurex.app.ui.components.DetailTopBar
import com.aurex.app.ui.components.FilterPill
import com.aurex.app.ui.components.FormField
import com.aurex.app.ui.components.FormSection
import com.aurex.app.ui.components.MoneyInputField
import com.aurex.app.ui.theme.AurexTheme

/**
 * Create or edit a debt.
 *
 * Only the terms of the debt are editable. What has been repaid comes from the
 * payment history and cannot be typed in here, which is what stops the two
 * from disagreeing.
 *
 * The questions are grouped as they are asked: what the debt is, when it runs,
 * and the details that are usually left alone.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditDebtScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    debtId: Long? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as AurexApplication

    val viewModel: AddEditDebtViewModel = viewModel(
        key = "add_edit_debt_" + (debtId ?: -1L),
        factory = viewModelFactory {
            initializer {
                AddEditDebtViewModel(
                    editingDebtId = debtId,
                    getDebtUseCase = app.appContainer.getDebtUseCase,
                    getAccountsUseCase = app.appContainer.getAccountsUseCase,
                    getSelectedCurrencySymbolUseCase = app.appContainer.getSelectedCurrencySymbolUseCase,
                    saveDebtUseCase = app.appContainer.saveDebtUseCase
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
                title = if (uiState.isEditing) "Edit debt" else "Add debt",
                onBack = onBack,
                subtitle = if (uiState.isEditing) "Terms only" else "Something you owe"
            )

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

                FormSection(title = "The debt") {
                    FormField(label = "What is it", error = uiState.nameError) {
                        AurexTextField(
                            value = uiState.name,
                            onValueChange = viewModel::setName,
                            placeholder = "e.g. Car loan",
                            leadingIcon = Icons.Rounded.EditNote,
                            textStyle = MaterialTheme.typography.titleMedium
                        )
                    }

                    FormField(label = "Owed to", error = uiState.lenderError) {
                        AurexTextField(
                            value = uiState.lender,
                            onValueChange = viewModel::setLender,
                            placeholder = "Bank, company or person",
                            leadingIcon = Icons.Rounded.AccountBalance,
                            textStyle = MaterialTheme.typography.titleMedium
                        )
                    }

                    FormField(label = "Total amount borrowed", error = uiState.amountError) {
                        MoneyInputField(
                            value = uiState.originalAmountInput,
                            currencySymbol = uiState.currencySymbol,
                            onValueChange = viewModel::setOriginalAmount
                        )
                    }
                }

                FormSection(title = "How it runs") {
                    FormField(label = "Start date") {
                        AurexDateField(
                            day = uiState.startDate,
                            onDayChange = { day -> day?.let(viewModel::setStartDate) }
                        )
                    }

                    FormField(label = "Due date (optional)") {
                        AurexDateField(
                            day = uiState.dueDate,
                            onDayChange = viewModel::setDueDate,
                            placeholder = "No due date set",
                            clearable = true
                        )
                    }

                    FormField(label = "Payment frequency") {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small),
                            verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
                        ) {
                            PaymentFrequency.entries.forEach { frequency ->
                                FilterPill(
                                    label = frequency.displayName,
                                    selected = frequency == uiState.paymentFrequency,
                                    onClick = { viewModel.setPaymentFrequency(frequency) }
                                )
                            }
                        }
                    }

                    if (uiState.showsInstallmentField) {
                        FormField(
                            label = "Amount per payment (optional)",
                            hint = "Used to pre-fill the payment form and to show what is due next."
                        ) {
                            MoneyInputField(
                                value = uiState.installmentAmountInput,
                                currencySymbol = uiState.currencySymbol,
                                onValueChange = viewModel::setInstallmentAmount
                            )
                        }
                    }
                }

                FormSection(title = "Details") {
                    FormField(label = "Interest rate (optional)") {
                        AurexTextField(
                            value = uiState.interestRateInput,
                            onValueChange = viewModel::setInterestRate,
                            placeholder = "0.0",
                            leadingIcon = Icons.Rounded.Percent,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.titleMedium
                        )
                    }

                    FormField(label = "Usually paid from (optional)") {
                        AccountPicker(
                            accounts = uiState.accounts,
                            selectedId = uiState.defaultAccountId,
                            currencySymbol = uiState.currencySymbol,
                            onSelect = viewModel::setDefaultAccount,
                            emptyMessage = "No accounts yet"
                        )
                    }

                    FormField(label = "Notes (optional)") {
                        AurexTextField(
                            value = uiState.notes,
                            onValueChange = viewModel::setNotes,
                            placeholder = "Anything worth remembering",
                            textStyle = MaterialTheme.typography.titleMedium,
                            singleLine = false
                        )
                    }
                }

                AurexButton(
                    text = if (uiState.isEditing) "Save changes" else "Add debt",
                    loading = uiState.isSaving,
                    onClick = viewModel::save
                )

                Spacer(Modifier.height(AurexTheme.spacing.extraExtraLarge))
            }
        }
    }
}
