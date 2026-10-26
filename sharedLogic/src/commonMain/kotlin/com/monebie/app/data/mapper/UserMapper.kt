package com.monebie.app.data.mapper

import com.monebie.app.domain.model.User
import com.monebie.app.domain.model.themeModeOf
import com.monebie.app.db.User as DbUser

fun DbUser.toDomain(): User {
    return User(
        name = name,
        date = date,
        currencyCode = currency_code,
        themeMode = themeModeOf(theme_mode)
    )
}
