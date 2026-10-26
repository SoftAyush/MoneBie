package com.monebie.app.core

import app.cash.sqldelight.db.SqlDriver
import com.monebie.app.data.database.DatabaseHelper
import com.monebie.app.data.repository.AccountRepositoryImpl
import com.monebie.app.data.repository.BackupRepositoryImpl
import com.monebie.app.data.repository.BudgetRepositoryImpl
import com.monebie.app.data.repository.RecurringTransactionRepositoryImpl
import com.monebie.app.data.repository.UnavailableRemoteBackupStore
import com.monebie.app.data.repository.CategoryRepositoryImpl
import com.monebie.app.data.repository.CurrencyRepositoryImpl
import com.monebie.app.data.repository.BorrowLendRepositoryImpl
import com.monebie.app.data.repository.DebtRepositoryImpl
import com.monebie.app.data.repository.EmiRepositoryImpl
import com.monebie.app.data.repository.TransactionRepositoryImpl
import com.monebie.app.data.repository.UserRepositoryImpl
import com.monebie.app.domain.repository.AccountRepository
import com.monebie.app.domain.repository.BackupRepository
import com.monebie.app.domain.repository.BudgetRepository
import com.monebie.app.domain.repository.RecurringTransactionRepository
import com.monebie.app.domain.repository.RemoteBackupStore
import com.monebie.app.domain.repository.BorrowLendRepository
import com.monebie.app.domain.repository.CategoryRepository
import com.monebie.app.domain.repository.CurrencyRepository
import com.monebie.app.domain.repository.DebtRepository
import com.monebie.app.domain.repository.EmiRepository
import com.monebie.app.domain.repository.TransactionRepository
import com.monebie.app.domain.repository.UserRepository
import com.monebie.app.domain.usecase.account.ArchiveAccountUseCase
import com.monebie.app.domain.usecase.backup.BackUpNowUseCase
import com.monebie.app.domain.usecase.backup.ObserveBackupIdentityUseCase
import com.monebie.app.domain.usecase.backup.RestoreBackupUseCase
import com.monebie.app.domain.usecase.budget.DeleteBudgetUseCase
import com.monebie.app.domain.usecase.budget.GetBudgetOverviewUseCase
import com.monebie.app.domain.usecase.budget.GetBudgetsUseCase
import com.monebie.app.domain.usecase.budget.SaveBudgetUseCase
import com.monebie.app.domain.usecase.csvimport.CommitCsvImportUseCase
import com.monebie.app.domain.usecase.csvimport.PrepareCsvImportUseCase
import com.monebie.app.domain.usecase.recurring.DeleteRecurringTransactionUseCase
import com.monebie.app.domain.usecase.recurring.GetRecurringTransactionUseCase
import com.monebie.app.domain.usecase.recurring.GetRecurringTransactionsUseCase
import com.monebie.app.domain.usecase.recurring.PostDueRecurringUseCase
import com.monebie.app.domain.usecase.recurring.PostRecurringNowUseCase
import com.monebie.app.domain.usecase.recurring.SaveRecurringTransactionUseCase
import com.monebie.app.domain.usecase.recurring.SetRecurringPausedUseCase
import com.monebie.app.domain.usecase.recurring.SkipRecurringOccurrenceUseCase
import com.monebie.app.domain.usecase.account.CreateAccountUseCase
import com.monebie.app.domain.usecase.account.GetAccountUseCase
import com.monebie.app.domain.usecase.account.SaveAccountUseCase
import com.monebie.app.domain.usecase.account.GetAccountsUseCase
import com.monebie.app.domain.usecase.account.HasAccountUseCase
import com.monebie.app.domain.usecase.borrowlend.DeleteLoanRecordUseCase
import com.monebie.app.domain.usecase.borrowlend.DeleteLoanRepaymentUseCase
import com.monebie.app.domain.usecase.borrowlend.GetLoanRecordUseCase
import com.monebie.app.domain.usecase.borrowlend.GetLoanRecordsUseCase
import com.monebie.app.domain.usecase.borrowlend.GetLoanRepaymentsUseCase
import com.monebie.app.domain.usecase.borrowlend.RecordLoanRepaymentUseCase
import com.monebie.app.domain.usecase.borrowlend.SaveLoanRecordUseCase
import com.monebie.app.domain.usecase.category.CountCategoryUsageUseCase
import com.monebie.app.domain.usecase.category.GetActiveCategoriesUseCase
import com.monebie.app.domain.usecase.category.GetCategoriesUseCase
import com.monebie.app.domain.usecase.category.RemoveCategoryUseCase
import com.monebie.app.domain.usecase.category.RestoreCategoryUseCase
import com.monebie.app.domain.usecase.category.SaveCategoryUseCase
import com.monebie.app.domain.usecase.currency.GetCurrenciesUseCase
import com.monebie.app.domain.usecase.dashboard.GetDashboardSnapshotUseCase
import com.monebie.app.domain.usecase.debt.DeleteDebtPaymentUseCase
import com.monebie.app.domain.usecase.debt.DeleteDebtUseCase
import com.monebie.app.domain.usecase.debt.GetDebtPaymentsUseCase
import com.monebie.app.domain.usecase.debt.GetDebtUseCase
import com.monebie.app.domain.usecase.debt.GetDebtsUseCase
import com.monebie.app.domain.usecase.debt.RecordDebtPaymentUseCase
import com.monebie.app.domain.usecase.debt.SaveDebtUseCase
import com.monebie.app.domain.usecase.emi.DeleteEmiUseCase
import com.monebie.app.domain.usecase.export.ExportTransactionsUseCase
import com.monebie.app.domain.usecase.insights.GetSpendingInsightsUseCase
import com.monebie.app.domain.usecase.reminders.GetUpcomingPaymentsUseCase
import com.monebie.app.domain.usecase.emi.GetEmiPaymentsUseCase
import com.monebie.app.domain.usecase.emi.GetEmiUseCase
import com.monebie.app.domain.usecase.emi.GetEmisUseCase
import com.monebie.app.domain.usecase.emi.MarkInstallmentPaidUseCase
import com.monebie.app.domain.usecase.emi.SaveEmiUseCase
import com.monebie.app.domain.usecase.emi.UndoLastInstallmentUseCase
import com.monebie.app.domain.usecase.overview.GetFinancialOverviewUseCase
import com.monebie.app.domain.usecase.transaction.DeleteTransactionUseCase
import com.monebie.app.domain.usecase.transaction.GetRecentTransactionsUseCase
import com.monebie.app.domain.usecase.transaction.GetTransactionDefaultsUseCase
import com.monebie.app.domain.usecase.transaction.GetTransactionUseCase
import com.monebie.app.domain.usecase.transaction.GetTransactionsInRangeUseCase
import com.monebie.app.domain.usecase.transaction.SaveTransactionUseCase
import com.monebie.app.domain.usecase.user.GetCurrentUserUseCase
import com.monebie.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import com.monebie.app.domain.usecase.user.GetThemeModeUseCase
import com.monebie.app.domain.usecase.user.SetThemeModeUseCase
import com.monebie.app.domain.usecase.user.GetUsersUseCase
import com.monebie.app.domain.usecase.user.SaveUserUseCase

