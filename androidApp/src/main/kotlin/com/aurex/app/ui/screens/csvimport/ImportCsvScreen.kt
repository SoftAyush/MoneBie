package com.aurex.app.ui.screens.csvimport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurex.app.AurexApplication
import com.aurex.app.core.Money
import com.aurex.app.core.MoneyFormatter
import com.aurex.app.domain.model.Account
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.domain.usecase.csvimport.CsvImportPlanner
import com.aurex.app.domain.usecase.csvimport.ParsedCsvRow
import com.aurex.app.presentation.csvimport.CsvImportStep
import com.aurex.app.presentation.csvimport.CsvImportViewModel
import com.aurex.app.security.AppLock
import com.aurex.app.ui.components.AmbientBackdrop
import com.aurex.app.ui.components.AurexButton
import com.aurex.app.ui.components.AurexLoadingScreen
import com.aurex.app.ui.components.CardDivider
import com.aurex.app.ui.components.DetailTopBar
import com.aurex.app.ui.components.EmptyState
import com.aurex.app.ui.components.FieldError
import com.aurex.app.ui.components.FormNotice
import com.aurex.app.ui.components.GlowBadge
import com.aurex.app.ui.components.PremiumCard
import com.aurex.app.ui.components.PremiumSectionHeader
import com.aurex.app.ui.components.pressable
import com.aurex.app.ui.components.SelectableChip
import com.aurex.app.ui.theme.AurexTheme

/**
 * CSV import.
 *
 * Three states on one screen: pick a file, look over what it holds and map
 * anything the phone does not recognise, then a confirmation. Nothing is
 * written until the user says so, and the review says exactly how many rows
 * that will be.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportCsvScreen(
    onBack: () -> Unit,
    onOpenAccounts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AurexApplication

    val viewModel: CsvImportViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CsvImportViewModel(
                    getAccountsUseCase = app.appContainer.getAccountsUseCase,
                    getSelectedCurrencySymbolUseCase = app.appContainer.getSelectedCurrencySymbolUseCase,
                    prepareCsvImportUseCase = app.appContainer.prepareCsvImportUseCase,
                    commitCsvImportUseCase = app.appContainer.commitCsvImportUseCase
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = context.displayName(uri) ?: "transactions.csv"
        val content = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (content == null) {
            viewModel.load(name, "")
        } else {
            viewModel.load(name, content)
        }
    }

    val openPicker = {
        AppLock.expectExternalActivity()
        // Every type a spreadsheet might save as: some label a CSV as plain
        // text, others as Excel's own type.
        picker.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel", "*/*"))
    }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                title = "Import transactions",
                onBack = onBack,
                subtitle = when (val step = uiState.step) {
                    is CsvImportStep.Review -> step.fileName
                    is CsvImportStep.Done -> "Done"
                    else -> "From a CSV file"
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AurexTheme.spacing.medium)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)
            ) {
                Spacer(Modifier.height(AurexTheme.spacing.extraSmall))

                when (val step = uiState.step) {
                    CsvImportStep.Pick -> PickStep(
                        hasAccounts = uiState.accounts.isNotEmpty(),
                        onPick = openPicker,
                        onOpenAccounts = onOpenAccounts
                    )

                    CsvImportStep.Reading -> AurexLoadingScreen(Modifier.fillMaxWidth().height(240.dp))

                    is CsvImportStep.Review -> ReviewStep(
                        step = step,
                        accounts = uiState.accounts,
                        currencySymbol = uiState.currencySymbol,
                        onMapAccount = viewModel::mapAccount,
                        onSkipDuplicatesChange = viewModel::setSkipDuplicates,
                        onCommit = viewModel::commit,
                        onPickAnother = viewModel::reset
                    )

                    is CsvImportStep.Done -> PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Rounded.CheckCircle,
                            title = if (step.imported == 1) "1 transaction imported" else "${step.imported} transactions imported",
                            message = buildString {
                                append("They are in your history now, and every balance has moved to match.")
                                if (step.categoriesCreated > 0) {
                                    append(" ")
                                    append(step.categoriesCreated)
                                    append(if (step.categoriesCreated == 1) " new category was" else " new categories were")
                                    append(" created for names the file used.")
                                }
                            },
                            actionLabel = "Done",
                            onAction = onBack
                        )
                    }

                    is CsvImportStep.Failed -> PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Rounded.ErrorOutline,
                            title = "Could not read that file",
                            message = step.message,
                            actionLabel = "Choose another",
                            onAction = { viewModel.reset(); openPicker() }
                        )
                    }
                }

                Spacer(Modifier.height(AurexTheme.spacing.extraExtraLarge))
            }
        }
    }
}

@Composable
private fun PickStep(
    hasAccounts: Boolean,
    onPick: () -> Unit,
    onOpenAccounts: () -> Unit
) {
    if (!hasAccounts) {
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            EmptyState(
                icon = Icons.Rounded.AccountBalanceWallet,
                title = "Add an account first",
                message = "Imported transactions have to land in an account.",
                actionLabel = "Add an account",
                onAction = onOpenAccounts
            )
        }
        return
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        EmptyState(
            icon = Icons.Rounded.UploadFile,
            title = "Choose a CSV file",
            message = "The file Aurex exports imports straight back. Any other file works " +
                "with the columns Date, Type, Amount and Account - Category, Note and " +
                "Payment Method are welcome too.",
            actionLabel = "Choose file",
            onAction = onPick
        )
    }

    PremiumSectionHeader(title = "How it works")

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        HowItWorksRow("Nothing is written until you confirm", "You see what the file holds first.")
        CardDivider(alpha = 0.07f)
        HowItWorksRow("Accounts are matched by name", "Anything unfamiliar, you point at an account.")
        CardDivider(alpha = 0.07f)
        HowItWorksRow("Categories you do not have get created", "Under the name the file gives them.")
        CardDivider(alpha = 0.07f)
        HowItWorksRow("Rows you already have are skipped", "Same day, amount and account - unless you say otherwise.")
    }
}

