package com.aurex.app.ui.theme

import androidx.compose.material3.lightColorScheme

val LightColorScheme = lightColorScheme(
    primary = AurexPrimary,
    onPrimary = AurexOnPrimary,
    primaryContainer = AurexPrimaryContainer,
    onPrimaryContainer = AurexOnPrimaryContainer,

    secondary = AurexSecondary,
    onSecondary = AurexOnSecondary,
    secondaryContainer = AurexSecondaryContainer,
    onSecondaryContainer = AurexOnSecondaryContainer,

    tertiary = AurexTertiary,
    onTertiary = AurexOnTertiary,
    tertiaryContainer = AurexTertiaryContainer,
    onTertiaryContainer = AurexOnTertiaryContainer,

    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    background = AurexBackgroundLight,
    onBackground = AurexOnSurfaceLight,
    surface = AurexSurfaceLight,
    onSurface = AurexOnSurfaceLight,
    surfaceVariant = AurexSurfaceVariantLight,
    onSurfaceVariant = AurexOnSurfaceVariantLight,

    // Set explicitly: text fields and the currency sheet read these, and an
    // unset role falls back to Material's baseline neutral, which is grey and
    // off-brand.
    surfaceContainerLowest = AurexSurfaceContainerLowestLight,
    surfaceContainerLow = AurexSurfaceContainerLowLight,
    surfaceContainer = AurexSurfaceContainerLight,
    surfaceContainerHigh = AurexSurfaceContainerHighLight,
    surfaceContainerHighest = AurexSurfaceContainerHighestLight,

    outline = AurexOutlineLight
)
