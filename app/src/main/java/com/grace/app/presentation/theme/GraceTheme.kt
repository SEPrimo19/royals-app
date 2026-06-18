package com.grace.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

private fun colorSchemeFor(p: GracePalette) =
    if (p.deepBlue.luminance() > 0.5f) {
        lightColorScheme(
            primary       = p.gold,
            onPrimary     = p.cardBg,
            secondary     = p.blue,
            background    = p.deepBlue,
            onBackground  = p.cream,
            surface       = p.cardBg,
            onSurface     = p.cream,
            surfaceVariant = p.cardAlt,
            error         = p.rose,
            onError       = p.cardBg
        )
    } else {
        darkColorScheme(
            primary       = p.gold,
            onPrimary     = p.deepBlue,
            secondary     = p.blue,
            background    = p.deepBlue,
            onBackground  = p.cream,
            surface       = p.cardBg,
            onSurface     = p.cream,
            surfaceVariant = p.cardAlt,
            error         = p.rose,
            onError       = p.cream
        )
    }

@Composable
fun GraceTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1.0f,
    palette: GracePalette = DarkGracePalette,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val isLightPalette = palette.deepBlue.luminance() > 0.5f
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = palette.deepBlue.toArgb()
            window.navigationBarColor = palette.deepBlue.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = isLightPalette
            controller.isAppearanceLightNavigationBars = isLightPalette
        }
    }

    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density,
        fontScale = baseDensity.fontScale * fontScale
    )

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalGracePalette provides palette
    ) {
        MaterialTheme(
            colorScheme = colorSchemeFor(palette),
            typography = GraceTypography,
            shapes = GraceShapes,
            content = content
        )
    }
}