/**
 * @param remoteBackupStore where backups are sent. Defaults to the store that
 * reports cloud backup unavailable, which is what iOS gets until it has a
 * backend of its own; Android passes its Firestore-backed implementation in.
 * Injecting it here rather than reaching for Firebase inside the shared code is
 * what keeps this module buildable for a target the Firebase SDK does not
 * support.
 */
class AppContainer(
    val sqlDriver: SqlDriver,
    val remoteBackupStore: RemoteBackupStore = UnavailableRemoteBackupStore
) {
    val databaseHelper = DatabaseHelper(sqlDriver)

    // ---- Repositories ----------------------------------------------------
    val userRepository: UserRepository = UserRepositoryImpl(databaseHelper)
    val currencyRepository: CurrencyRepository = CurrencyRepositoryImpl(databaseHelper)
    val accountRepository: AccountRepository = AccountRepositoryImpl(databaseHelper)
    val categoryRepository: CategoryRepository = CategoryRepositoryImpl(databaseHelper)

    val transactionRepository: TransactionRepository = TransactionRepositoryImpl(databaseHelper)
    val debtRepository: DebtRepository = DebtRepositoryImpl(databaseHelper)
    val emiRepository: EmiRepository = EmiRepositoryImpl(databaseHelper)

    val borrowLendRepository: BorrowLendRepository = BorrowLendRepositoryImpl(databaseHelper)

    val backupRepository: BackupRepository = BackupRepositoryImpl(databaseHelper)

    val budgetRepository: BudgetRepository = BudgetRepositoryImpl(databaseHelper)
    val recurringTransactionRepository: RecurringTransactionRepository =
        RecurringTransactionRepositoryImpl(databaseHelper)

    // ---- Use cases -------------------------------------------------------
    val getCurrenciesUseCase = GetCurrenciesUseCase(currencyRepository)
    val saveUserUseCase = SaveUserUseCase(userRepository)
    val getUsersUseCase = GetUsersUseCase(userRepository)
    val getCurrentUserUseCase = GetCurrentUserUseCase(userRepository)
    val getSelectedCurrencySymbolUseCase = GetSelectedCurrencySymbolUseCase(userRepository)
    val getThemeModeUseCase = GetThemeModeUseCase(userRepository)
    val setThemeModeUseCase = SetThemeModeUseCase(userRepository)

    val getCategoriesUseCase = GetCategoriesUseCase(categoryRepository)
    val getActiveCategoriesUseCase = GetActiveCategoriesUseCase(categoryRepository)
    val saveCategoryUseCase = SaveCategoryUseCase(categoryRepository)
    val removeCategoryUseCase = RemoveCategoryUseCase(categoryRepository)
    val restoreCategoryUseCase = RestoreCategoryUseCase(categoryRepository)
    val countCategoryUsageUseCase = CountCategoryUsageUseCase(categoryRepository)

    val getAccountsUseCase = GetAccountsUseCase(accountRepository)
    val hasAccountUseCase = HasAccountUseCase(accountRepository)
    val createAccountUseCase = CreateAccountUseCase(accountRepository)
    val getAccountUseCase = GetAccountUseCase(accountRepository)
    val saveAccountUseCase = SaveAccountUseCase(accountRepository)
    val archiveAccountUseCase = ArchiveAccountUseCase(accountRepository)

    val getTransactionsInRangeUseCase = GetTransactionsInRangeUseCase(transactionRepository)
    val getRecentTransactionsUseCase = GetRecentTransactionsUseCase(transactionRepository)
    val getTransactionUseCase = GetTransactionUseCase(transactionRepository)
    val getTransactionDefaultsUseCase = GetTransactionDefaultsUseCase(transactionRepository)
    val saveTransactionUseCase = SaveTransactionUseCase(transactionRepository)
    val deleteTransactionUseCase = DeleteTransactionUseCase(transactionRepository)

    val getDebtsUseCase = GetDebtsUseCase(debtRepository)
    val getDebtUseCase = GetDebtUseCase(debtRepository)
    val getDebtPaymentsUseCase = GetDebtPaymentsUseCase(debtRepository)
    val saveDebtUseCase = SaveDebtUseCase(debtRepository)
    val deleteDebtUseCase = DeleteDebtUseCase(debtRepository)
    val recordDebtPaymentUseCase = RecordDebtPaymentUseCase(debtRepository)
    val deleteDebtPaymentUseCase = DeleteDebtPaymentUseCase(debtRepository)

    val getEmisUseCase = GetEmisUseCase(emiRepository)
    val getEmiUseCase = GetEmiUseCase(emiRepository)
    val getEmiPaymentsUseCase = GetEmiPaymentsUseCase(emiRepository)
    val saveEmiUseCase = SaveEmiUseCase(emiRepository)
    val deleteEmiUseCase = DeleteEmiUseCase(emiRepository)
    val markInstallmentPaidUseCase = MarkInstallmentPaidUseCase(emiRepository)
    val undoLastInstallmentUseCase = UndoLastInstallmentUseCase(emiRepository)

    val getLoanRecordsUseCase = GetLoanRecordsUseCase(borrowLendRepository)
    val getLoanRecordUseCase = GetLoanRecordUseCase(borrowLendRepository)
    val getLoanRepaymentsUseCase = GetLoanRepaymentsUseCase(borrowLendRepository)
    val saveLoanRecordUseCase = SaveLoanRecordUseCase(borrowLendRepository)
    val deleteLoanRecordUseCase = DeleteLoanRecordUseCase(borrowLendRepository)
    val recordLoanRepaymentUseCase = RecordLoanRepaymentUseCase(borrowLendRepository)
    val deleteLoanRepaymentUseCase = DeleteLoanRepaymentUseCase(borrowLendRepository)

    val getSpendingInsightsUseCase = GetSpendingInsightsUseCase(
        transactionRepository = transactionRepository,
        userRepository = userRepository,
        categoryRepository = categoryRepository
    )

    val getUpcomingPaymentsUseCase = GetUpcomingPaymentsUseCase(
        debtRepository = debtRepository,
        emiRepository = emiRepository,
        borrowLendRepository = borrowLendRepository,
        userRepository = userRepository
    )

    val backUpNowUseCase = BackUpNowUseCase(
        backupRepository = backupRepository,
        remoteBackupStore = remoteBackupStore
    )

    val restoreBackupUseCase = RestoreBackupUseCase(
        backupRepository = backupRepository,
        remoteBackupStore = remoteBackupStore
    )

    val observeBackupIdentityUseCase = ObserveBackupIdentityUseCase(remoteBackupStore)

    val exportTransactionsUseCase = ExportTransactionsUseCase(
        transactionRepository = transactionRepository,
        accountRepository = accountRepository,
        categoryRepository = categoryRepository
    )

    val getFinancialOverviewUseCase = GetFinancialOverviewUseCase(
        userRepository = userRepository,
        accountRepository = accountRepository,
        debtRepository = debtRepository,
        emiRepository = emiRepository,
        borrowLendRepository = borrowLendRepository
    )

    val getBudgetsUseCase = GetBudgetsUseCase(budgetRepository)
    val saveBudgetUseCase = SaveBudgetUseCase(budgetRepository, categoryRepository)
    val deleteBudgetUseCase = DeleteBudgetUseCase(budgetRepository)
    val getBudgetOverviewUseCase = GetBudgetOverviewUseCase(
        budgetRepository = budgetRepository,
        transactionRepository = transactionRepository,
        categoryRepository = categoryRepository,
        userRepository = userRepository
    )

    val getRecurringTransactionsUseCase = GetRecurringTransactionsUseCase(recurringTransactionRepository)
    val getRecurringTransactionUseCase = GetRecurringTransactionUseCase(recurringTransactionRepository)
    val saveRecurringTransactionUseCase = SaveRecurringTransactionUseCase(recurringTransactionRepository)
    val deleteRecurringTransactionUseCase = DeleteRecurringTransactionUseCase(recurringTransactionRepository)
    val setRecurringPausedUseCase = SetRecurringPausedUseCase(recurringTransactionRepository)
    val postDueRecurringUseCase = PostDueRecurringUseCase(recurringTransactionRepository)
    val postRecurringNowUseCase = PostRecurringNowUseCase(recurringTransactionRepository)
    val skipRecurringOccurrenceUseCase = SkipRecurringOccurrenceUseCase(recurringTransactionRepository)

    val prepareCsvImportUseCase = PrepareCsvImportUseCase(
        transactionRepository = transactionRepository,
        accountRepository = accountRepository,
        categoryRepository = categoryRepository
    )
    val commitCsvImportUseCase = CommitCsvImportUseCase(
        transactionRepository = transactionRepository,
        accountRepository = accountRepository,
        categoryRepository = categoryRepository
    )

    val getDashboardSnapshotUseCase = GetDashboardSnapshotUseCase(
        userRepository = userRepository,
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        transactionRepository = transactionRepository,
        debtRepository = debtRepository,
        emiRepository = emiRepository,
        borrowLendRepository = borrowLendRepository,
        budgetRepository = budgetRepository
    )
}
