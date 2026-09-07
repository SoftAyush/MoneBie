package com.aurex.app.data.repository

import com.aurex.app.core.getCurrentDateString
import com.aurex.app.data.database.DatabaseHelper
import com.aurex.app.db.AurexDatabase
import com.aurex.app.domain.model.backup.BackupAccount
import com.aurex.app.domain.model.backup.BackupCategory
import com.aurex.app.domain.model.backup.BackupDebt
import com.aurex.app.domain.model.backup.BackupDebtPayment
import com.aurex.app.domain.model.backup.BackupEmi
import com.aurex.app.domain.model.backup.BackupEmiPayment
import com.aurex.app.domain.model.backup.BackupLoanRecord
import com.aurex.app.domain.model.backup.BackupLoanRepayment
import com.aurex.app.domain.model.backup.BackupSnapshot
import com.aurex.app.domain.model.backup.BackupTransaction
import com.aurex.app.domain.model.backup.BackupUser
import com.aurex.app.domain.repository.BackupRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupRepositoryImpl(
    private val dbHelper: DatabaseHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : BackupRepository {

    /**
     * Reads every table inside one transaction.
     *
     * Ten separate reads would let a transaction posted midway through land in
     * one list but not in the list of the account balance it moved, producing a
     * backup that never reflected any state the app was actually in.
     */
    override suspend fun snapshot(): BackupSnapshot = withContext(ioDispatcher) {
        dbHelper.database.transactionWithResult {
            BackupSnapshot(
                schemaVersion = AurexDatabase.Schema.version,
                createdAt = getCurrentDateString(),
                user = dbHelper.userQueries.selectAllUsers().executeAsList().firstOrNull()?.let {
                    BackupUser(
                        name = it.name,
                        date = it.date,
                        currencyCode = it.currency_code,
                        themeMode = it.theme_mode
                    )
                },
                accounts = dbHelper.accountQueries.selectAllAccounts().executeAsList().map {
                    BackupAccount(
                        id = it.id,
                        name = it.name,
                        type = it.type,
                        balanceMinor = it.balance_minor,
                        currencyCode = it.currency_code,
                        isArchived = it.is_archived,
                        createdAt = it.created_at
                    )
                },
                categories = dbHelper.categoryQueries.selectAllCategories().executeAsList().map {
                    BackupCategory(
                        id = it.id,
                        name = it.name,
                        iconKey = it.icon_key,
                        appliesTo = it.applies_to,
                        isBuiltIn = it.is_built_in,
                        isProtected = it.is_protected,
                        isArchived = it.is_archived,
                        sortOrder = it.sort_order,
                        createdAt = it.created_at
                    )
                },
                transactions = dbHelper.transactionQueries.selectAllTransactions().executeAsList().map {
                    BackupTransaction(
                        id = it.id,
                        amountMinor = it.amount_minor,
                        type = it.type,
                        categoryId = it.category_id,
                        accountId = it.account_id,
                        transferAccountId = it.transfer_account_id,
                        date = it.date,
                        note = it.note,
                        paymentMethod = it.payment_method,
                        sourceType = it.source_type,
                        sourceReferenceId = it.source_reference_id,
                        sourcePaymentId = it.source_payment_id
                    )
                },
                debts = dbHelper.debtQueries.backupSelectAllDebts().executeAsList().map {
                    BackupDebt(
                        id = it.id,
                        name = it.name,
                        lender = it.lender,
                        originalAmountMinor = it.original_amount_minor,
                        interestRate = it.interest_rate,
                        startDate = it.start_date,
                        dueDate = it.due_date,
                        paymentFrequency = it.payment_frequency,
                        installmentAmountMinor = it.installment_amount_minor,
                        notes = it.notes,
                        defaultAccountId = it.default_account_id,
                        createdAt = it.created_at
                    )
                },
                debtPayments = dbHelper.debtQueries.backupSelectAllDebtPayments().executeAsList().map {
                    BackupDebtPayment(
                        id = it.id,
                        debtId = it.debt_id,
                        amountMinor = it.amount_minor,
                        date = it.date,
                        accountId = it.account_id,
                        transactionId = it.transaction_id,
                        note = it.note
                    )
                },
                emis = dbHelper.emiQueries.backupSelectAllEmis().executeAsList().map {
                    BackupEmi(
                        id = it.id,
                        name = it.name,
                        provider = it.provider,
                        principalAmountMinor = it.principal_amount_minor,
                        totalPayableAmountMinor = it.total_payable_amount_minor,
                        monthlyAmountMinor = it.monthly_amount_minor,
                        interestRate = it.interest_rate,
                        startDate = it.start_date,
                        tenureYears = it.tenure_years,
                        paymentAccountId = it.payment_account_id,
                        notes = it.notes,
                        createdAt = it.created_at
                    )
                },
                emiPayments = dbHelper.emiQueries.backupSelectAllEmiPayments().executeAsList().map {
                    BackupEmiPayment(
                        id = it.id,
                        emiId = it.emi_id,
                        installmentNumber = it.installment_number,
                        amountMinor = it.amount_minor,
                        dueDate = it.due_date,
                        paidDate = it.paid_date,
                        accountId = it.account_id,
                        transactionId = it.transaction_id
                    )
                },
                loanRecords = dbHelper.borrowLendQueries.backupSelectAllLoanRecords().executeAsList().map {
                    BackupLoanRecord(
                        id = it.id,
                        personName = it.person_name,
                        direction = it.direction,
                        principalAmountMinor = it.principal_amount_minor,
                        date = it.date,
                        dueDate = it.due_date,
                        notes = it.notes,
                        accountId = it.account_id,
                        principalTransactionId = it.principal_transaction_id,
                        createdAt = it.created_at
                    )
                },
                loanRepayments = dbHelper.borrowLendQueries.backupSelectAllLoanRepayments().executeAsList().map {
                    BackupLoanRepayment(
                        id = it.id,
                        recordId = it.record_id,
                        amountMinor = it.amount_minor,
                        date = it.date,
                        accountId = it.account_id,
                        transactionId = it.transaction_id,
                        note = it.note
                    )
                }
            )
        }
    }

    /**
     * Clears the user's tables and writes the snapshot's rows in their place.
     *
     * One transaction, so a failure part way through rolls back to the data the
     * user already had rather than leaving them with neither their own ledger
     * nor the backed-up one.
     *
     * Deletes run children first and inserts run parents first. Foreign keys are
     * not enforced by default on either driver, so nothing would complain about
     * the wrong order at the time - it would surface later as a payment pointing
     * at a debt that no longer exists.
     *
     * The Currency table is untouched: it is reference data seeded by the
     * schema, and the restored rows reference it by code, which is stable.
     */
    override suspend fun restore(snapshot: BackupSnapshot) = withContext(ioDispatcher) {
        dbHelper.database.transaction {
            dbHelper.transactionQueries.backupDeleteAllTransactions()
            dbHelper.debtQueries.backupDeleteAllDebtPayments()
            dbHelper.emiQueries.backupDeleteAllEmiPayments()
            dbHelper.borrowLendQueries.backupDeleteAllLoanRepayments()
            dbHelper.debtQueries.backupDeleteAllDebts()
            dbHelper.emiQueries.backupDeleteAllEmis()
            dbHelper.borrowLendQueries.backupDeleteAllLoanRecords()
            dbHelper.accountQueries.backupDeleteAllAccounts()
            dbHelper.categoryQueries.backupDeleteAllCategories()
            dbHelper.userQueries.backupDeleteAllUsers()

            snapshot.user?.let {
                dbHelper.userQueries.backupInsertUser(
                    name = it.name,
                    date = it.date,
                    currency_code = it.currencyCode,
                    theme_mode = it.themeMode
                )
            }

            snapshot.categories.forEach {
                dbHelper.categoryQueries.backupInsertCategory(
                    id = it.id,
                    name = it.name,
                    icon_key = it.iconKey,
                    applies_to = it.appliesTo,
                    is_built_in = it.isBuiltIn,
                    is_protected = it.isProtected,
                    is_archived = it.isArchived,
                    sort_order = it.sortOrder,
                    created_at = it.createdAt
                )
            }

            snapshot.accounts.forEach {
                dbHelper.accountQueries.backupInsertAccount(
                    id = it.id,
                    name = it.name,
                    type = it.type,
                    balance_minor = it.balanceMinor,
                    currency_code = it.currencyCode,
                    is_archived = it.isArchived,
                    created_at = it.createdAt
                )
            }

            snapshot.debts.forEach {
                dbHelper.debtQueries.backupInsertDebt(
                    id = it.id,
                    name = it.name,
                    lender = it.lender,
                    original_amount_minor = it.originalAmountMinor,
                    interest_rate = it.interestRate,
                    start_date = it.startDate,
                    due_date = it.dueDate,
                    payment_frequency = it.paymentFrequency,
                    installment_amount_minor = it.installmentAmountMinor,
                    notes = it.notes,
                    default_account_id = it.defaultAccountId,
                    created_at = it.createdAt
                )
            }

            snapshot.emis.forEach {
                dbHelper.emiQueries.backupInsertEmi(
                    id = it.id,
                    name = it.name,
                    provider = it.provider,
                    principal_amount_minor = it.principalAmountMinor,
                    total_payable_amount_minor = it.totalPayableAmountMinor,
                    monthly_amount_minor = it.monthlyAmountMinor,
                    interest_rate = it.interestRate,
                    start_date = it.startDate,
                    tenure_years = it.tenureYears,
                    payment_account_id = it.paymentAccountId,
                    notes = it.notes,
                    created_at = it.createdAt
                )
            }

            snapshot.loanRecords.forEach {
                dbHelper.borrowLendQueries.backupInsertLoanRecord(
                    id = it.id,
                    person_name = it.personName,
                    direction = it.direction,
                    principal_amount_minor = it.principalAmountMinor,
                    date = it.date,
                    due_date = it.dueDate,
                    notes = it.notes,
                    account_id = it.accountId,
                    principal_transaction_id = it.principalTransactionId,
                    created_at = it.createdAt
                )
            }

            snapshot.transactions.forEach {
                dbHelper.transactionQueries.backupInsertTransaction(
                    id = it.id,
                    amount_minor = it.amountMinor,
                    type = it.type,
                    category_id = it.categoryId,
                    account_id = it.accountId,
                    transfer_account_id = it.transferAccountId,
                    date = it.date,
                    note = it.note,
                    payment_method = it.paymentMethod,
                    source_type = it.sourceType,
                    source_reference_id = it.sourceReferenceId,
                    source_payment_id = it.sourcePaymentId
                )
            }

            snapshot.debtPayments.forEach {
                dbHelper.debtQueries.backupInsertDebtPayment(
                    id = it.id,
                    debt_id = it.debtId,
                    amount_minor = it.amountMinor,
                    date = it.date,
                    account_id = it.accountId,
                    transaction_id = it.transactionId,
                    note = it.note
                )
            }

            snapshot.emiPayments.forEach {
                dbHelper.emiQueries.backupInsertEmiPayment(
                    id = it.id,
                    emi_id = it.emiId,
                    installment_number = it.installmentNumber,
                    amount_minor = it.amountMinor,
                    due_date = it.dueDate,
                    paid_date = it.paidDate,
                    account_id = it.accountId,
                    transaction_id = it.transactionId
                )
            }

            snapshot.loanRepayments.forEach {
                dbHelper.borrowLendQueries.backupInsertLoanRepayment(
                    id = it.id,
                    record_id = it.recordId,
                    amount_minor = it.amountMinor,
                    date = it.date,
                    account_id = it.accountId,
                    transaction_id = it.transactionId,
                    note = it.note
                )
            }
        }
    }
}
