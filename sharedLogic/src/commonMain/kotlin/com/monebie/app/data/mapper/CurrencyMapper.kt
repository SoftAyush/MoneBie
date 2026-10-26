package com.monebie.app.data.mapper

import com.monebie.app.domain.model.Currency
import com.monebie.app.db.Currency as DbCurrency

fun DbCurrency.toDomain(): Currency {
    return Currency(
        code    = code,
        symbol  = symbol,
        name    = name,
        flag    = flag,
        popular = popular != 0L   // SQLite stores booleans as INTEGER (0/1)
    )
}
