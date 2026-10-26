package com.monebie.app.domain.model.backup

/**
 * Everything the user has entered, in one transferable value.
 *
 * The rows are carried as flat mirrors of the tables rather than as the domain
 * models. A domain model is shaped for the screen that reads it - [
 * com.monebie.app.domain.model.Debt] arrives with its payments already rolled up
 * into an amount paid, and [com.monebie.app.domain.model.Account] hides the
 * archived flag behind computed properties. Restoring from those would mean
 * reconstructing the rows they were derived from, and any rollup that was
 * stored as well as derived could come back disagreeing with its own detail.
 * Mirroring the tables keeps a restore a copy rather than a recalculation.
 *
 * The Currency table is deliberately absent. It is a fixed reference list
 * seeded from the schema on every install, identical on every device and
 * roughly 160 rows - backing it up would upload the same constant list for
 * every user, and restoring it could only ever overwrite an identical copy.
 * What is personal about the currency choice is the code held on the user row,
 * which is carried.
 */
data class BackupSnapshot(
    /**
     * Version of this wire format, not of the app.
     *
     * Bumped only when the shape below changes in a way an older app could not
     * read. Kept separate from [schemaVersion] because the two move for
     * different reasons: a database migration that adds an index changes the
     * schema version without changing anything here.
     */
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    /** Database schema the snapshot was taken from, for diagnostics on restore. */
    val schemaVersion: Long,
    /** When the snapshot was taken, in the app's usual `yyyy-MM-dd HH:mm:ss`. */
    val createdAt: String,
    val user: BackupUser?,
    val accounts: List<BackupAccount>,
    val categories: List<BackupCategory>,
    val transactions: List<BackupTransaction>,
    val debts: List<BackupDebt>,
    val debtPayments: List<BackupDebtPayment>,
    val emis: List<BackupEmi>,
    val emiPayments: List<BackupEmiPayment>,
    val loanRecords: List<BackupLoanRecord>,
    val loanRepayments: List<BackupLoanRepayment>,
    /*
     * Added in 1.1.0. Defaulted so a backup written before then, which has no
     * such collections, reads back as one with none rather than failing.
     */
    val budgets: List<BackupBudget> = emptyList(),
    val recurringTransactions: List<BackupRecurringTransaction> = emptyList()
) {

    /**
     * Rows the user would notice the loss of.
     *
     * Categories are excluded: a fresh install seeds its own built-ins, so a
     * snapshot of a brand new profile would otherwise report a couple of dozen
     * rows and read as though there were something to lose.
     */
    val meaningfulRowCount: Int
        get() = accounts.size + transactions.size + debts.size + debtPayments.size +
            emis.size + emiPayments.size + loanRecords.size + loanRepayments.size +
            budgets.size + recurringTransactions.size

    /** Every row, including the seeded ones. What the upload actually writes. */
    val totalRowCount: Int
        get() = meaningfulRowCount + categories.size + (if (user == null) 0 else 1)

    val isEmpty: Boolean get() = meaningfulRowCount == 0

    companion object {
        const val CURRENT_FORMAT_VERSION: Int = 1
    }
}

/** The single profile row: name, joining date, chosen currency and palette. */
data class BackupUser(
    val name: String,
    val date: String,
    val currencyCode: String,
    val themeMode: String
)

data class BackupAccount(
    val id: Long,
    val name: String,
    val type: String,
    val balanceMinor: Long,
    val currencyCode: String,
    val isArchived: Long,
    val createdAt: String
)

data class BackupCategory(
    val id: String,
    val name: String,
    val iconKey: String,
    val appliesTo: String,
    val isBuiltIn: Long,
    val isProtected: Long,
    val isArchived: Long,
    val sortOrder: Long,
    val createdAt: String
)

data class BackupTransaction(
    val id: Long,
    val amountMinor: Long,
    val type: String,
    val categoryId: String,
    val accountId: Long,
    val transferAccountId: Long?,
    val date: String,
    val note: String?,
    val paymentMethod: String,
    val sourceType: String,
    val sourceReferenceId: Long?,
    val sourcePaymentId: Long?
)

data class BackupDebt(
    val id: Long,
    val name: String,
    val lender: String,
    val originalAmountMinor: Long,
    val interestRate: Double?,
    val startDate: String,
    val dueDate: String?,
    val paymentFrequency: String,
    val installmentAmountMinor: Long?,
    val notes: String?,
    val defaultAccountId: Long?,
    val createdAt: String
)

data class BackupDebtPayment(
    val id: Long,
    val debtId: Long,
    val amountMinor: Long,
    val date: String,
    val accountId: Long?,
    val transactionId: Long?,
    val note: String?
)

data class BackupEmi(
    val id: Long,
    val name: String,
    val provider: String,
    val principalAmountMinor: Long,
    val totalPayableAmountMinor: Long,
    val monthlyAmountMinor: Long,
    val interestRate: Double?,
    val startDate: String,
    val tenureYears: Double,
    val paymentAccountId: Long?,
    val notes: String?,
    val createdAt: String
)

data class BackupEmiPayment(
    val id: Long,
    val emiId: Long,
    val installmentNumber: Long,
    val amountMinor: Long,
    val dueDate: String,
    val paidDate: String,
    val accountId: Long?,
    val transactionId: Long?
)

data class BackupLoanRecord(
    val id: Long,
    val personName: String,
    val direction: String,
    val principalAmountMinor: Long,
    val date: String,
    val dueDate: String?,
    val notes: String?,
    val accountId: Long?,
    val principalTransactionId: Long?,
    val createdAt: String
)

data class BackupLoanRepayment(
    val id: Long,
    val recordId: Long,
    val amountMinor: Long,
    val date: String,
    val accountId: Long?,
    val transactionId: Long?,
    val note: String?
)

data class BackupBudget(
    val id: Long,
    val categoryId: String,
    val amountMinor: Long,
    val period: String,
    val createdAt: String
)

data class BackupRecurringTransaction(
    val id: Long,
    val name: String,
    val amountMinor: Long,
    val type: String,
    val categoryId: String,
    val accountId: Long,
    val transferAccountId: Long?,
    val paymentMethod: String,
    val frequency: String,
    val startDate: String,
    val nextDueDate: String,
    val endDate: String?,
    val note: String?,
    val isPaused: Long,
    val createdAt: String
)
