package com.grace.app.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

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

val LocalGracePalette = compositionLocalOf { DarkGracePalette }
