package com.monebie.app.domain.model

import com.monebie.app.core.Money

/**
 * Which component of the position a group represents.
 *
 * The key is what the UI switches on for icons and colour, so the labels can
 * change without any screen having to reinterpret what a group means.
 */
enum class PositionGroupKey {
    CASH,
    BANK,
    WALLET,
    OTHER_ASSETS,
    MONEY_LENT,
    DEBTS,
    EMI,
    MONEY_BORROWED,
    CREDIT_CARDS;

    val isAsset: Boolean
        get() = this == CASH || this == BANK || this == WALLET ||
            this == OTHER_ASSETS || this == MONEY_LENT
}

/**
 * A single contributor to a group: one account, one debt, one EMI, one person.
 *
 * This is what makes the overview auditable rather than a wall of totals - the
 * user can see that "Debts 7,500" is a specific set of records, not a number
 * the app arrived at somehow.
 */
data class PositionLine(
    val id: String,
    val label: String,
    val sublabel: String? = null,
    val amount: Money
)

/** A named component of the position, with the records that make it up. */
data class PositionGroup(
    val key: PositionGroupKey,
    val label: String,
    val total: Money,
    val lines: List<PositionLine> = emptyList()
) {
    val hasDetail: Boolean get() = lines.isNotEmpty()

    /** Share of the side of the balance sheet this group sits on, 0f..1f. */
    fun shareOf(sideTotal: Money): Float = total.ratioOf(sideTotal)
}

/**
 * The complete financial position, itemised.
 *
 * Everything here is computed by FinancialCalculator from the source records.
 * The screen renders it and performs no arithmetic, which is what keeps the
 * overview and the dashboard from ever disagreeing about the same figure.
 */
data class FinancialOverview(
    val currencySymbol: String = "$",
    /** yyyy-MM-dd the overview was computed for. */
    val today: String = "",
    val netPosition: NetPosition = NetPosition(),
    val assetGroups: List<PositionGroup> = emptyList(),
    val liabilityGroups: List<PositionGroup> = emptyList(),
    /**
     * Scheduled outgoings falling due this calendar month: EMI installments
     * plus debt payments. What the user has to find money for, not what they
     * owe in total.
     */
    val obligationsThisMonth: Money = Money.ZERO
) {
    val totalAssets: Money get() = netPosition.assets.total

    val totalLiabilities: Money get() = netPosition.liabilities.total

    val net: Money get() = netPosition.net

    val isPositive: Boolean get() = netPosition.isPositive

    /** Money immediately available, as opposed to lent out or held as credit. */
    val liquidAssets: Money get() = netPosition.assets.liquid

    /**
     * Share of assets consumed by liabilities.
     *
     * Reported alongside the net figure because two people with the same net
     * position can be in very different shape - one with no debt, one with
     * large offsetting amounts on both sides.
     */
    val liabilityRatio: Float get() = netPosition.liabilityRatio

    /** True once there is anything at all to describe. */
    val hasData: Boolean
        get() = !totalAssets.isZero || !totalLiabilities.isZero

    /**
     * Assets as a share of the two sides combined, for the comparison bar.
     *
     * Sizing the bar against the larger side alone would make an empty side
     * look like a full one, so both are summed.
     */
    val assetShareOfTotal: Float
        get() {
            val combined = totalAssets + totalLiabilities
            return if (combined.isZero) 0.5f else totalAssets.ratioOf(combined)
        }
}