@Composable
private fun HowItWorksRow(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(vertical = AurexTheme.spacing.small)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewStep(
    step: CsvImportStep.Review,
    accounts: List<Account>,
    currencySymbol: String,
    onMapAccount: (String, Long) -> Unit,
    onSkipDuplicatesChange: (Boolean) -> Unit,
    onCommit: () -> Unit,
    onPickAnother: () -> Unit
) {
    val plan = step.plan

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)
        ) {
            GlowBadge(icon = Icons.Rounded.FileUpload, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (plan.rows.size == 1) "1 transaction found" else "${plan.rows.size} transactions found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = summarise(plan.rows, currencySymbol),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (plan.errors.isNotEmpty()) {
            Spacer(Modifier.height(AurexTheme.spacing.small))
            FormNotice(
                icon = Icons.Rounded.ErrorOutline,
                text = (if (plan.errors.size == 1) "1 line" else "${plan.errors.size} lines") +
                    " could not be read and will be left out. Line " +
                    plan.errors.first().line + ": " + plan.errors.first().message,
                accent = AurexTheme.finance.caution
            )
        }
    }

    if (plan.unmatchedAccounts.isNotEmpty()) {
        PremiumSectionHeader(title = "Accounts to match")
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "The file names accounts you do not have here. Choose where each one's " +
                    "transactions should go.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            plan.unmatchedAccounts.forEach { name ->
                Spacer(Modifier.height(AurexTheme.spacing.medium))
                Text(
                    text = "\"$name\" in the file",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(AurexTheme.spacing.small))
                val chosen = step.accountMapping[CsvImportPlanner.key(name)]
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
                ) {
                    accounts.forEach { account ->
                        SelectableChip(
                            label = account.name,
                            selected = chosen == account.id,
                            onClick = { onMapAccount(name, account.id) }
                        )
                    }
                }
            }
        }
    }

    if (plan.newCategories.isNotEmpty()) {
        PremiumSectionHeader(title = "New categories")
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)
            ) {
                GlowBadge(icon = Icons.Rounded.Sell, tint = MaterialTheme.colorScheme.primary, size = 36.dp)
                Text(
                    text = "These will be created: " + plan.newCategories.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (plan.likelyDuplicates.isNotEmpty()) {
        PremiumSectionHeader(title = "Already in Aurex")
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.medium)
            ) {
                GlowBadge(icon = Icons.Rounded.ContentCopy, tint = AurexTheme.finance.caution, size = 36.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Skip " + plan.likelyDuplicates.size +
                            if (plan.likelyDuplicates.size == 1) " likely duplicate" else " likely duplicates",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Same day, direction, amount and account as a transaction you already have.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = step.skipDuplicates, onCheckedChange = onSkipDuplicatesChange)
            }
        }
    }

    PremiumSectionHeader(title = "First few rows")
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        plan.rows.take(5).forEachIndexed { index, row ->
            if (index > 0) CardDivider(alpha = 0.07f)
            PreviewRow(row, currencySymbol)
        }
        if (plan.rows.size > 5) {
            CardDivider(alpha = 0.07f)
            Text(
                text = "and ${plan.rows.size - 5} more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = AurexTheme.spacing.small)
            )
        }
    }

    FieldError(step.error)

    AurexButton(
        text = if (step.willImport == 1) "Import 1 transaction" else "Import ${step.willImport} transactions",
        onClick = onCommit,
        loading = step.isCommitting,
        enabled = step.isReady && step.willImport > 0,
        modifier = Modifier.fillMaxWidth()
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = "Choose a different file",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .pressable(onPickAnother)
                .padding(AurexTheme.spacing.small)
        )
    }
}

@Composable
private fun PreviewRow(row: ParsedCsvRow, currencySymbol: String) {
    val accent = when (row.type) {
        TransactionType.INCOME -> AurexTheme.finance.positive
        TransactionType.EXPENSE -> AurexTheme.finance.negative
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AurexTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.note?.takeIf { it.isNotBlank() } ?: row.categoryName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = row.day + " · " + row.categoryName + " · " + row.accountName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = MoneyFormatter.format(row.amount, currencySymbol, showDecimals = false),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

private fun summarise(rows: List<ParsedCsvRow>, symbol: String): String {
    val income = rows.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.minorUnits }
    val expense = rows.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.minorUnits }
    val first = rows.minOfOrNull { it.day }
    val last = rows.maxOfOrNull { it.day }
    return buildString {
        append(MoneyFormatter.compact(Money(income), symbol))
        append(" in, ")
        append(MoneyFormatter.compact(Money(expense), symbol))
        append(" out")
        if (first != null && last != null) {
            append(" · ")
            append(if (first == last) first else "$first to $last")
        }
    }
}

private fun Context.displayName(uri: Uri): String? = runCatching {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()
