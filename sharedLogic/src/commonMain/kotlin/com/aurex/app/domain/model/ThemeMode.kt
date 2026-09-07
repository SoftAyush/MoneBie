package com.aurex.app.domain.model

/**
 * How the app decides between the light and dark palettes.
 *
 * SYSTEM is a real option rather than the absence of a choice: a user who wants
 * the app to follow their phone has chosen that, and it must survive being set
 * explicitly after picking one of the other two.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

val ThemeMode.displayName: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }

/** Unknown names fall back to following the phone rather than throwing. */
fun themeModeOf(name: String?): ThemeMode =
    ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.SYSTEM
