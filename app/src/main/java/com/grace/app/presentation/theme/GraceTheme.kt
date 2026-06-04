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

// Material3 ColorScheme derived from a GracePalette. Kept private — UI code
// reads colors via the top-level Grace* properties (Color.kt) so a palette
// swap propagates everywhere, while Material components (Button, Switch,
// etc.) get their defaults from this scheme. Both stay in lockstep.
//
// We pick darkColorScheme() vs lightColorScheme() by the palette's bg
// luminance so future palettes (high-contrast, etc.) flow through without
// touching this function. The defaults differ in subtle ways (elevation
// shadows, ripple alpha, default text color) so the right scheme matters.
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
    // In-app text-size override. 1.0 = system default; values above scale
    // every .sp value across the app. Persisted to DataStore by the user
    // via the Text Size section in Settings, then passed in from
    // MainActivity. Defaults to 1.0 so callers that don't yet pipe it
    // through (e.g. previews) get the original behavior.
    fontScale: Float = 1.0f,
    // The active palette. Phase D will derive this from the user's theme
    // preference (Dark / Light / System) in MainActivity. For now every
    // caller gets Dark — Light is a placeholder alias until the palette
    // is designed.
    palette: GracePalette = DarkGracePalette,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    // Detect a light palette by background luminance — works for any future
    // palette (e.g. high-contrast white) without an explicit isLight flag.
    val isLightPalette = palette.deepBlue.luminance() > 0.5f
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status + nav bar colors track the active palette's background
            // so the system chrome doesn't clash when a Light theme is active.
            window.statusBarColor = palette.deepBlue.toArgb()
            window.navigationBarColor = palette.deepBlue.toArgb()
            // Light backgrounds need DARK status/nav icons (and vice versa);
            // the platform helper flips icon color automatically when these
            // flags are set.
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = isLightPalette
            controller.isAppearanceLightNavigationBars = isLightPalette
        }
    }

    // Wrap the app in a Density that multiplies the .sp scale by the
    // user's preference. We keep the platform density (px-per-dp) intact
    // so views stay the same physical size — only typography rescales.
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
