package com.monebie.app.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * The brand ramp, straight from colorPalatte.jpg. Every blue in the app comes
 * from one of these five steps - nothing is invented in between.
 */
val Palette900 = Color(0xFF03045E) // deepest navy
val Palette700 = Color(0xFF0077B6) // mid blue
val Palette500 = Color(0xFF00B4D8) // cyan
val Palette300 = Color(0xFF90E0EF) // light cyan
val Palette100 = Color(0xFFCAF0F8) // palest

// ---- Light roles ------------------------------------------------------------

val MoneBiePrimary = Palette700
val MoneBieOnPrimary = Color(0xFFFFFFFF)
val MoneBiePrimaryContainer = Palette100
val MoneBieOnPrimaryContainer = Palette900

val MoneBieSecondary = Palette500

/**
 * Dark ink on the cyan, not white.
 *
 * #00B4D8 is a light colour: white on it lands near 2.4:1, which fails even the
 * large-text bar. The deepest navy gives about 6.4:1.
 */
val MoneBieOnSecondary = Palette900
val MoneBieSecondaryContainer = Palette300
val MoneBieOnSecondaryContainer = Palette900

val MoneBieTertiary = Palette900
val MoneBieOnTertiary = Color(0xFFFFFFFF)
val MoneBieTertiaryContainer = Palette300
val MoneBieOnTertiaryContainer = Palette900

/*
 * Neutrals carry a blue cast so they sit with the ramp, but stay well
 * desaturated: #03045E as body text would be exhausting to read at length.
 */
val MoneBieBackgroundLight = Color(0xFFF6FCFE)
val MoneBieSurfaceLight = Color(0xFFFFFFFF)
val MoneBieSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val MoneBieSurfaceContainerLowLight = Color(0xFFF8FCFE)
val MoneBieSurfaceContainerLight = Color(0xFFEFF8FC)
val MoneBieSurfaceContainerHighLight = Color(0xFFE6F4FA)
val MoneBieSurfaceContainerHighestLight = Color(0xFFDDEFF7)
val MoneBieSurfaceVariantLight = Color(0xFFDDEFF7)
val MoneBieOnSurfaceLight = Color(0xFF0A1A2A)
val MoneBieOnSurfaceVariantLight = Color(0xFF43606F)
val MoneBieOutlineLight = Color(0xFF6C8FA6)

// ---- Dark roles -------------------------------------------------------------

val MoneBiePrimaryDark = Palette300
val MoneBieOnPrimaryDark = Palette900

/** A darkened #0077B6, so pale text on the hero card clears 4.5:1. */
val MoneBiePrimaryContainerDark = Color(0xFF005B8A)
val MoneBieOnPrimaryContainerDark = Palette100

val MoneBieSecondaryDark = Palette500
val MoneBieOnSecondaryDark = Palette900
val MoneBieSecondaryContainerDark = Color(0xFF0A3A55)
val MoneBieOnSecondaryContainerDark = Palette300

val MoneBieTertiaryDark = Palette100
val MoneBieOnTertiaryDark = Palette900
val MoneBieTertiaryContainerDark = Color(0xFF023E63)
val MoneBieOnTertiaryContainerDark = Palette100

val MoneBieBackgroundDark = Color(0xFF061224)
val MoneBieSurfaceDark = Color(0xFF0A1A2E)
val MoneBieSurfaceContainerLowestDark = Color(0xFF04101F)
val MoneBieSurfaceContainerLowDark = Color(0xFF0C1E33)
val MoneBieSurfaceContainerDark = Color(0xFF10233A)
val MoneBieSurfaceContainerHighDark = Color(0xFF152C45)
val MoneBieSurfaceContainerHighestDark = Color(0xFF1B3450)
val MoneBieSurfaceVariantDark = Color(0xFF1A2E42)
val MoneBieOnSurfaceDark = Color(0xFFDCE9F2)
val MoneBieOnSurfaceVariantDark = Color(0xFFA8C2D4)
val MoneBieOutlineDark = Color(0xFF55738A)

// ---- Error ------------------------------------------------------------------

val Error = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF410002)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)
