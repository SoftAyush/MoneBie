package com.aurex.app.domain.model

enum class TransactionType {
    /** Money entering an account. */
    INCOME,

    /** Money leaving an account. */
    EXPENSE,

    /** Money moved between two of the user's own accounts - net-neutral. */
    TRANSFER
}

/** Sign applied to an account balance when this transaction is posted. */
val TransactionType.balanceSign: Int
    get() = when (this) {
        TransactionType.INCOME -> 1
        TransactionType.EXPENSE -> -1
        TransactionType.TRANSFER -> 0
    }
