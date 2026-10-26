package com.aurex.app.domain.usecase.csvimport

import com.aurex.app.core.Money
import com.aurex.app.domain.model.PaymentMethod
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.domain.model.displayName

/**
 * One line of a CSV as the importer understood it.
 *
 * Names rather than ids: the file was written by a person or by an export, and
 * either way it knows accounts and categories only by what they are called.
 * Resolving those names against what this phone has is the plan's job.
 */
data class ParsedCsvRow(
    /** 1-based line in the file, for error messages. */
    val line: Int,
    /** yyyy-MM-dd */
    val day: String,
    val type: TransactionType,
    val categoryName: String,
    val accountName: String,
    val transferAccountName: String?,
    val amount: Money,
    val paymentMethod: PaymentMethod,
    val note: String?
)

/** A line the importer could not use, and why. */
data class CsvRowError(val line: Int, val message: String)

data class CsvParseResult(
    val rows: List<ParsedCsvRow>,
    val errors: List<CsvRowError>
)

/**
 * Reads the CSV that [com.aurex.app.domain.usecase.export.TransactionCsv]
 * writes, and anything else laid out the same way.
 *
 * Columns are found by header name rather than position, so a file with the
 * columns reordered, or with extra ones, still imports. The only required
 * columns are Date, Type, Amount and Account; Category defaults to "Other" on
 * whichever side the type puts it.
 */
object TransactionCsvParser {

    private const val DATE = "date"
    private const val TYPE = "type"
    private const val CATEGORY = "category"
    private const val ACCOUNT = "account"
    private const val TRANSFER_TO = "transfer to"
    private const val AMOUNT = "amount"
    private const val PAYMENT_METHOD = "payment method"
    private const val NOTE = "note"

    private val REQUIRED = listOf(DATE, TYPE, AMOUNT, ACCOUNT)

    fun parse(content: String): CsvParseResult {
        val lines = splitRecords(content.removePrefix("﻿"))
        if (lines.isEmpty()) {
            return CsvParseResult(emptyList(), listOf(CsvRowError(0, "The file is empty")))
        }

        val header = splitFields(lines.first()).map { it.trim().lowercase() }
        val missing = REQUIRED.filter { it !in header }
        if (missing.isNotEmpty()) {
            return CsvParseResult(
                emptyList(),
                listOf(
                    CsvRowError(
                        1,
                        "Missing column" + (if (missing.size > 1) "s " else " ") +
                            missing.joinToString(", ") { it.replaceFirstChar(Char::uppercase) }
                    )
                )
            )
        }

        val column = header.withIndex().associate { (index, name) -> name to index }
        fun List<String>.field(name: String): String? =
            column[name]?.let { getOrNull(it)?.trim() }?.takeIf { it.isNotEmpty() }

        val rows = mutableListOf<ParsedCsvRow>()
        val errors = mutableListOf<CsvRowError>()

        lines.drop(1).forEachIndexed { offset, raw ->
            val line = offset + 2
            if (raw.isBlank()) return@forEachIndexed
            val fields = splitFields(raw)

            val day = fields.field(DATE)?.let(::normaliseDay)
            if (day == null) {
                errors += CsvRowError(line, "Date must be yyyy-MM-dd")
                return@forEachIndexed
            }

            val type = fields.field(TYPE)?.let(::parseType)
            if (type == null) {
                errors += CsvRowError(line, "Type must be Income, Expense or Transfer")
                return@forEachIndexed
            }

            val amount = fields.field(AMOUNT)?.let(Money::parse)?.absolute
            if (amount == null || !amount.isPositive) {
                errors += CsvRowError(line, "Amount must be a number greater than zero")
                return@forEachIndexed
            }

            val account = fields.field(ACCOUNT)
            if (account == null) {
                errors += CsvRowError(line, "Account is empty")
                return@forEachIndexed
            }

            val transferTo = fields.field(TRANSFER_TO)
            if (type == TransactionType.TRANSFER && transferTo == null) {
                errors += CsvRowError(line, "A transfer needs a Transfer To account")
                return@forEachIndexed
            }

            rows += ParsedCsvRow(
                line = line,
                day = day,
                type = type,
                categoryName = fields.field(CATEGORY) ?: "Other",
                accountName = account,
                transferAccountName = transferTo.takeIf { type == TransactionType.TRANSFER },
                amount = amount,
                paymentMethod = fields.field(PAYMENT_METHOD)?.let(::parsePaymentMethod)
                    ?: PaymentMethod.OTHER,
                note = fields.field(NOTE)
            )
        }

        return CsvParseResult(rows, errors)
    }

    /**
     * Accepts `2026-09-04`, `2026-09-04 10:00:00`, `04/09/2026` and
     * `2026/09/04`. The slash form is read day-first, which is what most of
     * the world's spreadsheets write; a file from the US would need the ISO
     * form the export already uses.
     */
    private fun normaliseDay(raw: String): String? {
        val iso = Regex("""^(\d{4})[-/](\d{1,2})[-/](\d{1,2})""").find(raw)
        if (iso != null) {
            val (y, m, d) = iso.destructured
            return assemble(y, m, d)
        }
        val dayFirst = Regex("""^(\d{1,2})[-/](\d{1,2})[-/](\d{4})""").find(raw)
        if (dayFirst != null) {
            val (d, m, y) = dayFirst.destructured
            return assemble(y, m, d)
        }
        return null
    }

    private fun assemble(y: String, m: String, d: String): String? {
        val month = m.toInt()
        val day = d.toInt()
        if (month !in 1..12 || day !in 1..31) return null
        return y + "-" + m.padStart(2, '0') + "-" + d.padStart(2, '0')
    }

    private fun parseType(raw: String): TransactionType? {
        val key = raw.trim().uppercase()
        return TransactionType.entries.firstOrNull { it.name == key } ?: when (key) {
            "IN", "CREDIT", "DEPOSIT" -> TransactionType.INCOME
            "OUT", "DEBIT", "SPEND", "SPENDING", "WITHDRAWAL" -> TransactionType.EXPENSE
            "MOVE" -> TransactionType.TRANSFER
            else -> null
        }
    }

    private fun parsePaymentMethod(raw: String): PaymentMethod? {
        val key = raw.trim()
        return PaymentMethod.entries.firstOrNull {
            it.name.equals(key, ignoreCase = true) ||
                it.name.replace('_', ' ').equals(key, ignoreCase = true) ||
                it.displayName.equals(key, ignoreCase = true)
        }
    }

    /**
     * Splits a file into records, honouring quotes: a quoted note may contain
     * a newline, and that newline is part of the note rather than the end of
     * the row.
     */
    private fun splitRecords(content: String): List<String> {
        val records = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < content.length) {
            val c = content[i]
            when {
                c == '"' -> {
                    inQuotes = !inQuotes
                    current.append(c)
                }
                (c == '\n' || c == '\r') && !inQuotes -> {
                    if (c == '\r' && i + 1 < content.length && content[i + 1] == '\n') i++
                    records += current.toString()
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        if (current.isNotEmpty()) records += current.toString()
        return records.filter { it.isNotBlank() }
    }

    /** RFC 4180 field split: commas inside quotes stay, `""` is a literal quote. */
    private fun splitFields(record: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < record.length) {
            val c = record[i]
            when {
                inQuotes && c == '"' && i + 1 < record.length && record[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    fields += current.toString()
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        fields += current.toString()
        return fields
    }
}
