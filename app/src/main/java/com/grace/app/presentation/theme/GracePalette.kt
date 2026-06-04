package com.grace.app.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Royals brand palette held as a single data class so the entire color set
 * can be swapped atomically when the user picks Light/Dark/System theme.
 * UI code reads colors via the top-level `Grace*` properties in Color.kt,
 * which are composable getters that delegate to LocalGracePalette.current —
 * so a palette swap propagates through every screen on the next frame
 * without touching call sites.
 *
 * Adding a NEW color: add the field here, add it to BOTH Dark and Light
 * instances (or alias the Light value to Dark for now), and add the
 * composable getter in Color.kt.
 */
@Immutable
data class GracePalette(
    val gold:       Color,
    val goldLight:  Color,
    val goldDim:    Color,
    val deepBlue:   Color,
    val cardBg:     Color,
    val cardAlt:    Color,
    val cream:      Color,
    val creamDim:   Color,
    val muted:      Color,
    val blue:       Color,
    val green:      Color,
    val rose:       Color,
    val purple:     Color,
    val orange:     Color
)

/** Dark theme palette — the original Royals brand. */
val DarkGracePalette = GracePalette(
    gold       = Color(0xFFC9A84C),
    goldLight  = Color(0xFFE8C96A),
    goldDim    = Color(0xFF7A6430),
    deepBlue   = Color(0xFF08090F),
    cardBg     = Color(0xFF0E1020),
    cardAlt    = Color(0xFF111428),
    cream      = Color(0xFFEDE5D8),
    creamDim   = Color(0xFFA09080),
    muted      = Color(0xFF3A3D52),
    blue       = Color(0xFF4A7CFF),
    green      = Color(0xFF3ECF8E),
    rose       = Color(0xFFE05C7A),
    purple     = Color(0xFF9B5DE5),
    orange     = Color(0xFFF4A261)
)

/**
 * Light theme palette — starter values shipped 2026-06-03.
 *
 * Design rationale:
 *  - Background = cream (#FAF7EE), echoes the Royals logo's white inside
 *  - Text = dark navy (#1B1D2E) for ~16:1 contrast (WCAG AAA)
 *  - Primary gold darkened (#A8841F) so it reads on white — pure brand
 *    gold (#C9A84C) disappears against light surfaces
 *  - Green darkened to match the laurel green in the Royals crest
 *  - Other accents (blue/rose/purple/orange) deepened for AA contrast
 *  - goldDim is INVERTED: in Dark mode it's a dimmer/darker gold for
 *    labels; in Light mode it's a lighter/softer gold for the same
 *    visual weight (label should fade against bg, not dominate it)
 *
 * Iterate by editing the hex values here — palette is the single source
 * of truth and propagates app-wide via LocalGracePalette.
 */
val LightGracePalette = GracePalette(
    gold       = Color(0xFFA8841F),
    goldLight  = Color(0xFFC9A14C),
    goldDim    = Color(0xFFB89544),
    deepBlue   = Color(0xFFFAF7EE),
    cardBg     = Color(0xFFFFFFFF),
    cardAlt    = Color(0xFFF6F0DE),
    cream      = Color(0xFF1B1D2E),
    creamDim   = Color(0xFF5A5E78),
    muted      = Color(0xFFCFCED8),
    blue       = Color(0xFF2A56D6),
    green      = Color(0xFF1E9A65),
    rose       = Color(0xFFC74660),
    purple     = Color(0xFF7842C8),
    orange     = Color(0xFFD37A36)
)

/**
 * CompositionLocal carrying the currently active palette. Defaults to Dark
 * so any composable that renders OUTSIDE a GraceTheme (previews,
 * standalone tests) gets sensible values instead of a crash.
 */
val LocalGracePalette = compositionLocalOf { DarkGracePalette }
