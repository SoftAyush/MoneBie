package com.aurex.app.domain.calculation

import com.aurex.app.core.Money
import com.aurex.app.core.addDays
import com.aurex.app.core.addMonths
import com.aurex.app.core.dayOf
import com.aurex.app.core.firstDayOfMonth
import com.aurex.app.core.daysBetween
import com.aurex.app.core.getCurrentDay
import com.aurex.app.core.isInMonth
import com.aurex.app.core.isOnDay
import com.aurex.app.core.monthOf
import com.aurex.app.core.totalOf
import com.aurex.app.domain.model.Account
import com.aurex.app.domain.model.AccountDelta
import com.aurex.app.domain.model.AccountType
import com.aurex.app.domain.model.AssetBreakdown
import com.aurex.app.domain.model.BorrowLendRecord
import com.aurex.app.domain.model.CategorySpend
import com.aurex.app.domain.model.DailyTotal
import com.aurex.app.domain.model.DayGroup
import com.aurex.app.domain.model.Debt
import com.aurex.app.domain.model.DueBucket
import com.aurex.app.domain.model.DebtStatus
import com.aurex.app.domain.model.Emi
import com.aurex.app.domain.model.EmiStatus
import com.aurex.app.domain.model.LiabilityBreakdown
import com.aurex.app.domain.model.LoanDirection
import com.aurex.app.domain.model.LoanStatus
import com.aurex.app.domain.model.NetPosition
import com.aurex.app.domain.model.PaymentFrequency
import com.aurex.app.domain.model.PeriodSummary
import com.aurex.app.domain.model.Transaction
import com.aurex.app.domain.model.CategoryLookup
import com.aurex.app.domain.model.TransactionDraft
import com.aurex.app.domain.model.TransactionType
import com.aurex.app.domain.model.UpcomingPayment
import com.aurex.app.domain.model.UpcomingPaymentKind
import com.aurex.app.domain.model.isAsset
import com.aurex.app.domain.model.periodDays
import com.aurex.app.domain.model.periodMonths

/**
 * The single source of truth for every derived financial figure in the app.
 *
 * Rules this file exists to enforce:
 *  - a total is computed in exactly one place, so two screens can never disagree;
 *  - every function is pure and takes today as a parameter, so results are
 *    deterministic and unit-testable with no clock and no database;
 *  - money stays in minor units end to end, with no Double arithmetic anywhere.
 *
 * Later phases add screens, not new maths: a debt detail screen calls
 * [debtStatus] rather than re-deriving status from a remaining amount.
 */
object FinancialCalculator {

    // ---------------------------------------------------------------------
    // Accounts
    // ---------------------------------------------------------------------

    /**
     * Liquid money the user actually holds.
     *
     * Credit-card, loan and EMI accounts are excluded: their balance is an
     * amount owed, and adding it here would inflate the headline balance while
     * the same money is also counted as a liability.
     */
    fun totalBalance(accounts: List<Account>): Money =
        accounts.filter { !it.isArchived && it.isAssetAccount }.totalOf { it.balance }

    fun creditCardOutstanding(accounts: List<Account>): Money =
        accounts.filter { !it.isArchived && it.type == AccountType.CREDIT_CARD }
            .totalOf { it.outstanding }

    // ---------------------------------------------------------------------
    // Period summaries
    // ---------------------------------------------------------------------

    /**
     * Income and expense totals over an arbitrary subset.
     *
     * Two kinds of movement are deliberately excluded, because neither is
     * earning or spending:
     *
     *  - transfers between the user's own accounts, which are net-neutral;
     *  - person-to-person loan principal, in either direction. Lending cash
     *    turns an asset into a receivable and being repaid turns it back;
     *    borrowing raises cash and a liability together and repaying lowers
     *    both. Counting them would inflate income by money that must be given
     *    back, and a complete lend-then-repay round trip would show a profit
     *    that never existed.
     *
     * Debt and EMI payments are *not* excluded: servicing a loan is real
     * spending that people budget for, and the interest is a genuine cost.
     *
     * The excluded transactions still move account balances and still appear in
     * the transaction list - only these totals leave them out.
     */
    fun summarise(transactions: List<Transaction>): PeriodSummary {
        val cashFlow = transactions.filterNot { it.source.isLoanPrincipalMovement }
        return PeriodSummary(
            income = cashFlow.filter { it.type == TransactionType.INCOME }.totalOf { it.amount },
            expense = cashFlow.filter { it.type == TransactionType.EXPENSE }.totalOf { it.amount }
        )
    }

