package com.grace.app.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Royals brand palette — see CLAUDE.md DESIGN SYSTEM.
//
// IMPORTANT — these are not compile-time constants any more. Each one is a
// composable getter that reads the active palette from LocalGracePalette,
// so the same `Grace*` name resolves to Dark or Light values depending on
// the current theme. The API surface for callers is identical to the old
// `val Grace* = Color(...)` constants — every existing UI usage works
// without modification (Compose calls the getter automatically since UI
// code runs in a @Composable scope).
//
// NEVER use these names from NON-composable code (e.g. PDF rendering,
// Android Canvas outside Compose). For that path, use raw RGB literals
// — see PdfExporter.kt for the pattern.

val GraceGold: Color
    @Composable @ReadOnlyComposable
    get() = LocalGracePalette.current.gold

val GraceGoldLight: Color
    @Composable @ReadOnlyComposable
    get() = LocalGracePalette.current.goldLight

val GraceGoldDim: Color
    @Composable @ReadOnlyComposable
    get() = LocalGracePalette.current.goldDim

val GraceDeepBlue: Color
    @Composable @ReadOnlyComposable
    get() = LocalGracePalette.current.deepBlue

val GraceCardBg: Color
    @Composable @ReadOnlyComposable
    get() = LocalGracePalette.current.cardBg

val GraceCardAlt: Color
    @Composable @ReadOnlyComposable
    get() = LocalGracePalette.current.cardAlt

val GraceCream: Color
    @Composable @ReadOnlyComposable
    get() = LocalGracePalette.current.cream

val GraceCreamDim: Color
    @Composable @ReadOnlyComposable
    get() = LocalGracePalette.current.creamDim

val GraceMuted: Color
    @Composable @ReadOnlyComposable
    get() = LocalGracePalette.current.muted

val GraceBlue: Color
    @Composable @ReadOnlyComposable
    get() = LocalGracePalette.current.blue

val GraceGreen: Color
    @Composable @ReadOnlyComposable
    get() = LocalGracePalette.current.green

val GraceRose: Color
    @Composable @ReadOnlyComposable
    get() = LocalGracePalette.current.rose

val GracePurple: Color
    @Composable @ReadOnlyComposable
    get() = LocalGracePalette.current.purple

val GraceOrange: Color
    @Composable @ReadOnlyComposable
    get() = LocalGracePalette.current.orange
