package com.aurex.app.core

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
