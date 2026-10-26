package com.aurex.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.aurex.app.domain.model.LoanDirection
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.ui.screens.dashboard.DashboardScreen
import com.aurex.app.ui.screens.backup.BackupScreen
import com.aurex.app.ui.screens.budget.BudgetsScreen
import com.aurex.app.ui.screens.csvimport.ImportCsvScreen
import com.aurex.app.ui.screens.lock.AppLockSettingsScreen
import com.aurex.app.ui.screens.recurring.AddEditRecurringScreen
import com.aurex.app.ui.screens.recurring.RecurringScreen
import com.aurex.app.ui.screens.category.CategoriesScreen
import com.aurex.app.ui.screens.module.AccountsScreen

import com.aurex.app.ui.screens.borrowlend.AddEditBorrowLendScreen
import com.aurex.app.ui.screens.borrowlend.BorrowLendDetailScreen
import com.aurex.app.ui.screens.borrowlend.BorrowLendScreen
import com.aurex.app.ui.screens.debt.AddEditDebtScreen
import com.aurex.app.ui.screens.emi.AddEditEmiScreen
import com.aurex.app.ui.screens.emi.EmiDetailScreen
import com.aurex.app.ui.screens.emi.EmiScreen
import com.aurex.app.ui.screens.insights.InsightsScreen
import com.aurex.app.ui.screens.overview.FinancialOverviewScreen
import com.aurex.app.ui.screens.reminders.RemindersScreen
import com.aurex.app.ui.screens.settings.SettingsScreen
import com.aurex.app.ui.screens.debt.DebtDetailScreen
import com.aurex.app.ui.screens.debt.DebtScreen
import com.aurex.app.ui.screens.transaction.AddTransactionScreen
import com.aurex.app.ui.screens.transaction.TransactionsScreen

/**
 * The authenticated application shell.
 *
 * Owns the bottom navigation and a nested NavHost, so every module screen is
 * reachable in one tap and the setup flow above it stays chrome-free.
 */
