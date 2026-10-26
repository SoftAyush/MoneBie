package com.monebie.app.domain.calculation

import com.monebie.app.core.Money
import com.monebie.app.core.getCurrentDay
import com.monebie.app.core.monthOf
import com.monebie.app.core.totalOf
import com.monebie.app.domain.model.Account
import com.monebie.app.domain.model.AccountType
import com.monebie.app.domain.model.BorrowLendRecord
import com.monebie.app.domain.model.Debt
import com.monebie.app.domain.model.Emi
import com.monebie.app.domain.model.FinancialOverview
import com.monebie.app.domain.model.LoanDirection
import com.monebie.app.domain.model.PositionGroup
import com.monebie.app.domain.model.PositionGroupKey
import com.monebie.app.domain.model.PositionLine
import com.monebie.app.domain.model.isAsset

/**
 * Turns raw records into the itemised financial position.
 *
 * Separate from [FinancialCalculator] because this is composition rather than
 * arithmetic: every total here comes from the calculator, and this file only
 * decides how those totals are grouped and which records to show underneath
 * them. Keeping the split means a labelling change can never alter a number.
 *
 * Groups with nothing in them are dropped, so the overview shows what the user
 * actually has rather than a list of zeroes for products they do not use.
 */
object PositionComposer {

    fun compose(
        accounts: List<Account>,
        debts: List<Debt>,
        emis: List<Emi>,
        records: List<BorrowLendRecord>,
        currencySymbol: String,
        today: String = getCurrentDay()
    ): FinancialOverview {
        val active = accounts.filter { !it.isArchived }
        val netPosition = FinancialCalculator.netPosition(accounts, debts, emis, records)

        return FinancialOverview(
            currencySymbol = currencySymbol,
            today = today,
            netPosition = netPosition,
            assetGroups = assetGroups(active, records),
            liabilityGroups = liabilityGroups(active, debts, emis, records, today),
            obligationsThisMonth = FinancialCalculator.obligationsInMonth(
                debts = debts,
                emis = emis,
                month = monthOf(today),
                today = today
            )
        )
    }

    // -------------------------------------------------------------------------
    // Assets
    // -------------------------------------------------------------------------

    private fun assetGroups(
        accounts: List<Account>,
        records: List<BorrowLendRecord>
    ): List<PositionGroup> = buildList {
        add(accountGroup(accounts, AccountType.CASH, PositionGroupKey.CASH, "Cash"))
        add(accountGroup(accounts, AccountType.BANK, PositionGroupKey.BANK, "Bank accounts"))
        add(
            accountGroup(
                accounts,
                AccountType.DIGITAL_WALLET,
                PositionGroupKey.WALLET,
                "Digital wallets"
            )
        )

        // Credit balances on cards and loan accounts - money the user can draw
        // on that belongs to neither of the buckets above.
        val inCredit = accounts.filter { !it.type.isAsset && it.balance.isPositive }
        add(
            PositionGroup(
                key = PositionGroupKey.OTHER_ASSETS,
                label = "Credit balances",
                total = inCredit.totalOf { it.balance },
                lines = inCredit.map { account ->
                    PositionLine(
                        id = "account-" + account.id,
                        label = account.name,
                        sublabel = "In credit",
                        amount = account.balance
                    )
                }
            )
        )

        val lent = records.filter { it.direction == LoanDirection.LENT && !it.isSettled }
        add(
            PositionGroup(
                key = PositionGroupKey.MONEY_LENT,
                label = "Money lent out",
                total = lent.totalOf { it.remainingAmount },
                lines = lent
                    .sortedByDescending { it.remainingAmount.minorUnits }
                    .map { record ->
                        PositionLine(
                            id = "lent-" + record.id,
                            label = record.personName,
                            sublabel = record.dueDate?.let { "Expected back" } ?: "No date agreed",
                            amount = record.remainingAmount
                        )
                    }
            )
        )
    }.filterNot { it.total.isZero }

    private fun accountGroup(
        accounts: List<Account>,
        type: AccountType,
        key: PositionGroupKey,
        label: String
    ): PositionGroup {
        val matching = accounts.filter { it.type == type }
        return PositionGroup(
            key = key,
            label = label,
            total = matching.totalOf { it.balance },
            lines = matching.map { account ->
                PositionLine(
                    id = "account-" + account.id,
                    label = account.name,
                    amount = account.balance
                )
            }
        )
    }

    // -------------------------------------------------------------------------
    // Liabilities
    // -------------------------------------------------------------------------

    private fun liabilityGroups(
        accounts: List<Account>,
        debts: List<Debt>,
        emis: List<Emi>,
        records: List<BorrowLendRecord>,
        today: String
    ): List<PositionGroup> = buildList {
        val openDebts = debts.filter { !it.isSettled }
        add(
            PositionGroup(
                key = PositionGroupKey.DEBTS,
                label = "Debts",
                total = openDebts.totalOf { it.remainingAmount },
                lines = openDebts
                    .sortedByDescending { it.remainingAmount.minorUnits }
                    .map { debt ->
                        PositionLine(
                            id = "debt-" + debt.id,
                            label = debt.name,
                            sublabel = debt.lender,
                            amount = debt.remainingAmount
                        )
                    }
            )
        )

        val openEmis = emis.filter { !it.isCompleted }
        add(
            PositionGroup(
                key = PositionGroupKey.EMI,
                label = "EMIs",
                total = openEmis.totalOf { it.remainingAmount },
                lines = openEmis
                    .sortedByDescending { it.remainingAmount.minorUnits }
                    .map { emi ->
                        PositionLine(
                            id = "emi-" + emi.id,
                            label = emi.name,
                            sublabel = emi.remainingInstallments.toString() + " left",
                            amount = emi.remainingAmount
                        )
                    }
            )
        )

        val borrowed = records.filter { it.direction == LoanDirection.BORROWED && !it.isSettled }
        add(
            PositionGroup(
                key = PositionGroupKey.MONEY_BORROWED,
                label = "Money borrowed",
                total = borrowed.totalOf { it.remainingAmount },
                lines = borrowed
                    .sortedByDescending { it.remainingAmount.minorUnits }
                    .map { record ->
                        PositionLine(
                            id = "borrowed-" + record.id,
                            label = record.personName,
                            sublabel = record.dueDate?.let { "Due" } ?: "No date agreed",
                            amount = record.remainingAmount
                        )
                    }
            )
        )

        val cards = accounts.filter { it.type == AccountType.CREDIT_CARD && !it.outstanding.isZero }
        add(
            PositionGroup(
                key = PositionGroupKey.CREDIT_CARDS,
                label = "Credit cards",
                total = cards.totalOf { it.outstanding },
                lines = cards.map { account ->
                    PositionLine(
                        id = "card-" + account.id,
                        label = account.name,
                        sublabel = "Outstanding",
                        amount = account.outstanding
                    )
                }
            )
        )
    }.filterNot { it.total.isZero }

    /** Sum of a set of groups, used to assert the parts add up to the whole. */
    fun sumOf(groups: List<PositionGroup>): Money = groups.totalOf { it.total }
}