    fun summaryForDay(transactions: List<Transaction>, day: String): PeriodSummary =
        summarise(transactions.filter { isOnDay(it.date, day) })

    /** [month] is yyyy-MM. */
    fun summaryForMonth(transactions: List<Transaction>, month: String): PeriodSummary =
        summarise(transactions.filter { isInMonth(it.date, month) })

    /**
     * The last [days] calendar days ending on [today], oldest first.
     *
     * Days with no activity are emitted as zeroes so the chart keeps a fixed
     * number of bars and does not silently compress gaps.
     */
    fun dailyTrend(transactions: List<Transaction>, today: String, days: Int = 7): List<DailyTotal> {
        val byDay = transactions.groupBy { dayOf(it.date) }
        return (days - 1 downTo 0).map { offset ->
            val day = addDays(today, -offset)
            val summary = summarise(byDay[day].orEmpty())
            DailyTotal(day = day, income = summary.income, expense = summary.expense)
        }
    }

    /**
     * Groups transactions into calendar days, newest day first, each with its
     * own income/expense totals.
     *
     * Day subtotals are money maths, so they are produced here rather than in
     * the transaction list screen.
     */
    fun dayGroups(transactions: List<Transaction>): List<DayGroup> =
        transactions
            .groupBy { dayOf(it.date) }
            .entries
            .sortedByDescending { it.key }
            .map { (day, entries) ->
                val ordered = entries.sortedByDescending { it.date }
                DayGroup(day = day, transactions = ordered, summary = summarise(ordered))
            }

    // ---------------------------------------------------------------------
    // Category analysis
    // ---------------------------------------------------------------------

    /**
     * Spending or earning split by category, largest first.
     *
     * Runs over the same cash-flow subset as [summarise], so the shares always
     * add to 100% of the figure shown above them - moving money to a friend
     * would otherwise appear as a spending category.
     */
    fun categoryBreakdown(
        transactions: List<Transaction>,
        type: TransactionType,
        // The user may have renamed or hidden any of these, so the labels come
        // from whatever the caller loaded rather than from the shipped list.
        categories: CategoryLookup = CategoryLookup.Seeded
    ): List<CategorySpend> {
        val relevant = transactions
            .filterNot { it.source.isLoanPrincipalMovement }
            .filter { it.type == type }

        val total = relevant.totalOf { it.amount }
        if (total.isZero) return emptyList()

        return relevant
            .groupBy { it.categoryId }
            .map { (categoryId, entries) ->
                val category = categories[categoryId]
                val amount = entries.totalOf { it.amount }
                CategorySpend(
                    categoryId = categoryId,
                    label = category.name,
                    iconKey = category.iconKey,
                    amount = amount,
                    share = amount.ratioOf(total),
                    transactionCount = entries.size
                )
            }
            .sortedByDescending { it.amount.minorUnits }
    }

    /**
     * Average spend per day across a period.
     *
     * [days] is the number of days actually elapsed, not the length of the
     * month: dividing a half-finished month by 31 would understate the rate
     * and make overspending look under control.
     */
    fun dailyAverageExpense(expense: Money, days: Int): Money =
        if (days <= 0) Money.ZERO else Money(expense.minorUnits / days)

    /**
     * Days elapsed in [month] as of [today], capped at the month's length.
     *
     * A past month counts in full; the current one counts only so far.
     */
    fun elapsedDaysInMonth(month: String, today: String): Int {
        val firstDay = firstDayOfMonth(month)
        val dayAfterMonth = addMonths(firstDay, 1)
        val monthLength = daysBetween(firstDay, dayAfterMonth).toInt()

        if (monthOf(today) != month) {
            // Any month other than the current one is either fully elapsed or
            // has not started; both are answered by comparing to its first day.
            return if (today >= dayAfterMonth) monthLength else 0
        }
        return (daysBetween(firstDay, today).toInt() + 1).coerceIn(1, monthLength)
    }