@Composable
fun MainShell(
    modifier: Modifier = Modifier,
    /** A payment reminder was tapped: land on the screen it is about. */
    openReminders: Boolean = false,
    onRemindersOpened: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Consumed the moment it is acted on, so returning from the screen does not
    // bounce the user straight back into it.
    LaunchedEffect(openReminders) {
        if (openReminders) {
            navController.navigate(Reminders) { launchSingleTop = true }
            onRemindersOpened()
        }
    }

    val selectedTab = MainTab.entries.firstOrNull { tab ->
        currentDestination?.hierarchy()?.any { it.hasRouteOf(tab) } == true
    }

    // Hidden on the detail screens pushed above the tabs, where "add" is not the
    // primary action and the FAB would cover content.
    val showAddButton = selectedTab == MainTab.HOME || selectedTab == MainTab.TRANSACTIONS

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = selectedTab != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AurexNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab -> navController.switchTab(tab) }
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showAddButton,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(
                            AddTransaction(presetType = TransactionType.EXPENSE.name)
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add transaction")
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Home,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() }
        ) {
            composable<Home> {
                DashboardScreen(
                    onOpenAccounts = { navController.navigate(Accounts) },
                    onOpenOverview = { navController.navigate(FinancialPosition) },
                    onOpenInsights = { navController.navigate(Insights) },
                    onOpenReminders = { navController.navigate(Reminders) },
                    onOpenProfile = { navController.navigate(Profile) },
                    onOpenBudgets = { navController.navigate(Budgets) },
                    onOpenTransactions = { navController.switchTab(MainTab.TRANSACTIONS) },
                    onOpenDebts = { navController.switchTab(MainTab.DEBT) },
                    onOpenBorrowLend = { navController.switchTab(MainTab.BORROW) },
                    onAddTransaction = { type ->
                        navController.navigate(AddTransaction(presetType = type.name))
                    }
                )
            }
            composable<Transactions> {
                TransactionsScreen(
                    onAddTransaction = {
                        navController.navigate(
                            AddTransaction(presetType = TransactionType.EXPENSE.name)
                        )
                    },
                    onEditTransaction = { id ->
                        navController.navigate(AddTransaction(transactionId = id))
                    }
                )
            }
            composable<Debts> {
                DebtScreen(
                    onAddDebt = { navController.navigate(AddEditDebt()) },
                    onOpenDebt = { id -> navController.navigate(DebtDetail(id)) }
                )
            }
            composable<DebtDetail> { entry ->
                DebtDetailScreen(
                    debtId = entry.toRoute<DebtDetail>().debtId,
                    onBack = { navController.popBackStack() },
                    onEditDebt = { id -> navController.navigate(AddEditDebt(debtId = id)) }
                )
            }
            composable<AddEditDebt> { entry ->
                AddEditDebtScreen(
                    onBack = { navController.popBackStack() },
                    debtId = entry.toRoute<AddEditDebt>().debtId
                        .takeIf { it != AddEditDebt.NO_DEBT_ID }
                )
            }
            composable<Emis> {
                EmiScreen(
                    onAddEmi = { navController.navigate(AddEditEmi()) },
                    onOpenEmi = { id -> navController.navigate(EmiDetail(id)) }
                )
            }
            composable<EmiDetail> { entry ->
                EmiDetailScreen(
                    emiId = entry.toRoute<EmiDetail>().emiId,
                    onBack = { navController.popBackStack() },
                    onEditEmi = { id -> navController.navigate(AddEditEmi(emiId = id)) }
                )
            }
            composable<AddEditEmi> { entry ->
                AddEditEmiScreen(
                    onBack = { navController.popBackStack() },
                    emiId = entry.toRoute<AddEditEmi>().emiId.takeIf { it != AddEditEmi.NO_EMI_ID }
                )
            }
            composable<BorrowLend> {
                BorrowLendScreen(
                    onAddRecord = { direction ->
                        navController.navigate(AddEditLoan(direction = direction.name))
                    },
                    onOpenRecord = { id -> navController.navigate(LoanDetail(id)) }
                )
            }
            composable<LoanDetail> { entry ->
                BorrowLendDetailScreen(
                    recordId = entry.toRoute<LoanDetail>().recordId,
                    onBack = { navController.popBackStack() },
                    onEditRecord = { id -> navController.navigate(AddEditLoan(recordId = id)) }
                )
            }
            composable<AddEditLoan> { entry ->
                val route = entry.toRoute<AddEditLoan>()
                AddEditBorrowLendScreen(
                    onBack = { navController.popBackStack() },
                    recordId = route.recordId.takeIf { it != AddEditLoan.NO_RECORD_ID },
                    initialDirection = runCatching { LoanDirection.valueOf(route.direction) }
                        .getOrDefault(LoanDirection.LENT)
                )
            }
            composable<FinancialPosition> {
                FinancialOverviewScreen(onBack = { navController.popBackStack() })
            }
            composable<Accounts> { AccountsScreen(onBack = { navController.popBackStack() }) }
            composable<Categories> {
                CategoriesScreen(onBack = { navController.popBackStack() })
            }
            composable<Profile> {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenReminders = { navController.navigate(Reminders) },
                    onOpenCategories = { navController.navigate(Categories) },
                    onOpenBackup = { navController.navigate(Backup) },
                    onOpenBudgets = { navController.navigate(Budgets) },
                    onOpenRecurring = { navController.navigate(Recurring) },
                    onOpenImport = { navController.navigate(ImportCsv) },
                    onOpenAppLock = { navController.navigate(AppLockSettingsRoute) }
                )
            }
            composable<Budgets> {
                BudgetsScreen(onBack = { navController.popBackStack() })
            }
            composable<Recurring> {
                RecurringScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { type -> navController.navigate(AddEditRecurring(presetType = type.name)) },
                    onEdit = { id -> navController.navigate(AddEditRecurring(recurringId = id)) }
                )
            }
            composable<AddEditRecurring> { entry ->
                val route = entry.toRoute<AddEditRecurring>()
                AddEditRecurringScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAccounts = { navController.navigate(Accounts) },
                    recurringId = route.recurringId.takeIf { it != AddEditRecurring.NO_RECURRING_ID },
                    presetType = runCatching { TransactionType.valueOf(route.presetType) }
                        .getOrDefault(TransactionType.EXPENSE)
                )
            }
            composable<ImportCsv> {
                ImportCsvScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAccounts = { navController.navigate(Accounts) }
                )
            }
            composable<AppLockSettingsRoute> {
                AppLockSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<Backup> {
                BackupScreen(onBack = { navController.popBackStack() })
            }
            composable<Insights> {
                InsightsScreen(onBack = { navController.popBackStack() })
            }
            composable<Reminders> {
                RemindersScreen(onBack = { navController.popBackStack() })
            }
            composable<AddTransaction> { entry ->
                val route = entry.toRoute<AddTransaction>()
                AddTransactionScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAccounts = { navController.navigate(Accounts) },
                    transactionId = route.transactionId.takeIf { it != AddTransaction.NO_TRANSACTION_ID },
                    presetType = runCatching { TransactionType.valueOf(route.presetType) }
                        .getOrDefault(TransactionType.EXPENSE)
                )
            }
        }
    }
}

@Composable
private fun AurexNavigationBar(
    selectedTab: MainTab?,
    onTabSelected: (MainTab) -> Unit
) {
    androidx.compose.foundation.layout.Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            MainTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = {
                        Text(
                            text = tab.label,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

/**
 * Standard bottom-bar switch: single top-level entry per tab, state preserved,
 * and no growing back stack from tab hopping.
 */
private fun androidx.navigation.NavHostController.switchTab(tab: MainTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun androidx.navigation.NavDestination.hierarchy(): Sequence<androidx.navigation.NavDestination> =
    generateSequence(this) { it.parent }

private fun androidx.navigation.NavDestination.hasRouteOf(tab: MainTab): Boolean = when (tab) {
    MainTab.HOME -> hasRoute(Home::class)
    MainTab.TRANSACTIONS -> hasRoute(Transactions::class)
    MainTab.DEBT -> hasRoute(Debts::class)
    MainTab.EMI -> hasRoute(Emis::class)
    MainTab.BORROW -> hasRoute(BorrowLend::class)
}
