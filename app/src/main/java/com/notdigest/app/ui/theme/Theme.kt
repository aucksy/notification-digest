package com.notdigest.app.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.notdigest.app.domain.model.ThemeMode

/** Extra, non-Material brand tokens layered on top of the Material color scheme. */
@Immutable
data class BrandColors(
    val positive: Color,
    val positiveContainer: Color,
    val heroGradient: List<Color>,
    val surfaceElevated: Color,
    val isDark: Boolean,
)

val LocalBrandColors = staticCompositionLocalOf {
    BrandColors(
        positive = Positive,
        positiveContainer = PositiveContainerLight,
        heroGradient = listOf(Violet50, Violet40),
        surfaceElevated = NeutralWhite,
        isDark = false,
    )
}

object NotDigestTheme {
    val brand: BrandColors
        @Composable @ReadOnlyComposable
        get() = LocalBrandColors.current
}

@Composable
fun NotDigestTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }

    val brand = BrandColors(
        positive = Positive,
        positiveContainer = if (dark) PositiveContainerDark else PositiveContainerLight,
        heroGradient = if (dark) listOf(Color(0xFF52429F), Color(0xFF2C2350)) else listOf(Violet50, Violet40),
        surfaceElevated = if (dark) DarkSurfaceElevated else NeutralWhite,
        isDark = dark,
    )

    CompositionLocalProvider(LocalBrandColors provides brand) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NotDigestTypography,
            shapes = NotDigestShapes,
            content = content,
        )
    }
}
