package com.aurex.app.data.mapper

import com.aurex.app.domain.model.User
import com.aurex.app.domain.model.themeModeOf
import com.aurex.app.db.User as DbUser

fun DbUser.toDomain(): User {
    return User(
        name = name,
        date = date,
        currencyCode = currency_code,
        themeMode = themeModeOf(theme_mode)
    )
}