    /** The day in [transactions] with the highest expense total, or null if nothing was spent. */
    fun heaviestSpendingDay(transactions: List<Transaction>): DailyTotal? =
        transactions
            .filterNot { it.source.isLoanPrincipalMovement }
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { dayOf(it.date) }
            .map { (day, entries) ->
                val summary = summarise(entries)
                DailyTotal(day = day, income = summary.income, expense = summary.expense)
            }
            .maxByOrNull { it.expense.minorUnits }

    // ---------------------------------------------------------------------
    // Due-date bucketing
    // ---------------------------------------------------------------------

    /**
     * Groups obligations by how soon they need attention.
     *
     * Buckets rather than a flat list because "overdue" and "due in three
     * weeks" call for completely different reactions, and a single sorted list
     * makes the reader work that out for themselves.
     */
    fun bucketByUrgency(payments: List<UpcomingPayment>): Map<DueBucket, List<UpcomingPayment>> =
        payments
            .groupBy { DueBucket.forDaysUntilDue(it.daysUntilDue) }
            .toList()
            .sortedBy { (bucket, _) -> bucket.ordinal }
            .toMap()

    // ---------------------------------------------------------------------
    // Balance posting
    // ---------------------------------------------------------------------

    /**
     * How a transaction moves account balances. The single definition of that
     * rule, used when posting, editing and deleting.
     *
     * Income credits its account, an expense debits it, and a transfer does
     * both - which is what keeps moving cash into the bank from looking like
     * income and an expense. A credit-card expense debits the card into
     * negative territory, and [Account.outstanding] reads that back as the
     * amount owed.
     */
    fun balanceDeltas(
        type: TransactionType,
        amount: Money,
        accountId: Long,
        transferAccountId: Long?
    ): List<AccountDelta> = when (type) {
        TransactionType.INCOME -> listOf(AccountDelta(accountId, amount))
        TransactionType.EXPENSE -> listOf(AccountDelta(accountId, -amount))
        TransactionType.TRANSFER -> buildList {
            add(AccountDelta(accountId, -amount))
            transferAccountId?.let { add(AccountDelta(it, amount)) }
        }
    }

    fun balanceDeltas(draft: TransactionDraft): List<AccountDelta> =
        balanceDeltas(draft.type, draft.amount, draft.accountId, draft.transferAccountId)

    fun balanceDeltas(transaction: Transaction): List<AccountDelta> =
        balanceDeltas(
            transaction.type,
            transaction.amount,
            transaction.accountId,
            transaction.transferAccountId
        )

    /** The deltas that undo [transaction], used when it is edited or deleted. */
    fun reversingDeltas(transaction: Transaction): List<AccountDelta> =
        balanceDeltas(transaction).map { AccountDelta(it.accountId, -it.delta) }

    // ---------------------------------------------------------------------
    // Module totals
    // ---------------------------------------------------------------------

    fun totalDebtOutstanding(debts: List<Debt>): Money =
        debts.filter { !it.isSettled }.totalOf { it.remainingAmount }

    fun totalEmiOutstanding(emis: List<Emi>): Money =
        emis.filter { !it.isCompleted }.totalOf { it.remainingAmount }

    fun totalBorrowed(records: List<BorrowLendRecord>): Money =
        records.filter { it.direction == LoanDirection.BORROWED && !it.isSettled }
            .totalOf { it.remainingAmount }

    fun totalLent(records: List<BorrowLendRecord>): Money =
        records.filter { it.direction == LoanDirection.LENT && !it.isSettled }
            .totalOf { it.remainingAmount }

    /** Installments falling due inside [month] (yyyy-MM) across all active EMIs. */
    fun emiDueInMonth(emis: List<Emi>, month: String): Money =
        emis.filter { emi -> emi.nextDueDate?.let { monthOf(it) == month } == true }
            .totalOf { it.monthlyAmount }

