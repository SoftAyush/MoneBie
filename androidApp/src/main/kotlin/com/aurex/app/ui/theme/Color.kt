package com.aurex.app.ui.theme

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

val AurexPrimary = Palette700
val AurexOnPrimary = Color(0xFFFFFFFF)
val AurexPrimaryContainer = Palette100
val AurexOnPrimaryContainer = Palette900

val AurexSecondary = Palette500

/**
 * Dark ink on the cyan, not white.
 *
 * #00B4D8 is a light colour: white on it lands near 2.4:1, which fails even the
 * large-text bar. The deepest navy gives about 6.4:1.
 */
val AurexOnSecondary = Palette900
val AurexSecondaryContainer = Palette300
val AurexOnSecondaryContainer = Palette900

val AurexTertiary = Palette900
val AurexOnTertiary = Color(0xFFFFFFFF)
val AurexTertiaryContainer = Palette300
val AurexOnTertiaryContainer = Palette900

/*
 * Neutrals carry a blue cast so they sit with the ramp, but stay well
 * desaturated: #03045E as body text would be exhausting to read at length.
 */
val AurexBackgroundLight = Color(0xFFF6FCFE)
val AurexSurfaceLight = Color(0xFFFFFFFF)
val AurexSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val AurexSurfaceContainerLowLight = Color(0xFFF8FCFE)
val AurexSurfaceContainerLight = Color(0xFFEFF8FC)
val AurexSurfaceContainerHighLight = Color(0xFFE6F4FA)
val AurexSurfaceContainerHighestLight = Color(0xFFDDEFF7)
val AurexSurfaceVariantLight = Color(0xFFDDEFF7)
val AurexOnSurfaceLight = Color(0xFF0A1A2A)
val AurexOnSurfaceVariantLight = Color(0xFF43606F)
val AurexOutlineLight = Color(0xFF6C8FA6)

// ---- Dark roles -------------------------------------------------------------

val AurexPrimaryDark = Palette300
val AurexOnPrimaryDark = Palette900

/** A darkened #0077B6, so pale text on the hero card clears 4.5:1. */
val AurexPrimaryContainerDark = Color(0xFF005B8A)
val AurexOnPrimaryContainerDark = Palette100

val AurexSecondaryDark = Palette500
val AurexOnSecondaryDark = Palette900
val AurexSecondaryContainerDark = Color(0xFF0A3A55)
val AurexOnSecondaryContainerDark = Palette300

val AurexTertiaryDark = Palette100
val AurexOnTertiaryDark = Palette900
val AurexTertiaryContainerDark = Color(0xFF023E63)
val AurexOnTertiaryContainerDark = Palette100

val AurexBackgroundDark = Color(0xFF061224)
val AurexSurfaceDark = Color(0xFF0A1A2E)
val AurexSurfaceContainerLowestDark = Color(0xFF04101F)
val AurexSurfaceContainerLowDark = Color(0xFF0C1E33)
val AurexSurfaceContainerDark = Color(0xFF10233A)
val AurexSurfaceContainerHighDark = Color(0xFF152C45)
val AurexSurfaceContainerHighestDark = Color(0xFF1B3450)
val AurexSurfaceVariantDark = Color(0xFF1A2E42)
val AurexOnSurfaceDark = Color(0xFFDCE9F2)
val AurexOnSurfaceVariantDark = Color(0xFFA8C2D4)
val AurexOutlineDark = Color(0xFF55738A)

// ---- Error ------------------------------------------------------------------

val Error = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF410002)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)
