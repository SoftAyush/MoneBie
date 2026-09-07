package com.aurex.app

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.aurex.app.db.AurexDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises the schema migrations end to end.
 *
 * Every phase added tables to a database that real users already have data in,
 * and a broken migration bricks the app on launch for exactly those users - the
 * ones who cannot simply reinstall without losing everything. A fresh install
 * never runs these statements, so nothing else in the suite covers them.
 */
class SchemaMigrationTest {

    /** The schema as it shipped before any of the module phases: profile only. */
    private val version1Schema = listOf(
        """
        CREATE TABLE Currency (
            code    TEXT    NOT NULL PRIMARY KEY,
            symbol  TEXT    NOT NULL,
            name    TEXT    NOT NULL,
            flag    TEXT    NOT NULL DEFAULT '',
            popular INTEGER NOT NULL DEFAULT 0
        )
        """.trimIndent(),
        """
        CREATE TABLE User (
            name          TEXT NOT NULL,
            date          TEXT NOT NULL,
            currency_code TEXT NOT NULL,
            FOREIGN KEY (currency_code) REFERENCES Currency(code) ON UPDATE CASCADE
        )
        """.trimIndent()
    )

    private fun driverAtVersion1(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        version1Schema.forEach { driver.execute(null, it, 0) }
        driver.execute(null, "PRAGMA user_version = 1", 0)
        return driver
    }

    private fun tableNames(driver: SqlDriver): Set<String> {
        val names = mutableSetOf<String>()
        driver.executeQuery(
            identifier = null,
            sql = "SELECT name FROM sqlite_master WHERE type = 'table'",
            mapper = { cursor ->
                app.cash.sqldelight.db.QueryResult.Value(
                    buildList { while (cursor.next().value) add(cursor.getString(0).orEmpty()) }
                        .also { names.addAll(it) }
                )
            },
            parameters = 0
        )
        return names
    }

    @Test
    fun theCurrentSchemaIsVersionNine() {
        // Guards against adding a .sqm without the version moving, which would
        // leave existing installs on an older schema with no migration run.
        assertEquals(9L, AurexDatabase.Schema.version)
    }

    @Test
    fun anInstallFromTheFirstReleaseMigratesAllTheWayUp() {
        val driver = driverAtVersion1()

        AurexDatabase.Schema.migrate(driver, 1L, AurexDatabase.Schema.version).value

        val tables = tableNames(driver)
        listOf(
            "User", "Currency",          // pre-existing, must survive
            "Account",                   // v2
            "TransactionRecord",         // v3
            "Debt", "DebtPayment",       // v4
            "Emi", "EmiPayment",         // v5
            "LoanRecord", "LoanRepayment", // v6
            // v7 seeds rows only - it adds no tables
            "Category"                    // v8
            // v9 adds a column to User - no new tables
        ).forEach { table ->
            assertTrue(table in tables, "migration lost or never created $table (have: $tables)")
        }

        driver.close()
    }

    @Test
    fun everyIntermediateStartingPointReachesTheSameSchema() {
        // Users are spread across whichever version they last updated on, so
        // each starting point has to arrive at the identical end state.
        val target = AurexDatabase.Schema.version
        val fromV1 = driverAtVersion1().also {
            AurexDatabase.Schema.migrate(it, 1L, target).value
        }
        val expected = tableNames(fromV1)

        (2L until target).forEach { start ->
            val driver = driverAtVersion1()
            // Walk up to the intermediate point, then finish the journey.
            AurexDatabase.Schema.migrate(driver, 1L, start).value
            AurexDatabase.Schema.migrate(driver, start, target).value

            assertEquals(
                expected,
                tableNames(driver),
                "starting from v$start produced a different schema"
            )
            driver.close()
        }
        fromV1.close()
    }

    @Test
    fun existingRowsSurviveTheMigration() {
        val driver = driverAtVersion1()
        driver.execute(
            null,
            "INSERT INTO Currency(code, symbol, name, flag, popular) VALUES ('NPR', 'Rs', 'Nepalese Rupee', 'NP', 1)",
            0
        )
        driver.execute(
            null,
            "INSERT INTO User(name, date, currency_code) VALUES ('Ayush', '2026-01-01 00:00:00', 'NPR')",
            0
        )

        AurexDatabase.Schema.migrate(driver, 1L, AurexDatabase.Schema.version).value

        // The whole point of migrating rather than recreating: the profile the
        // user set up on day one is still there afterwards.
        var name: String? = null
        driver.executeQuery(
            identifier = null,
            sql = "SELECT name FROM User LIMIT 1",
            mapper = { cursor ->
                app.cash.sqldelight.db.QueryResult.Value(
                    if (cursor.next().value) cursor.getString(0) else null
                ).also { name = it.value }
            },
            parameters = 0
        )
        assertEquals("Ayush", name)

        driver.close()
    }
}
