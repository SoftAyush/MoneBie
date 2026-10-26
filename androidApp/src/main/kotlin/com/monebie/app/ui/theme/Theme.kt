package com.monebie.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * The palette the app has settled on, for nested [MoneBieTheme] calls to inherit.
 *
 * Several screens wrap themselves in the theme again - the onboarding flow, the
 * setup screens, the currency selector - and without this they would each fall
 * back to the phone's setting and ignore a user who has chosen light or dark.
 */
val LocalMoneBieDarkTheme = compositionLocalOf<Boolean?> { null }

@Composable
fun MoneBieTheme(
    darkTheme: Boolean = LocalMoneBieDarkTheme.current ?: isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalMoneBieDarkTheme provides darkTheme,
        LocalFinanceColors provides if (darkTheme) DarkFinanceColors else LightFinanceColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

/**
 * Accessor for the MoneBie design system.
 */
object MoneBieTheme {
    val spacing: Spacing
        @Composable
        get() = LocalSpacing.current

    /** Semantic colours for money direction. See [FinanceColors]. */
    val finance: FinanceColors
        @Composable
        get() = LocalFinanceColors.current
}
