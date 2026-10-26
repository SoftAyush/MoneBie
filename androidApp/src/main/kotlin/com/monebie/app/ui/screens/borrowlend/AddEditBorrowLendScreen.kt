package com.monebie.app.ui.screens.borrowlend

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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.monebie.app.MoneBieApplication
import com.monebie.app.domain.model.LoanDirection
import com.monebie.app.presentation.borrowlend.AddEditBorrowLendViewModel
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
import com.monebie.app.ui.components.Segment
import com.monebie.app.ui.components.SegmentedSelector
import com.monebie.app.ui.theme.MoneBieTheme

/**
 * Record money borrowed or lent.
 *
 * Direction, amount and account are set once and then locked: they determine
 * the principal movement already posted to an account, and every repayment
 * recorded against it.
 */
@Composable
fun AddEditBorrowLendScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    recordId: Long? = null,
    initialDirection: LoanDirection = LoanDirection.LENT
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneBieApplication

    val viewModel: AddEditBorrowLendViewModel = viewModel(
        key = "add_edit_loan_" + (recordId ?: -1L) + "_" + initialDirection.name,
        factory = viewModelFactory {
            initializer {
                AddEditBorrowLendViewModel(
                    editingRecordId = recordId,
                    initialDirection = initialDirection,
                    getLoanRecordUseCase = app.appContainer.getLoanRecordUseCase,
                    getAccountsUseCase = app.appContainer.getAccountsUseCase,
                    getSelectedCurrencySymbolUseCase = app.appContainer.getSelectedCurrencySymbolUseCase,
                    saveLoanRecordUseCase = app.appContainer.saveLoanRecordUseCase
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
                title = if (uiState.isEditing) "Edit record" else "New record",
                onBack = onBack,
                subtitle = if (uiState.isBorrowed) "You borrowed" else "You lent"
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

                if (uiState.canEditTerms) {
                    // Phrased from the user's side rather than as "Borrowed" /
                    // "Lent", which are easy to read the wrong way round.
                    SegmentedSelector(
                        segments = listOf(
                            Segment(
                                value = LoanDirection.BORROWED,
                                label = "I borrowed",
                                accent = MoneBieTheme.finance.negative
                            ),
                            Segment(
                                value = LoanDirection.LENT,
                                label = "I lent",
                                accent = MoneBieTheme.finance.receivable
                            )
                        ),
                        selected = uiState.direction,
                        onSelect = viewModel::setDirection
                    )
                } else {
                    FormNotice(
                        icon = Icons.Rounded.Info,
                        text = "The direction, amount and account are fixed once a record exists, " +
                            "because money has already moved against them. Delete and re-create " +
                            "to change them."
                    )
                }

                FormSection(title = "Who and how much") {
                    FormField(
                        label = if (uiState.isBorrowed) "Borrowed from" else "Lent to",
                        error = uiState.nameError
                    ) {
                        MoneBieTextField(
                            value = uiState.personName,
                            onValueChange = viewModel::setPersonName,
                            placeholder = "Their name",
                            leadingIcon = Icons.Rounded.PersonOutline,
                            textStyle = MaterialTheme.typography.titleMedium
                        )
                    }

                    FormField(label = "Amount", error = uiState.amountError) {
                        MoneyInputField(
                            value = uiState.amountInput,
                            currencySymbol = uiState.currencySymbol,
                            onValueChange = viewModel::setAmount
                        )
                    }
                }

                FormSection(title = "Dates") {
                    FormField(label = if (uiState.isBorrowed) "Borrowed on" else "Lent on") {
                        MoneBieDateField(
                            day = uiState.date,
                            onDayChange = { day -> day?.let(viewModel::setDate) }
                        )
                    }

                    FormField(
                        label = if (uiState.isBorrowed) {
                            "Due date (optional)"
                        } else {
                            "Expected back (optional)"
                        }
                    ) {
                        MoneBieDateField(
                            day = uiState.dueDate,
                            onDayChange = viewModel::setDueDate,
                            placeholder = "No date agreed",
                            clearable = true
                        )
                    }
                }

                if (uiState.canEditTerms) {
                    FormSection(title = "The money itself") {
                        FormField(
                            label = "Money moved through",
                            hint = if (uiState.accounts.isEmpty()) {
                                null
                            } else if (uiState.accountId == null) {
                                "No balance will change. The amount still counts towards what " +
                                    "you owe or are owed."
                            } else {
                                uiState.accountEffectHint
                            }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(
                                    MoneBieTheme.spacing.small
                                )
                            ) {
                                AccountPicker(
                                    accounts = uiState.accounts,
                                    selectedId = uiState.accountId,
                                    currencySymbol = uiState.currencySymbol,
                                    onSelect = viewModel::setAccount,
                                    emptyMessage = "No accounts yet"
                                )

                                if (uiState.accounts.isNotEmpty()) {
                                    // Cash that never touched a tracked account:
                                    // record the arrangement without moving a
                                    // balance that did not move.
                                    FilterPill(
                                        label = "Cash - no account involved",
                                        selected = uiState.accountId == null,
                                        onClick = { viewModel.setAccount(null) }
                                    )
                                }
                            }
                        }
                    }
                }

                FormSection(title = "Details") {
                    FormField(label = "Notes (optional)") {
                        MoneBieTextField(
                            value = uiState.notes,
                            onValueChange = viewModel::setNotes,
                            placeholder = "What was it for?",
                            textStyle = MaterialTheme.typography.titleMedium,
                            singleLine = false
                        )
                    }
                }

                MoneBieButton(
                    text = if (uiState.isEditing) "Save changes" else "Save record",
                    loading = uiState.isSaving,
                    onClick = viewModel::save
                )

                Spacer(Modifier.height(MoneBieTheme.spacing.extraExtraLarge))
            }
        }
    }
}
