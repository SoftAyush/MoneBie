package com.aurex.app

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.aurex.app.db.AurexDatabase
import com.aurex.app.domain.model.TransactionCategories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Holds the category seed to its invariants.
 *
 * The built-in set is written three times - in Category.sq for a fresh install,
 * in 7.sqm for a database that already exists, and in TransactionCategories as
 * the fallback the app resolves an unknown id against. Duplicated data drifts
 * silently: someone renames a category in one place, an upgrading user never
 * sees it, and the only symptom is a stale label in a picker months later.
 *
 * So the checks here are that a database built from scratch, a database
 * migrated from v1, and the code's own list all describe the same categories.
 */
class CategorySeedTest {

    private fun driverAtVersion1(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            null,
            """
            CREATE TABLE Currency (
                code    TEXT    NOT NULL PRIMARY KEY,
                symbol  TEXT    NOT NULL,
                name    TEXT    NOT NULL,
                flag    TEXT    NOT NULL DEFAULT '',
                popular INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
            0
        )
        driver.execute(
            null,
            """
            CREATE TABLE User (
                name          TEXT NOT NULL,
                date          TEXT NOT NULL,
                currency_code TEXT NOT NULL,
                FOREIGN KEY (currency_code) REFERENCES Currency(code) ON UPDATE CASCADE
            )
            """.trimIndent(),
            0
        )
        driver.execute(null, "PRAGMA user_version = 1", 0)
        return driver
    }

    private fun freshDriver(): SqlDriver =
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
            AurexDatabase.Schema.create(it).value
        }

    private fun migratedDriver(): SqlDriver =
        driverAtVersion1().also {
            AurexDatabase.Schema.migrate(it, 1L, AurexDatabase.Schema.version).value
        }

    /** id, name, icon, side, built-in and protected flags - everything that matters. */
    private fun categories(driver: SqlDriver): List<String> {
        val rows = mutableListOf<String>()
        driver.executeQuery(
            identifier = null,
            sql = "SELECT id, name, icon_key, applies_to, is_built_in, is_protected " +
                "FROM Category ORDER BY id",
            mapper = { cursor ->
                QueryResult.Value(
                    buildList {
                        while (cursor.next().value) {
                            add(
                                listOf(
                                    cursor.getString(0),
                                    cursor.getString(1),
                                    cursor.getString(2),
                                    cursor.getString(3),
                                    cursor.getLong(4)?.toString(),
                                    cursor.getLong(5)?.toString()
                                ).joinToString("|")
                            )
                        }
                    }.also { rows.addAll(it) }
                )
            },
            parameters = 0
        )
        return rows
    }

    @Test
    fun afreshInstallAndAnUpgradeSeedTheSameCategories() {
        val fresh = freshDriver()
        val migrated = migratedDriver()

        assertEquals(
            categories(fresh),
            categories(migrated),
            "Category.sq and 7.sqm have drifted apart"
        )

        fresh.close()
        migrated.close()
    }

    @Test
    fun theSeededSetMatchesTheCodesOwnList() {
        val driver = freshDriver()

        val fromDatabase = categories(driver)
        val fromCode = TransactionCategories.all
            .sortedBy { it.id }
            .map {
                listOf(
                    it.id,
                    it.name,
                    it.iconKey,
                    it.appliesTo.name,
                    if (it.isBuiltIn) "1" else "0",
                    if (it.isProtected) "1" else "0"
                ).joinToString("|")
            }

        assertEquals(
            fromCode,
            fromDatabase,
            "TransactionCategories and the seeded table disagree"
        )

        driver.close()
    }

    @Test
    fun everyCategoryTheAppPostsToIsProtected() {
        // These ids are written directly by the debt, EMI and borrow/lend
        // repositories. If one of them could be deleted, those postings would
        // point at a category that no longer exists.
        val postedTo = listOf(
            "debt_payment",
            "emi_payment",
            "borrowed",
            "lending",
            "loan_repayment",
            "loan_returned",
            "transfer"
        )

        val driver = freshDriver()
        val protectedIds = categories(driver)
            .filter { it.split("|").last() == "1" }
            .map { it.substringBefore("|") }

        postedTo.forEach { id ->
            assertTrue(id in protectedIds, "$id is posted to by the app but is not protected")
        }

        driver.close()
    }
}
