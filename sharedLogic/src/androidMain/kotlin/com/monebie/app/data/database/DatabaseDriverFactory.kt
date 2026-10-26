package com.monebie.app.data.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.monebie.app.db.MoneBieDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(MoneBieDatabase.Schema, context, MONEBIE_DATABASE_NAME)
    }
}
