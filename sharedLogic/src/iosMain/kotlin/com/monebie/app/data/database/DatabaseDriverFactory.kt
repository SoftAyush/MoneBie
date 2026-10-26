package com.monebie.app.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.monebie.app.db.MoneBieDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(MoneBieDatabase.Schema, MONEBIE_DATABASE_NAME)
    }
}