    /** Debt payments falling due inside [month] (yyyy-MM). */
    fun debtDueInMonth(debts: List<Debt>, month: String, today: String = getCurrentDay()): Money =
        debts.filter { !it.isSettled }
            .filter { debt -> nextDebtDueDate(debt, today)?.let { monthOf(it) == month } == true }
            .totalOf { it.nextPaymentAmount }

    /**
     * Everything scheduled to go out this month across debts and EMIs.
     *
     * This is what the user has to find money for now, which is a different
     * question from what they owe in total - and the more useful one when
     * looking at whether a month works.
     */
    fun obligationsInMonth(
        debts: List<Debt>,
        emis: List<Emi>,
        month: String,
        today: String = getCurrentDay()
    ): Money = debtDueInMonth(debts, month, today) + emiDueInMonth(emis, month)

    // ---------------------------------------------------------------------
    // Net position
    // ---------------------------------------------------------------------

    fun assetBreakdown(accounts: List<Account>, records: List<BorrowLendRecord>): AssetBreakdown {
        val active = accounts.filter { !it.isArchived }
        return AssetBreakdown(
            cash = active.filter { it.type == AccountType.CASH }.totalOf { it.balance },
            bank = active.filter { it.type == AccountType.BANK }.totalOf { it.balance },
            wallet = active.filter { it.type == AccountType.DIGITAL_WALLET }.totalOf { it.balance },
            // A liability-type account in credit - an overpaid card - holds real
            // money. Counted here so it stops falling through the gap between
            // "not an asset account" and "no outstanding balance".
            otherAssets = active.filter { !it.type.isAsset }
                .totalOf { it.balance.coerceAtLeastZero() },
            moneyLentOut = totalLent(records)
        )
    }

    fun liabilityBreakdown(
        accounts: List<Account>,
        debts: List<Debt>,
        emis: List<Emi>,
        records: List<BorrowLendRecord>
    ): LiabilityBreakdown = LiabilityBreakdown(
        debts = totalDebtOutstanding(debts),
        emiOutstanding = totalEmiOutstanding(emis),
        moneyBorrowed = totalBorrowed(records),
        creditCardOutstanding = creditCardOutstanding(accounts)
    )

    /**
     * Estimated Net Financial Position = Total Assets - Total Liabilities.
     *
     * Note what is deliberately not double counted: a debt or EMI tracked
     * against a dedicated LOAN or EMI account contributes through its debt and
     * EMI records, and such accounts are never summed as assets.
     */
    fun netPosition(
        accounts: List<Account>,
        debts: List<Debt>,
        emis: List<Emi>,
        records: List<BorrowLendRecord>
    ): NetPosition = NetPosition(
        assets = assetBreakdown(accounts, records),
        liabilities = liabilityBreakdown(accounts, debts, emis, records)
    )

    // ---------------------------------------------------------------------
    // Status resolution - derived, never stored
    // ---------------------------------------------------------------------

    fun debtStatus(debt: Debt, today: String = getCurrentDay()): DebtStatus = when {
        debt.isSettled -> DebtStatus.PAID
        debt.dueDate != null && daysBetween(today, debt.dueDate) < 0L -> DebtStatus.OVERDUE
        debt.amountPaid.isPositive -> DebtStatus.PARTIALLY_PAID
        else -> DebtStatus.ACTIVE
    }

    fun emiStatus(emi: Emi, today: String = getCurrentDay()): EmiStatus {
        if (emi.isCompleted) return EmiStatus.COMPLETED
        if (daysBetween(today, emi.startDate) > 0L) return EmiStatus.UPCOMING
        val nextDue = emi.nextDueDate ?: return EmiStatus.COMPLETED
        return if (daysBetween(today, nextDue) < 0L) EmiStatus.OVERDUE else EmiStatus.ACTIVE
    }

