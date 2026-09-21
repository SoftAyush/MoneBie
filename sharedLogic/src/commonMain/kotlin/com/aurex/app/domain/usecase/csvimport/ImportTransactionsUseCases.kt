package com.aurex.app.domain.usecase.csvimport

import com.aurex.app.core.dayOf
import com.aurex.app.core.timestampForDay
import com.aurex.app.domain.model.Account
import com.aurex.app.domain.model.Transaction
import com.aurex.app.domain.model.TransactionCategories
import com.aurex.app.domain.model.TransactionCategory
import com.aurex.app.domain.model.TransactionDraft
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.domain.repository.AccountRepository
import com.aurex.app.domain.repository.CategoryRepository
import com.aurex.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first

/**
 * A parsed file matched against this phone's accounts and categories, with
 * every decision the user still has to make laid out.
 *
 * Nothing is written while a plan exists. The screen shows it, the user maps
 * any account the file names that this phone does not have, and only then is
 * the plan committed - so a file that turns out to be the wrong one costs
 * nothing to walk away from.
 */
data class CsvImportPlan(
    val rows: List<ParsedCsvRow>,
    val errors: List<CsvRowError>,
    /** Account names in the file matched to an account here, case-insensitively. */
    val matchedAccounts: Map<String, Long>,
    /** Account names in the file with no match, for the user to map or create. */
    val unmatchedAccounts: List<String>,
    /** Category names in the file that will be created as new categories. */
    val newCategories: List<String>,
    /** Rows that look identical to one already in the ledger. */
    val likelyDuplicates: List<ParsedCsvRow>
) {
    val importableCount: Int get() = rows.size

    val isEmpty: Boolean get() = rows.isEmpty()

    /** Whether every account the file names has somewhere to go. */
    val isResolved: Boolean get() = unmatchedAccounts.isEmpty()
}

/** Parses and matches. Nothing is written. */
class PrepareCsvImportUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) {

    suspend operator fun invoke(content: String): CsvImportPlan {
        val parsed = TransactionCsvParser.parse(content)
        // Archived accounts count as a match: an export made before an account
        // was archived still names it, and re-filing that history elsewhere
        // would be wrong.
        val accounts = accountRepository.observeAccounts().first()
        val categories = categoryRepository.observeCategories().first()
        val existing = transactionRepository.observeTransactions().first()
        return CsvImportPlanner.plan(parsed, accounts, categories, existing)
    }
}

/**
 * Writes a plan to the ledger.
 *
 * [accountMapping] supplies an account id for every name the plan could not
 * match - the user picked one, or asked for it to be created and the screen
 * created it first. Missing categories are created here as custom ones on the
 * row's side of the ledger.
 */
class CommitCsvImportUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) {

    sealed interface Result {
        data class Imported(val count: Int, val categoriesCreated: Int) : Result
        data class Failed(val message: String) : Result
    }

