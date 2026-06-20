package com.notdigest.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// --- Brand palette: a calm violet/indigo with warm-neutral surfaces ---

internal val Violet10 = Color(0xFF21005D)
internal val Violet40 = Color(0xFF6E56CF)
internal val Violet50 = Color(0xFF7C66E0)
internal val Violet80 = Color(0xFFCBBEFF)
internal val Violet90 = Color(0xFFE8E0FF)
internal val Violet95 = Color(0xFFF4EFFF)

internal val Teal40 = Color(0xFF1F8A82)
internal val Teal80 = Color(0xFF8AD9D1)

internal val NeutralWhite = Color(0xFFFFFFFF)
internal val NeutralBg = Color(0xFFFAF8FF)
internal val NeutralSurfaceVariant = Color(0xFFEAE5F2)
internal val NeutralOutline = Color(0xFFCDC6DA)
internal val Ink = Color(0xFF1A1820)
internal val InkMuted = Color(0xFF55525F)

internal val DarkBg = Color(0xFF0E0D12)
internal val DarkSurface = Color(0xFF16151C)
internal val DarkSurfaceElevated = Color(0xFF1F1D27)
internal val DarkSurfaceVariant = Color(0xFF2A2833)
internal val DarkOutline = Color(0xFF454353)
internal val DarkInk = Color(0xFFEAE5F2)
internal val DarkInkMuted = Color(0xFFA9A4B6)

internal val Positive = Color(0xFF2E9E6B)
internal val PositiveContainerLight = Color(0xFFD6F2E2)
internal val PositiveContainerDark = Color(0xFF143D2A)

val LightColors = lightColorScheme(
    primary = Violet40,
    onPrimary = NeutralWhite,
    primaryContainer = Violet90,
    onPrimaryContainer = Violet10,
    secondary = Teal40,
    onSecondary = NeutralWhite,
    secondaryContainer = Teal80,
    onSecondaryContainer = Color(0xFF00201D),
    tertiary = Color(0xFF7D5260),
    background = NeutralBg,
    onBackground = Ink,
    surface = NeutralWhite,
    onSurface = Ink,
    surfaceVariant = NeutralSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = NeutralOutline,
    outlineVariant = Color(0xFFE3DEEC),
    error = Color(0xFFBA1A1A),
    onError = NeutralWhite,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surfaceTint = Violet40,
)

val DarkColors = darkColorScheme(
    primary = Violet80,
    onPrimary = Violet10,
    primaryContainer = Color(0xFF52429F),
    onPrimaryContainer = Violet95,
    secondary = Teal80,
    onSecondary = Color(0xFF00201D),
    secondaryContainer = Color(0xFF0D4A44),
    onSecondaryContainer = Teal80,
    tertiary = Color(0xFFEFB8C8),
    background = DarkBg,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkInkMuted,
    outline = DarkOutline,
    outlineVariant = Color(0xFF332F3D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceTint = Violet80,
)