    fun loanStatus(record: BorrowLendRecord, today: String = getCurrentDay()): LoanStatus = when {
        record.isSettled -> LoanStatus.SETTLED
        record.dueDate != null && daysBetween(today, record.dueDate) < 0L -> LoanStatus.OVERDUE
        record.amountSettled.isPositive -> LoanStatus.PARTIALLY_SETTLED
        else -> LoanStatus.ACTIVE
    }

    // ---------------------------------------------------------------------
    // Upcoming payments
    // ---------------------------------------------------------------------

    /**
     * Merges every outstanding obligation into one chronological list, so the
     * dashboard renders a single timeline instead of three competing ones.
     *
     * Sorted by due date, which puts anything overdue at the top where it
     * cannot be pushed off the list by items that are merely due soon.
     */
    fun upcomingPayments(
        debts: List<Debt>,
        emis: List<Emi>,
        records: List<BorrowLendRecord>,
        today: String = getCurrentDay(),
        limit: Int = Int.MAX_VALUE
    ): List<UpcomingPayment> {
        val entries = buildList {
            debts.filter { !it.isSettled }.forEach { debt ->
                val due = nextDebtDueDate(debt, today) ?: return@forEach
                add(
                    UpcomingPayment(
                        id = "debt-" + debt.id,
                        kind = UpcomingPaymentKind.DEBT,
                        title = debt.name,
                        subtitle = debt.lender,
                        amount = debt.nextPaymentAmount,
                        dueDate = due,
                        daysUntilDue = daysBetween(today, due)
                    )
                )
            }

            emis.filter { !it.isCompleted }.forEach { emi ->
                val due = emi.nextDueDate ?: return@forEach
                add(
                    UpcomingPayment(
                        id = "emi-" + emi.id,
                        kind = UpcomingPaymentKind.EMI,
                        title = emi.name,
                        subtitle = emi.provider + " - installment " +
                            (emi.installmentsPaid + 1) + " of " + emi.totalInstallments,
                        amount = emi.monthlyAmount,
                        dueDate = due,
                        daysUntilDue = daysBetween(today, due)
                    )
                )
            }

            records.filter { !it.isSettled }.forEach { record ->
                val due = record.dueDate ?: return@forEach
                val borrowed = record.direction == LoanDirection.BORROWED
                add(
                    UpcomingPayment(
                        id = "loan-" + record.id,
                        kind = if (borrowed) UpcomingPaymentKind.BORROWED else UpcomingPaymentKind.LENT,
                        title = record.personName,
                        subtitle = if (borrowed) "You owe" else "Owed to you",
                        amount = record.remainingAmount,
                        dueDate = due,
                        daysUntilDue = daysBetween(today, due)
                    )
                )
            }
        }

        return entries.sortedWith(compareBy({ it.dueDate }, { it.title })).take(limit)
    }

    /**
     * Projects the next due date for a debt from its anchor due date and payment
     * frequency, so a recurring debt whose first due date has passed reports a
     * sensible next date instead of staying permanently overdue.
     */
    fun nextDebtDueDate(debt: Debt, today: String = getCurrentDay()): String? {
        val anchor = debt.dueDate ?: return null
        if (debt.paymentFrequency == PaymentFrequency.ONE_TIME) return anchor
        if (daysBetween(today, anchor) >= 0L) return anchor

        debt.paymentFrequency.periodMonths?.let { months ->
            var candidate = anchor
            // Bounded so a badly dated record can never spin here.
            repeat(MAX_PROJECTION_CYCLES) {
                if (daysBetween(today, candidate) >= 0L) return candidate
                candidate = addMonths(candidate, months)
            }
            return candidate
        }

        debt.paymentFrequency.periodDays?.let { days ->
            var candidate = anchor
            repeat(MAX_PROJECTION_CYCLES) {
                if (daysBetween(today, candidate) >= 0L) return candidate
                candidate = addDays(candidate, days)
            }
            return candidate
        }

        return anchor
    }

    // ---------------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------------

    /** Repayment progress as 0f..1f, safe when [total] is zero. */
    fun progress(paid: Money, total: Money): Float = paid.ratioOf(total)

    private const val MAX_PROJECTION_CYCLES = 480
}
