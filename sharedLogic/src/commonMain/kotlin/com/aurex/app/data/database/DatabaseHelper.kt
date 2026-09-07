package com.aurex.app.data.database

import app.cash.sqldelight.db.SqlDriver
import com.aurex.app.db.AurexDatabase

class DatabaseHelper(driver: SqlDriver) {
    val database = AurexDatabase(driver)
    val userQueries = database.userQueries
    val currencyQueries = database.currencyQueries
    val accountQueries = database.accountQueries
    val transactionQueries = database.transactionRecordQueries
    val emiQueries = database.emiQueries
    val borrowLendQueries = database.borrowLendQueries
    val debtQueries = database.debtQueries
    val categoryQueries = database.categoryQueries
}
