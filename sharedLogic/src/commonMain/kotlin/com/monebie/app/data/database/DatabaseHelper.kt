package com.monebie.app.data.database

import app.cash.sqldelight.db.SqlDriver
import com.monebie.app.db.MoneBieDatabase

class DatabaseHelper(driver: SqlDriver) {
    val database = MoneBieDatabase(driver)
    val userQueries = database.userQueries
    val currencyQueries = database.currencyQueries
    val accountQueries = database.accountQueries
    val transactionQueries = database.transactionRecordQueries
    val emiQueries = database.emiQueries
    val borrowLendQueries = database.borrowLendQueries
    val debtQueries = database.debtQueries
    val categoryQueries = database.categoryQueries
    val budgetQueries = database.budgetQueries
    val recurringQueries = database.recurringTransactionQueries
}
