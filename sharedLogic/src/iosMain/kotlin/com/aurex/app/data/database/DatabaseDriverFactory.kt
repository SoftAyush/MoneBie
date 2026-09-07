package com.aurex.app.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.aurex.app.db.AurexDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(AurexDatabase.Schema, AUREX_DATABASE_NAME)
    }
}
