package com.monebie.app.domain.model

import com.monebie.app.core.Money

/** How often a budget resets. Only monthly exists yet; stored by name so more can follow. */
enum class BudgetPeriod { MONTHLY }

/** A spending limit on one category. The limit only - what was spent is measured, not stored. */
data class Budget(
    val id: Long,
    val categoryId: String,
    val amount: Money,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val createdAt: String
)

/** A budget that has not been persisted yet. */
data class BudgetDraft(
    val categoryId: String,
    val amount: Money,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY
)

/**
 * One budget measured against one month's spending.
 *
 * [spent] is a sum over the ledger for the month, taken at read time, so the
 * status can never disagree with the transactions list underneath it.
 */
data class BudgetStatus(
    val budget: Budget,
    val category: TransactionCategory,
    val spent: Money,
    val transactionCount: Int
) {
    val limit: Money get() = budget.amount

    /** Negative once the limit is passed - callers decide whether to clamp. */
    val remaining: Money get() = limit - spent

    /** Share of the limit used, `0f..1f`. Saturates at 1 when over. */
    val progress: Float get() = spent.ratioOf(limit)

    /** Share of the limit used, unclamped, for "142% of budget" copy. */
    val usedRatio: Float
        get() = if (limit.isZero) 0f else (spent.minorUnits.toDouble() / limit.minorUnits).toFloat()

    val isOver: Boolean get() = spent > limit

    /** Past the point where the rest of the month is likely to overshoot. */
    val isNearLimit: Boolean get() = !isOver && progress >= NEAR_LIMIT_THRESHOLD

    val level: BudgetLevel
        get() = when {
            isOver -> BudgetLevel.OVER
            isNearLimit -> BudgetLevel.NEAR
            else -> BudgetLevel.OK
        }

    private companion object {
        const val NEAR_LIMIT_THRESHOLD = 0.8f
    }
}

/** How a budget is doing, for colour and copy. */
enum class BudgetLevel { OK, NEAR, OVER }

/**
 * Every budget for one month, with the totals across them.
 *
 * Ordered worst first: what is over is what the user opened the screen to
 * find out about.
 */
data class BudgetOverview(
    /** yyyy-MM */
    val month: String = "",
    val monthLabel: String = "",
    val currencySymbol: String = "$",
    val canGoToNextMonth: Boolean = false,
    val statuses: List<BudgetStatus> = emptyList(),
    /** Categories without a budget yet, offered when adding one. */
    val availableCategories: List<TransactionCategory> = emptyList(),
    val isLoading: Boolean = true
) {
    val totalLimit: Money get() = Money(statuses.sumOf { it.limit.minorUnits })

    val totalSpent: Money get() = Money(statuses.sumOf { it.spent.minorUnits })

    val totalRemaining: Money get() = totalLimit - totalSpent

    val overCount: Int get() = statuses.count { it.isOver }

    val nearCount: Int get() = statuses.count { it.isNearLimit }

    val hasBudgets: Boolean get() = statuses.isNotEmpty()

    /** Share of the combined limit used, `0f..1f`. */
    val overallProgress: Float get() = totalSpent.ratioOf(totalLimit)
}
