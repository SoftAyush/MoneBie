package com.aurex.app.domain.model

import com.aurex.app.core.Money

/**
 * A place money physically sits. Every transaction, debt payment, EMI
 * installment and loan repayment settles against exactly one account, which is
 * what makes each financial event traceable back to a balance change.
 */
data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val balance: Money,
    val currencyCode: String,
    val isArchived: Boolean = false,
    val createdAt: String
) {
    /** Contributes to Total Assets. See [isAsset]. */
    val isAssetAccount: Boolean get() = type.isAsset

    /**
     * Outstanding amount owed on a credit/loan style account.
     *
     * Spending on a credit card debits it into negative territory, so the
     * amount owed is the magnitude of a negative balance. A non-negative
     * balance means the card is settled or overpaid and nothing is owed -
     * reading the magnitude unconditionally would report an overpaid card as
     * a liability.
     */
    val outstanding: Money
        get() = when {
            type.isAsset -> Money.ZERO
            balance.isNegative -> balance.absolute
            else -> Money.ZERO
        }
}
