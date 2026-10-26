package com.monebie.app.ui.screens.debt

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
import com.monebie.app.MoneBieApplication
import com.monebie.app.domain.model.PaymentFrequency
import com.monebie.app.domain.model.displayName
import com.monebie.app.presentation.debt.AddEditDebtViewModel
import com.monebie.app.ui.components.AccountPicker
import com.monebie.app.ui.components.AmbientBackdrop
import com.monebie.app.ui.components.MoneBieButton
import com.monebie.app.ui.components.MoneBieDateField
import com.monebie.app.ui.components.MoneBieTextField
import com.monebie.app.ui.components.DetailTopBar
import com.monebie.app.ui.components.FilterPill
import com.monebie.app.ui.components.FormField
import com.monebie.app.ui.components.FormSection
import com.monebie.app.ui.components.MoneyInputField
import com.monebie.app.ui.theme.MoneBieTheme

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
    val app = context.applicationContext as MoneBieApplication

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
                    .padding(horizontal = MoneBieTheme.spacing.medium)
                    .imePadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.large)
            ) {
                Spacer(Modifier.height(MoneBieTheme.spacing.extraSmall))

                FormSection(title = "The debt") {
                    FormField(label = "What is it", error = uiState.nameError) {
                        MoneBieTextField(
                            value = uiState.name,
                            onValueChange = viewModel::setName,
                            placeholder = "e.g. Car loan",
                            leadingIcon = Icons.Rounded.EditNote,
                            textStyle = MaterialTheme.typography.titleMedium
                        )
                    }

                    FormField(label = "Owed to", error = uiState.lenderError) {
                        MoneBieTextField(
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
                        MoneBieDateField(
                            day = uiState.startDate,
                            onDayChange = { day -> day?.let(viewModel::setStartDate) }
                        )
                    }

                    FormField(label = "Due date (optional)") {
                        MoneBieDateField(
                            day = uiState.dueDate,
                            onDayChange = viewModel::setDueDate,
                            placeholder = "No due date set",
                            clearable = true
                        )
                    }

                    FormField(label = "Payment frequency") {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small),
                            verticalArrangement = Arrangement.spacedBy(MoneBieTheme.spacing.small)
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
                        MoneBieTextField(
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
                        MoneBieTextField(
                            value = uiState.notes,
                            onValueChange = viewModel::setNotes,
                            placeholder = "Anything worth remembering",
                            textStyle = MaterialTheme.typography.titleMedium,
                            singleLine = false
                        )
                    }
                }

                MoneBieButton(
                    text = if (uiState.isEditing) "Save changes" else "Add debt",
                    loading = uiState.isSaving,
                    onClick = viewModel::save
                )

                Spacer(Modifier.height(MoneBieTheme.spacing.extraExtraLarge))
            }
        }
    }
}
