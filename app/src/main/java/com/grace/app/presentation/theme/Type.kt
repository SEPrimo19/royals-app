package com.grace.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// NOTE: The spec calls for Cormorant Garamond (display/scripture) + Lato (UI).
// The actual font files (res/font/) are added in a later prompt. Until then we
// alias them to system families so the project compiles and renders cleanly.
// WHY: referencing R.font.* resources that do not yet exist would break the
// build — these aliases are the single place to swap in real fonts later.
val DisplayFont: FontFamily = FontFamily.Serif      // → cormorant_garamond_semibold
val ScriptureFont: FontFamily = FontFamily.Serif    // → cormorant_garamond_italic
val UIFont: FontFamily = FontFamily.SansSerif       // → lato

val GraceTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 36.sp
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = UIFont, fontWeight = FontWeight.Bold, fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = UIFont, fontWeight = FontWeight.Normal, fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ScriptureFont, fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic, fontSize = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = UIFont, fontWeight = FontWeight.Bold, fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = UIFont, fontWeight = FontWeight.Bold, fontSize = 11.sp
    )
)
