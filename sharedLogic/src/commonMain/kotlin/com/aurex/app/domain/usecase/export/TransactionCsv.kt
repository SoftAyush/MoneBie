package com.aurex.app.domain.usecase.export

import com.aurex.app.core.dayOf
import com.aurex.app.domain.model.Account
import com.aurex.app.domain.model.Transaction
import com.aurex.app.domain.model.CategoryLookup
import com.aurex.app.domain.model.displayName

/** A generated file, ready for the platform layer to write and share. */
data class ExportedFile(
    val fileName: String,
    val mimeType: String,
    val content: String,
    val rowCount: Int
)

/**
 * Renders transactions as CSV.
 *
 * A pure function of its inputs, deliberately separate from the use case that
 * loads them: a broken export is otherwise only discovered by opening the file
 * in a spreadsheet, and this way the format itself can be asserted directly.
 *
 * Amounts are written in major units with a plain `.` separator and no
 * thousands grouping, which is what spreadsheets parse back into numbers.
 * Display formatting is not reused here for exactly that reason.
 */
object TransactionCsv {

    private const val HEADER =
        "Date,Type,Category,Account,Transfer To,Amount,Payment Method,Note,Source"

    const val FILE_NAME = "aurex-transactions.csv"
    const val MIME_TYPE = "text/csv"

    fun build(
        transactions: List<Transaction>,
        accounts: List<Account>,
        categories: CategoryLookup = CategoryLookup.Seeded
    ): ExportedFile {
        val accountNames = accounts.associate { it.id to it.name }

        val content = buildString {
            appendLine(HEADER)
            transactions
                .sortedByDescending { it.date }
                .forEach { transaction ->
                    appendLine(
                        listOf(
                            dayOf(transaction.date),
                            transaction.type.name,
                            categories[transaction.categoryId].name,
                            accountNames[transaction.accountId] ?: "Unknown account",
                            transaction.transferAccountId
                                ?.let { accountNames[it] ?: "Unknown account" }
                                .orEmpty(),
                            majorUnits(transaction),
                            transaction.paymentMethod.displayName,
                            transaction.note.orEmpty(),
                            // Says where a row came from, so an automatically
                            // posted debt or EMI payment is identifiable in the
                            // export rather than looking hand-entered.
                            transaction.source.type.name
                        ).joinToString(",") { escapeCsv(it) }
                    )
                }
        }

        return ExportedFile(
            fileName = FILE_NAME,
            mimeType = MIME_TYPE,
            content = content,
            rowCount = transactions.size
        )
    }

    /** `1234.56`, always two decimals, no grouping separator. */
    private fun majorUnits(transaction: Transaction): String {
        val minor = transaction.amount.minorUnits
        val negative = minor < 0L
        val absolute = if (negative) -minor else minor
        val whole = absolute / 100L
        val fraction = (absolute % 100L).toString().padStart(2, '0')
        return (if (negative) "-" else "") + whole + "." + fraction
    }

    /**
     * Quotes a field when it contains anything that would break the row.
     *
     * Notes and account names are free text and routinely contain commas;
     * without this a single comma would shift every later column in the row.
     */
    private fun escapeCsv(value: String): String {
        val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuoting) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