    /**
     * [accountMapping] is keyed by [CsvImportPlanner.key] of the file's account
     * name, the same key [CsvImportPlan.matchedAccounts] uses.
     */
    suspend operator fun invoke(
        plan: CsvImportPlan,
        accountMapping: Map<String, Long>,
        skipDuplicates: Boolean
    ): Result {
        val accountIds = plan.matchedAccounts + accountMapping
        val unresolved = plan.unmatchedAccounts.filter { CsvImportPlanner.key(it) !in accountMapping }
        if (unresolved.isNotEmpty()) {
            return Result.Failed("Choose an account for " + unresolved.joinToString(", "))
        }

        val validAccounts = accountRepository.observeAccounts().first().map { it.id }.toSet()
        if (accountIds.values.any { it !in validAccounts }) {
            return Result.Failed("One of the chosen accounts no longer exists")
        }

        val duplicates = if (skipDuplicates) plan.likelyDuplicates.toSet() else emptySet()
        val rows = plan.rows.filter { it !in duplicates }
        if (rows.isEmpty()) return Result.Imported(0, 0)

        val categories = categoryRepository.observeCategories().first().toMutableList()
        var created = 0
        val drafts = rows.map { row ->
            val categoryId = when (row.type) {
                TransactionType.TRANSFER -> TransactionCategories.transfer.id
                else -> {
                    val match = CsvImportPlanner.findCategory(categories, row.categoryName, row.type)
                    if (match != null) {
                        match.id
                    } else {
                        val id = categoryRepository.createCategory(
                            name = row.categoryName,
                            iconKey = "other",
                            appliesTo = row.type
                        )
                        created++
                        categories += TransactionCategory(
                            id = id,
                            name = row.categoryName,
                            iconKey = "other",
                            appliesTo = row.type,
                            isBuiltIn = false
                        )
                        id
                    }
                }
            }
            TransactionDraft(
                amount = row.amount,
                type = row.type,
                categoryId = categoryId,
                accountId = accountIds.getValue(CsvImportPlanner.key(row.accountName)),
                date = timestampForDay(row.day),
                note = row.note,
                paymentMethod = row.paymentMethod,
                transferAccountId = row.transferAccountName?.let {
                    accountIds[CsvImportPlanner.key(it)]
                }
            )
        }

        val count = transactionRepository.postTransactions(drafts)
        return Result.Imported(count, created)
    }
}

/** Pure matching logic, kept apart from the repositories so it can be tested. */
object CsvImportPlanner {

    /** Names compare trimmed and case-insensitively: "Everyday" and "everyday " are one account. */
    fun key(name: String): String = name.trim().lowercase()

    fun plan(
        parsed: CsvParseResult,
        accounts: List<Account>,
        categories: List<TransactionCategory>,
        existing: List<Transaction>
    ): CsvImportPlan {
        val accountByKey = accounts.associateBy { key(it.name) }

        val namedAccounts = parsed.rows
            .flatMap { listOfNotNull(it.accountName, it.transferAccountName) }
            .distinctBy { key(it) }

        val matched = mutableMapOf<String, Long>()
        val unmatched = mutableListOf<String>()
        namedAccounts.forEach { name ->
            val account = accountByKey[key(name)]
            if (account != null) matched[key(name)] = account.id else unmatched += name.trim()
        }

        val newCategories = parsed.rows
            .filter { it.type != TransactionType.TRANSFER }
            .filter { findCategory(categories, it.categoryName, it.type) == null }
            .map { it.categoryName.trim() }
            .distinctBy { key(it) }

        // A duplicate is the same day, direction, amount and account. The note
        // is left out: an export round-trips it, but a hand-written file may
        // not, and two coffees on one day at the same price really are two
        // rows - so this is offered as a skip, not applied silently.
        val fingerprints = existing.map { fingerprint(dayOf(it.date), it.type, it.amount.minorUnits, it.accountId) }.toSet()
        val duplicates = parsed.rows.filter { row ->
            val accountId = matched[key(row.accountName)] ?: return@filter false
            fingerprint(row.day, row.type, row.amount.minorUnits, accountId) in fingerprints
        }

        return CsvImportPlan(
            rows = parsed.rows,
            errors = parsed.errors,
            matchedAccounts = matched,
            unmatchedAccounts = unmatched,
            newCategories = newCategories,
            likelyDuplicates = duplicates
        )
    }

    /**
     * A category by name on the right side of the ledger.
     *
     * "Other" exists on both sides, so the side matters; and an archived one
     * still matches, because the history it names is exactly what an old
     * export contains.
     */
    fun findCategory(
        categories: List<TransactionCategory>,
        name: String,
        type: TransactionType
    ): TransactionCategory? {
        val wanted = key(name)
        return categories.firstOrNull { it.appliesTo == type && key(it.name) == wanted }
            ?: categories.firstOrNull { it.appliesTo == type && it.id == wanted }
    }

    private fun fingerprint(day: String, type: TransactionType, minor: Long, accountId: Long) =
        "$day|${type.name}|$minor|$accountId"
}
