package com.monebie.app.ui.screens.emi

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.WarningAmber
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
import com.monebie.app.presentation.emi.AddEditEmiViewModel
import com.monebie.app.ui.components.AccountPicker
import com.monebie.app.ui.components.AmbientBackdrop
import com.monebie.app.ui.components.MoneBieButton
import com.monebie.app.ui.components.MoneBieDateField
import com.monebie.app.ui.components.MoneBieTextField
import com.monebie.app.ui.components.DetailTopBar
import com.monebie.app.ui.components.FilterPill
import com.monebie.app.ui.components.FormField
import com.monebie.app.ui.components.FormNotice
import com.monebie.app.ui.components.FormSection
import com.monebie.app.ui.components.MoneyInputField
import com.monebie.app.ui.theme.MoneBieTheme

/**
 * Create or edit an EMI.
 *
 * The monthly amount and total payable can be auto-calculated from principal,
 * rate and tenure, but whatever is in the fields is what gets stored - a real
 * lender's schedule can differ from the formula, and the record must match the
 * actual obligation.
 */
@Composable
fun AddEditEmiScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    emiId: Long? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneBieApplication

    val viewModel: AddEditEmiViewModel = viewModel(
        key = "add_edit_emi_" + (emiId ?: -1L),
        factory = viewModelFactory {
            initializer {
                AddEditEmiViewModel(
                    editingEmiId = emiId,
                    getEmiUseCase = app.appContainer.getEmiUseCase,
                    getAccountsUseCase = app.appContainer.getAccountsUseCase,
                    getSelectedCurrencySymbolUseCase = app.appContainer.getSelectedCurrencySymbolUseCase,
                    saveEmiUseCase = app.appContainer.saveEmiUseCase
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
                title = if (uiState.isEditing) "Edit EMI" else "Add EMI",
                onBack = onBack,
                subtitle = "Instalment plan"
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

                uiState.scheduleWarning?.let { warning ->
                    FormNotice(
                        icon = Icons.Rounded.WarningAmber,
                        text = warning,
                        accent = MoneBieTheme.finance.caution
                    )
                }

                FormSection(title = "What it is") {
                    FormField(label = "Name", error = uiState.nameError) {
                        MoneBieTextField(
                            value = uiState.name,
                            onValueChange = viewModel::setName,
                            placeholder = "e.g. Car loan",
                            leadingIcon = Icons.Rounded.EditNote,
                            textStyle = MaterialTheme.typography.titleMedium
                        )
                    }

                    FormField(label = "Provider", error = uiState.providerError) {
                        MoneBieTextField(
                            value = uiState.provider,
                            onValueChange = viewModel::setProvider,
                            placeholder = "Bank or finance company",
                            leadingIcon = Icons.Rounded.Storefront,
                            textStyle = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                FormSection(title = "The loan") {
                    FormField(label = "Amount financed", error = uiState.principalError) {
                        MoneyInputField(
                            value = uiState.principalInput,
                            currencySymbol = uiState.currencySymbol,
                            onValueChange = viewModel::setPrincipal
                        )
                    }

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

                    FormField(
                        label = "Tenure in years",
                        error = uiState.tenureError,
                        // Both figures are derived, shown live so a mistyped
                        // tenure is obvious before saving rather than after.
                        hint = uiState.installmentCount?.let { count ->
                            "$count monthly installments" +
                                (uiState.endDatePreview?.let { ", ending $it" } ?: "")
                        }
                    ) {
                        MoneBieTextField(
                            value = uiState.tenureYearsInput,
                            onValueChange = viewModel::setTenureYears,
                            placeholder = "e.g. 5",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                FormSection(title = "The schedule") {
                    FormField(
                        label = "Monthly installment",
                        error = uiState.monthlyError,
                        trailing = if (uiState.canAutoCalculate) {
                            {
                                FilterPill(
                                    label = "Calculate",
                                    icon = Icons.Rounded.AutoAwesome,
                                    selected = false,
                                    onClick = viewModel::autoCalculate
                                )
                            }
                        } else {
                            null
                        }
                    ) {
                        MoneyInputField(
                            value = uiState.monthlyAmountInput,
                            currencySymbol = uiState.currencySymbol,
                            onValueChange = viewModel::setMonthlyAmount
                        )
                    }

                    FormField(
                        label = "Total payable (optional)",
                        hint = "Leave blank to use the monthly amount across the full tenure."
                    ) {
                        MoneyInputField(
                            value = uiState.totalPayableInput,
                            currencySymbol = uiState.currencySymbol,
                            onValueChange = viewModel::setTotalPayable
                        )
                    }

                    FormField(
                        label = "Start date",
                        hint = "The first installment falls due one month after this date."
                    ) {
                        MoneBieDateField(
                            day = uiState.startDate,
                            onDayChange = { day -> day?.let(viewModel::setStartDate) }
                        )
                    }
                }

                FormSection(title = "Details") {
                    FormField(label = "Paid from (optional)") {
                        AccountPicker(
                            accounts = uiState.accounts,
                            selectedId = uiState.paymentAccountId,
                            currencySymbol = uiState.currencySymbol,
                            onSelect = viewModel::setPaymentAccount,
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
                    text = if (uiState.isEditing) "Save changes" else "Add EMI",
                    loading = uiState.isSaving,
                    onClick = viewModel::save
                )

                Spacer(Modifier.height(MoneBieTheme.spacing.extraExtraLarge))
            }
        }
    }
}
