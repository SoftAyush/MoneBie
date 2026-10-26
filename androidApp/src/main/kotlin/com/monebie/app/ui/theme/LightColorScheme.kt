package com.monebie.app.ui.theme

import androidx.compose.material3.lightColorScheme

val LightColorScheme = lightColorScheme(
    primary = MoneBiePrimary,
    onPrimary = MoneBieOnPrimary,
    primaryContainer = MoneBiePrimaryContainer,
    onPrimaryContainer = MoneBieOnPrimaryContainer,

    secondary = MoneBieSecondary,
    onSecondary = MoneBieOnSecondary,
    secondaryContainer = MoneBieSecondaryContainer,
    onSecondaryContainer = MoneBieOnSecondaryContainer,

    tertiary = MoneBieTertiary,
    onTertiary = MoneBieOnTertiary,
    tertiaryContainer = MoneBieTertiaryContainer,
    onTertiaryContainer = MoneBieOnTertiaryContainer,

    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    background = MoneBieBackgroundLight,
    onBackground = MoneBieOnSurfaceLight,
    surface = MoneBieSurfaceLight,
    onSurface = MoneBieOnSurfaceLight,
    surfaceVariant = MoneBieSurfaceVariantLight,
    onSurfaceVariant = MoneBieOnSurfaceVariantLight,

    // Set explicitly: text fields and the currency sheet read these, and an
    // unset role falls back to Material's baseline neutral, which is grey and
    // off-brand.
    surfaceContainerLowest = MoneBieSurfaceContainerLowestLight,
    surfaceContainerLow = MoneBieSurfaceContainerLowLight,
    surfaceContainer = MoneBieSurfaceContainerLight,
    surfaceContainerHigh = MoneBieSurfaceContainerHighLight,
    surfaceContainerHighest = MoneBieSurfaceContainerHighestLight,

    outline = MoneBieOutlineLight
)
