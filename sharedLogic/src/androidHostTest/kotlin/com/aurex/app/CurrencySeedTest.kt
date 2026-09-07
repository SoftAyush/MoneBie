package com.aurex.app

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.aurex.app.db.AurexDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Holds the currency seed to its one real invariant.
 *
 * The list is written twice - in Currency.sq for fresh installs, and in 6.sqm
 * for databases that already exist - because SQLDelight runs the one on create
 * and the other on upgrade, and neither can include the other. Duplicated data
 * drifts silently: someone adds a currency to the .sq, an upgrading user never
 * sees it, and the only symptom is a missing row in a picker months later.
 *
 * So the check here is not that the files match textually, but that a database
 * built from scratch and a database migrated from v1 end up holding exactly the
 * same currencies.
 */
class CurrencySeedTest {

    /** The schema as it shipped before any of the module phases. */
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

    private fun rows(driver: SqlDriver): List<Currency> {
        val out = mutableListOf<Currency>()
        driver.executeQuery(
            identifier = null,
            sql = "SELECT code, symbol, name, flag, popular FROM Currency ORDER BY code",
            mapper = { cursor ->
                QueryResult.Value(
                    buildList {
                        while (cursor.next().value) {
                            add(
                                Currency(
                                    code = cursor.getString(0).orEmpty(),
                                    symbol = cursor.getString(1).orEmpty(),
                                    name = cursor.getString(2).orEmpty(),
                                    flag = cursor.getString(3).orEmpty(),
                                    popular = cursor.getLong(4) ?: 0L
                                )
                            )
                        }
                    }.also { out.addAll(it) }
                )
            },
            parameters = 0
        )
        return out
    }

    private data class Currency(
        val code: String,
        val symbol: String,
        val name: String,
        val flag: String,
        val popular: Long
    )

    @Test
    fun anUpgradedInstallEndsUpWithTheSameCurrenciesAsAFreshOne() {
        val fresh = freshDriver()
        val upgraded = driverAtVersion1().also {
            AurexDatabase.Schema.migrate(it, 1L, AurexDatabase.Schema.version).value
        }

        assertEquals(
            rows(fresh),
            rows(upgraded),
            "the seed in Currency.sq and the one in 6.sqm have drifted apart"
        )

        fresh.close()
        upgraded.close()
    }

    @Test
    fun everyCurrencyHasASymbolAndAFlag() {
        val driver = freshDriver()

        rows(driver).forEach { currency ->
            assertTrue(
                currency.symbol.isNotBlank(),
                "${currency.code} has no symbol; the amount fields prefix with it"
            )
            assertTrue(currency.flag.isNotBlank(), "${currency.code} has no flag")
            assertTrue(currency.name.isNotBlank(), "${currency.code} has no name")
            assertEquals(
                3,
                currency.code.length,
                "${currency.code} is not a three-letter ISO 4217 code"
            )
        }

        driver.close()
    }

    @Test
    fun theSeedCoversTheWorldAndKeepsTheOriginalTwenty() {
        val driver = freshDriver()
        val byCode = rows(driver).associateBy { it.code }

        assertTrue(
            byCode.size > 150,
            "expected the full ISO 4217 set, found only ${byCode.size}"
        )

        // The codes that shipped before the widening. A user is already pegged
        // to one of these, and losing the row would orphan their profile.
        listOf(
            "USD", "EUR", "GBP", "INR", "JPY", "CAD", "AUD", "NPR",
            "CHF", "BRL", "CNY", "KRW", "SGD", "HKD", "MXN", "ZAR",
            "SEK", "NOK", "DKK", "NZD"
        ).forEach { code ->
            assertTrue(code in byCode, "$code disappeared from the seed")
        }

        // Codes the canonical list added on top of the original twenty.
        listOf("ANG", "HRK", "TVD", "ZWL").forEach { code ->
            assertTrue(code in byCode, "$code from the canonical list was not seeded")
        }

        // The eight the picker floats to the top must stay exactly eight.
        assertEquals(
            listOf("AUD", "CAD", "EUR", "GBP", "INR", "JPY", "NPR", "USD"),
            byCode.values.filter { it.popular == 1L }.map { it.code }.sorted()
        )

        driver.close()
    }

    @Test
    fun aStaleSymbolIsCorrectedInPlaceWithoutOrphaningTheProfile() {
        val driver = driverAtVersion1()
        // How the first release seeded it: the wrong symbol, and a profile
        // pegged to it by foreign key.
        driver.execute(
            null,
            "INSERT INTO Currency(code, symbol, name, flag, popular) " +
                "VALUES ('NPR', 'Rs', 'Nepalese Rupee', 'NP', 1)",
            0
        )
        driver.execute(
            null,
            "INSERT INTO User(name, date, currency_code) VALUES ('Ayush', '2026-01-01', 'NPR')",
            0
        )

        AurexDatabase.Schema.migrate(driver, 1L, AurexDatabase.Schema.version).value

        // Updated in place - one row, canonical symbol, same primary key.
        val npr = rows(driver).single { it.code == "NPR" }
        assertEquals("रू", npr.symbol, "the stale symbol was left behind")
        assertEquals("🇳🇵", npr.flag)

        // The row was updated, never deleted and re-inserted, so the profile
        // still resolves through the foreign key.
        var joined: String? = null
        driver.executeQuery(
            identifier = null,
            sql = "SELECT c.symbol FROM User u JOIN Currency c ON c.code = u.currency_code",
            mapper = { cursor ->
                QueryResult.Value(
                    if (cursor.next().value) cursor.getString(0) else null
                ).also { joined = it.value }
            },
            parameters = 0
        )
        assertEquals("रू", joined, "the profile lost its currency in the migration")

        driver.close()
    }
}
