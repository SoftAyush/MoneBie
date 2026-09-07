package com.aurex.app.core

import app.cash.sqldelight.db.SqlDriver
import com.aurex.app.data.database.DatabaseHelper
import com.aurex.app.data.repository.AccountRepositoryImpl
import com.aurex.app.data.repository.BackupRepositoryImpl
import com.aurex.app.data.repository.UnavailableRemoteBackupStore
import com.aurex.app.data.repository.CategoryRepositoryImpl
import com.aurex.app.data.repository.CurrencyRepositoryImpl
import com.aurex.app.data.repository.BorrowLendRepositoryImpl
import com.aurex.app.data.repository.DebtRepositoryImpl
import com.aurex.app.data.repository.EmiRepositoryImpl
import com.aurex.app.data.repository.TransactionRepositoryImpl
import com.aurex.app.data.repository.UserRepositoryImpl
import com.aurex.app.domain.repository.AccountRepository
import com.aurex.app.domain.repository.BackupRepository
import com.aurex.app.domain.repository.RemoteBackupStore
import com.aurex.app.domain.repository.BorrowLendRepository
import com.aurex.app.domain.repository.CategoryRepository
import com.aurex.app.domain.repository.CurrencyRepository
import com.aurex.app.domain.repository.DebtRepository
import com.aurex.app.domain.repository.EmiRepository
import com.aurex.app.domain.repository.TransactionRepository
import com.aurex.app.domain.repository.UserRepository
import com.aurex.app.domain.usecase.account.ArchiveAccountUseCase
import com.aurex.app.domain.usecase.backup.BackUpNowUseCase
import com.aurex.app.domain.usecase.backup.ObserveBackupIdentityUseCase
import com.aurex.app.domain.usecase.backup.RestoreBackupUseCase
import com.aurex.app.domain.usecase.account.CreateAccountUseCase
import com.aurex.app.domain.usecase.account.GetAccountUseCase
import com.aurex.app.domain.usecase.account.SaveAccountUseCase
import com.aurex.app.domain.usecase.account.GetAccountsUseCase
import com.aurex.app.domain.usecase.account.HasAccountUseCase
import com.aurex.app.domain.usecase.borrowlend.DeleteLoanRecordUseCase
import com.aurex.app.domain.usecase.borrowlend.DeleteLoanRepaymentUseCase
import com.aurex.app.domain.usecase.borrowlend.GetLoanRecordUseCase
import com.aurex.app.domain.usecase.borrowlend.GetLoanRecordsUseCase
import com.aurex.app.domain.usecase.borrowlend.GetLoanRepaymentsUseCase
import com.aurex.app.domain.usecase.borrowlend.RecordLoanRepaymentUseCase
import com.aurex.app.domain.usecase.borrowlend.SaveLoanRecordUseCase
import com.aurex.app.domain.usecase.category.CountCategoryUsageUseCase
import com.aurex.app.domain.usecase.category.GetActiveCategoriesUseCase
import com.aurex.app.domain.usecase.category.GetCategoriesUseCase
import com.aurex.app.domain.usecase.category.RemoveCategoryUseCase
import com.aurex.app.domain.usecase.category.RestoreCategoryUseCase
import com.aurex.app.domain.usecase.category.SaveCategoryUseCase
import com.aurex.app.domain.usecase.currency.GetCurrenciesUseCase
import com.aurex.app.domain.usecase.dashboard.GetDashboardSnapshotUseCase
import com.aurex.app.domain.usecase.debt.DeleteDebtPaymentUseCase
import com.aurex.app.domain.usecase.debt.DeleteDebtUseCase
import com.aurex.app.domain.usecase.debt.GetDebtPaymentsUseCase
import com.aurex.app.domain.usecase.debt.GetDebtUseCase
import com.aurex.app.domain.usecase.debt.GetDebtsUseCase
import com.aurex.app.domain.usecase.debt.RecordDebtPaymentUseCase
import com.aurex.app.domain.usecase.debt.SaveDebtUseCase
import com.aurex.app.domain.usecase.emi.DeleteEmiUseCase
import com.aurex.app.domain.usecase.export.ExportTransactionsUseCase
import com.aurex.app.domain.usecase.insights.GetSpendingInsightsUseCase
import com.aurex.app.domain.usecase.reminders.GetUpcomingPaymentsUseCase
import com.aurex.app.domain.usecase.emi.GetEmiPaymentsUseCase
import com.aurex.app.domain.usecase.emi.GetEmiUseCase
import com.aurex.app.domain.usecase.emi.GetEmisUseCase
import com.aurex.app.domain.usecase.emi.MarkInstallmentPaidUseCase
import com.aurex.app.domain.usecase.emi.SaveEmiUseCase
import com.aurex.app.domain.usecase.emi.UndoLastInstallmentUseCase
import com.aurex.app.domain.usecase.overview.GetFinancialOverviewUseCase
import com.aurex.app.domain.usecase.transaction.DeleteTransactionUseCase
import com.aurex.app.domain.usecase.transaction.GetRecentTransactionsUseCase
import com.aurex.app.domain.usecase.transaction.GetTransactionDefaultsUseCase
import com.aurex.app.domain.usecase.transaction.GetTransactionUseCase
import com.aurex.app.domain.usecase.transaction.GetTransactionsInRangeUseCase
import com.aurex.app.domain.usecase.transaction.SaveTransactionUseCase
import com.aurex.app.domain.usecase.user.GetCurrentUserUseCase
import com.aurex.app.domain.usecase.user.GetSelectedCurrencySymbolUseCase
import com.aurex.app.domain.usecase.user.GetThemeModeUseCase
import com.aurex.app.domain.usecase.user.SetThemeModeUseCase
import com.aurex.app.domain.usecase.user.GetUsersUseCase
import com.aurex.app.domain.usecase.user.SaveUserUseCase

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

    val getDashboardSnapshotUseCase = GetDashboardSnapshotUseCase(
        userRepository = userRepository,
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        transactionRepository = transactionRepository,
        debtRepository = debtRepository,
        emiRepository = emiRepository,
        borrowLendRepository = borrowLendRepository
    )
}
