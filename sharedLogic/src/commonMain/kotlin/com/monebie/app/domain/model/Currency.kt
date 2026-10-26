package com.monebie.app.domain.model

data class Currency(
    val code: String,
    val symbol: String,
    val name: String,
    val flag: String,
    val popular: Boolean
)
