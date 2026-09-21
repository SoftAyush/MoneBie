package com.aurex.app.presentation.csvimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurex.app.domain.model.Account
import com.aurex.app.domain.usecase.account.GetAccountsUseCase
import com.aurex.app.domain.usecase.csvimport.CommitCsvImportUseCase
import com.aurex.app.domain.usecase.csvimport.CsvImportPlan
import com.aurex.app.domain.usecase.csvimport.CsvImportPlanner
import com.aurex.app.domain.usecase.csvimport.PrepareCsvImportUseCase
import com.aurex.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the import is, from picking a file to having written it. */
sealed interface CsvImportStep {
    /** Nothing chosen yet. */
    data object Pick : CsvImportStep

    /** The file is being read and matched. */
    data object Reading : CsvImportStep

    /** The plan is shown and the user is deciding. */
    data class Review(
        val fileName: String,
        val plan: CsvImportPlan,
        /** Keyed by [CsvImportPlanner.key] of the file's account name. */
        val accountMapping: Map<String, Long> = emptyMap(),
        val skipDuplicates: Boolean = true,
        val isCommitting: Boolean = false,
        val error: String? = null
    ) : CsvImportStep {
        /** Every account named in the file now has somewhere to go. */
        val isReady: Boolean
            get() = plan.unmatchedAccounts.all { CsvImportPlanner.key(it) in accountMapping }

        val willImport: Int
            get() = plan.rows.size - (if (skipDuplicates) plan.likelyDuplicates.size else 0)
    }

    data class Done(val imported: Int, val categoriesCreated: Int) : CsvImportStep

    data class Failed(val message: String) : CsvImportStep
}

data class CsvImportUiState(
    val step: CsvImportStep = CsvImportStep.Pick,
    val accounts: List<Account> = emptyList(),
    val currencySymbol: String = "$"
)

class CsvImportViewModel(
    getAccountsUseCase: GetAccountsUseCase,
    getSelectedCurrencySymbolUseCase: GetSelectedCurrencySymbolUseCase,
    private val prepareCsvImportUseCase: PrepareCsvImportUseCase,
    private val commitCsvImportUseCase: CommitCsvImportUseCase
) : ViewModel() {

    private val step = MutableStateFlow<CsvImportStep>(CsvImportStep.Pick)

    val uiState: StateFlow<CsvImportUiState> = combine(
        step,
        getAccountsUseCase(),
        getSelectedCurrencySymbolUseCase()
    ) { current, accounts, symbol ->
        CsvImportUiState(
            step = current,
            accounts = accounts.filter { !it.isArchived },
            currencySymbol = symbol ?: "$"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CsvImportUiState()
    )

    /** The platform read the file; this turns it into a plan to look over. */
    fun load(fileName: String, content: String) {
        step.value = CsvImportStep.Reading
        viewModelScope.launch {
            val result = runCatching { prepareCsvImportUseCase(content) }
            step.value = result.fold(
                onSuccess = { plan ->
                    if (plan.isEmpty && plan.errors.isNotEmpty()) {
                        CsvImportStep.Failed(
                            plan.errors.first().message +
                                if (plan.errors.size > 1) " (and ${plan.errors.size - 1} more)" else ""
                        )
                    } else if (plan.isEmpty) {
                        CsvImportStep.Failed("The file has no transactions in it")
                    } else {
                        CsvImportStep.Review(fileName = fileName, plan = plan)
                    }
                },
                onFailure = { CsvImportStep.Failed(it.message ?: "Could not read the file") }
            )
        }
    }

    fun mapAccount(fileAccountName: String, accountId: Long) {
        step.update { current ->
            if (current !is CsvImportStep.Review) return@update current
            current.copy(
                accountMapping = current.accountMapping +
                    (CsvImportPlanner.key(fileAccountName) to accountId),
                error = null
            )
        }
    }

    fun setSkipDuplicates(skip: Boolean) {
        step.update { current ->
            if (current is CsvImportStep.Review) current.copy(skipDuplicates = skip) else current
        }
    }

    fun commit() {
        val current = step.value as? CsvImportStep.Review ?: return
        if (current.isCommitting) return
        if (!current.isReady) {
            step.value = current.copy(error = "Choose an account for every name in the file")
            return
        }

        step.value = current.copy(isCommitting = true, error = null)
        viewModelScope.launch {
            val result = runCatching {
                commitCsvImportUseCase(
                    plan = current.plan,
                    accountMapping = current.accountMapping,
                    skipDuplicates = current.skipDuplicates
                )
            }.getOrElse { CommitCsvImportUseCase.Result.Failed(it.message ?: "Import failed") }

            step.value = when (result) {
                is CommitCsvImportUseCase.Result.Imported ->
                    CsvImportStep.Done(result.count, result.categoriesCreated)

                is CommitCsvImportUseCase.Result.Failed ->
                    current.copy(isCommitting = false, error = result.message)
            }
        }
    }

    fun reset() {
        step.value = CsvImportStep.Pick
    }
}
