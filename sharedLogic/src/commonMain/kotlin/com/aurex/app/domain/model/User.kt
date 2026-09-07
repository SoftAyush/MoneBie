package com.aurex.app.domain.model

data class User(
    val name: String,
    val date: String,
    val currencyCode: String,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
