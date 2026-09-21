package com.aurex.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Home
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/*
 * Type-safe routes, matching the convention already used for the setup flow.
 *
 * Two graphs: setup destinations live in the root NavHost, and the tabs below
 * live in a nested NavHost inside MainShell. Nesting keeps the bottom bar out of
 * the onboarding screens without a per-screen visibility flag.
 */

/** Root route hosting the authenticated shell. */
@Serializable
object Main

@Serializable
object Home

@Serializable
object Transactions

@Serializable
object Debts

@Serializable
object Emis

@Serializable
object BorrowLend

@Serializable
object Accounts

@Serializable
object Categories

@Serializable
object Profile

/**
 * Add or edit a transaction.
 *
 * Both arguments are non-null with defaults because typed routes serialise
 * primitives directly: [transactionId] of [NO_TRANSACTION_ID] means "new
 * entry", and [presetType] lets the dashboard's quick-add buttons open the form
 * already on the right side.
 */
@Serializable
data class AddTransaction(
    val transactionId: Long = NO_TRANSACTION_ID,
    val presetType: String = "EXPENSE"
) {
    companion object {
        const val NO_TRANSACTION_ID = -1L
    }
}

/**
 * Primary navigation.
 *
 * Five tabs, matching the modules the user works in daily. Accounts and Profile
 * are reached from the dashboard header instead: they are visited occasionally,
 * and a seven-item bar would shrink every label past readability.
 */
enum class MainTab(
    val route: Any,
    val label: String,
    val icon: ImageVector
) {
    HOME(Home, "Home", Icons.Rounded.Home),
    TRANSACTIONS(Transactions, "Activity", Icons.AutoMirrored.Rounded.ReceiptLong),
    DEBT(Debts, "Debt", Icons.Rounded.AccountBalanceWallet),
    EMI(Emis, "EMI", Icons.Rounded.EventRepeat),
    BORROW(BorrowLend, "Lend", Icons.Rounded.Handshake)
}

/** A single debt with its payment history. */
@Serializable
data class DebtDetail(val debtId: Long)

/** Create a debt, or edit an existing one when [debtId] is set. */
@Serializable
data class AddEditDebt(val debtId: Long = NO_DEBT_ID) {
    companion object {
        const val NO_DEBT_ID = -1L
    }
}

/** A single EMI with its installment history. */
@Serializable
data class EmiDetail(val emiId: Long)

/** Create an EMI, or edit an existing one when [emiId] is set. */
@Serializable
data class AddEditEmi(val emiId: Long = NO_EMI_ID) {
    companion object {
        const val NO_EMI_ID = -1L
    }
}

/** A single borrowing or lending with its repayment history. */
@Serializable
data class LoanDetail(val recordId: Long)

/**
 * Create a borrow/lend record, or edit an existing one when [recordId] is set.
 *
 * [direction] seeds a new record from whichever side the list was showing.
 */
@Serializable
data class AddEditLoan(
    val recordId: Long = NO_RECORD_ID,
    val direction: String = "LENT"
) {
    companion object {
        const val NO_RECORD_ID = -1L
    }
}

/** The full balance sheet: assets, liabilities and the net position. */
@Serializable
object FinancialPosition

/** Spending analysis for one month at a time. */
@Serializable
object Insights

/** Everything with a due date, grouped by urgency. */
@Serializable
object Reminders

/** Cloud backup: sign in, upload the ledger, restore it on another phone. */
@Serializable
object Backup

/** Monthly spending limits by category, one month at a time. */
@Serializable
object Budgets

/** Scheduled transactions: rent, salary, subscriptions. */
@Serializable
object Recurring

/**
 * Create a recurring transaction, or edit an existing one when [recurringId]
 * is set. [presetType] opens the form on the right side of the ledger.
 */
@Serializable
data class AddEditRecurring(
    val recurringId: Long = NO_RECURRING_ID,
    val presetType: String = "EXPENSE"
) {
    companion object {
        const val NO_RECURRING_ID = -1L
    }
}

/** Bring transactions in from a CSV file. */
@Serializable
object ImportCsv

/** The PIN and biometric lock. */
@Serializable
object AppLockSettingsRoute
