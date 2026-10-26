package com.monebie.app

import com.monebie.app.core.Money
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.AccountType
import com.monebie.app.domain.model.CategoryLookup
import com.monebie.app.domain.model.PaymentMethod
import com.monebie.app.domain.model.Transaction
import com.monebie.app.domain.model.TransactionCategories
import com.monebie.app.domain.model.TransactionType
import com.monebie.app.domain.usecase.csvimport.CsvImportPlanner
import com.monebie.app.domain.usecase.csvimport.TransactionCsvParser
import com.monebie.app.domain.usecase.export.TransactionCsv
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The importer against the exporter, and against the kind of file a person
 * writes by hand.
 */
class CsvImportTest {

    private val everyday = Account(
        id = 7L, name = "Everyday", type = AccountType.BANK, balance = Money.ZERO,
        currencyCode = "NPR", createdAt = ""
    )

    // -------------------------------------------------------------------------
    // Parsing
    // -------------------------------------------------------------------------

    @Test
    fun theExportReadsStraightBackIn() {
        val exported = TransactionCsv.build(
            transactions = listOf(
                Transaction(
                    id = 1L, amount = Money.fromMajor(250L), type = TransactionType.EXPENSE,
                    categoryId = "food", accountId = 7L, date = "2026-09-04 12:30:00",
                    note = "Lunch, with a comma", paymentMethod = PaymentMethod.CARD
                ),
                Transaction(
                    id = 2L, amount = Money.fromMajor(50_000L), type = TransactionType.INCOME,
                    categoryId = "salary", accountId = 7L, date = "2026-09-01 09:00:00",
                    paymentMethod = PaymentMethod.BANK_TRANSFER
                )
            ),
            accounts = listOf(everyday),
            categories = CategoryLookup.Seeded
        )

        val parsed = TransactionCsvParser.parse(exported.content)

        assertTrue(parsed.errors.isEmpty(), "errors: ${parsed.errors}")
        assertEquals(2, parsed.rows.size)

        val lunch = parsed.rows.first { it.type == TransactionType.EXPENSE }
        assertEquals("2026-09-04", lunch.day)
        assertEquals("Food & Drink", lunch.categoryName)
        assertEquals("Everyday", lunch.accountName)
        assertEquals(Money.fromMajor(250L), lunch.amount)
        assertEquals(PaymentMethod.CARD, lunch.paymentMethod)
        assertEquals("Lunch, with a comma", lunch.note)

        val salary = parsed.rows.first { it.type == TransactionType.INCOME }
        assertEquals(Money.fromMajor(50_000L), salary.amount)
        assertEquals(PaymentMethod.BANK_TRANSFER, salary.paymentMethod)
    }

    @Test
    fun columnsAreFoundByNameNotPosition() {
        val content = """
            Amount,Account,Date,Type,Note
            12.50,Everyday,2026-09-04,expense,Coffee
        """.trimIndent()

        val row = TransactionCsvParser.parse(content).rows.single()
        assertEquals(Money(12_50L), row.amount)
        assertEquals(TransactionType.EXPENSE, row.type)
        assertEquals("Other", row.categoryName, "a missing category falls back to Other")
        assertEquals("Coffee", row.note)
        assertEquals(PaymentMethod.OTHER, row.paymentMethod)
    }

    @Test
    fun dayFirstDatesAndNegativeAmountsAreAccepted() {
        val content = """
            Date,Type,Amount,Account
            04/09/2026,Expense,-99,Everyday
            2026/09/05,Income,1000,Everyday
        """.trimIndent()

        val rows = TransactionCsvParser.parse(content).rows
        assertEquals("2026-09-04", rows[0].day)
        assertEquals(Money.fromMajor(99L), rows[0].amount, "the sign is carried by the type")
        assertEquals("2026-09-05", rows[1].day)
    }

    @Test
    fun badLinesAreReportedWithoutSinkingTheRest() {
        val content = """
            Date,Type,Amount,Account
            2026-09-04,Expense,10,Everyday
            not a date,Expense,10,Everyday
            2026-09-04,Sideways,10,Everyday
            2026-09-04,Expense,zero,Everyday
            2026-09-04,Transfer,10,Everyday
        """.trimIndent()

        val parsed = TransactionCsvParser.parse(content)
        assertEquals(1, parsed.rows.size)
        assertEquals(listOf(3, 4, 5, 6), parsed.errors.map { it.line })
    }

    @Test
    fun aFileWithoutTheRequiredColumnsIsRefusedOutright() {
        val parsed = TransactionCsvParser.parse("Name,Value\nx,1\n")
        assertTrue(parsed.rows.isEmpty())
        assertEquals(1, parsed.errors.single().line)
        assertTrue("Missing column" in parsed.errors.single().message)
    }

    @Test
    fun quotedFieldsMayHoldCommasQuotesAndNewlines() {
        val content = "Date,Type,Amount,Account,Note\n" +
            "2026-09-04,Expense,10,Everyday,\"Said \"\"hi\"\", then\nleft\"\n"

        val row = TransactionCsvParser.parse(content).rows.single()
        assertEquals("Said \"hi\", then\nleft", row.note)
    }

    // -------------------------------------------------------------------------
    // Planning
    // -------------------------------------------------------------------------

    @Test
    fun accountsMatchByNameIgnoringCaseAndCategoriesMatchOnTheirSide() {
        val parsed = TransactionCsvParser.parse(
            """
            Date,Type,Category,Account,Amount
            2026-09-04,Expense,food & drink,everyday,10
            2026-09-04,Expense,Vet bills,Old wallet,20
            2026-09-04,Income,Other,EVERYDAY,30
            """.trimIndent()
        )

        val plan = CsvImportPlanner.plan(parsed, listOf(everyday), TransactionCategories.all, emptyList())

        assertEquals(mapOf("everyday" to 7L), plan.matchedAccounts)
        assertEquals(listOf("Old wallet"), plan.unmatchedAccounts)
        // "Other" exists on both sides and must resolve on the income side, so
        // it is not new; "Vet bills" is.
        assertEquals(listOf("Vet bills"), plan.newCategories)
        assertTrue(!plan.isResolved)
    }

    @Test
    fun rowsAlreadyInTheLedgerAreFlaggedAsLikelyDuplicates() {
        val existing = Transaction(
            id = 1L, amount = Money.fromMajor(10L), type = TransactionType.EXPENSE,
            categoryId = "food", accountId = 7L, date = "2026-09-04 12:00:00"
        )
        val parsed = TransactionCsvParser.parse(
            """
            Date,Type,Category,Account,Amount
            2026-09-04,Expense,Food & Drink,Everyday,10
            2026-09-04,Expense,Food & Drink,Everyday,11
            """.trimIndent()
        )

        val plan = CsvImportPlanner.plan(parsed, listOf(everyday), TransactionCategories.all, listOf(existing))

        assertEquals(1, plan.likelyDuplicates.size)
        assertEquals(Money.fromMajor(10L), plan.likelyDuplicates.single().amount)
    }

    @Test
    fun findCategoryPrefersTheRightSideOfTheLedger() {
        val income = CsvImportPlanner.findCategory(TransactionCategories.all, "Other", TransactionType.INCOME)
        val expense = CsvImportPlanner.findCategory(TransactionCategories.all, "other", TransactionType.EXPENSE)
        assertEquals("other_income", income?.id)
        assertEquals("other_expense", expense?.id)
        assertNull(CsvImportPlanner.findCategory(TransactionCategories.all, "Salary", TransactionType.EXPENSE))
    }
}
