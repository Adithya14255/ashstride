package com.ashstride.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AshColors = darkColorScheme(
    primary = Gold,
    onPrimary = Ash,
    secondary = Gold,
    onSecondary = AshDeep,
    background = Ash,
    onBackground = Parchment,
    surface = AshRaised,
    onSurface = Parchment,
    surfaceVariant = AshHigh,
    onSurfaceVariant = Smoke,
    outline = Cinder,
    outlineVariant = Cinder,
    // Dialogs and menus use the surfaceContainer roles; without these they fall back to a purple-grey default.
    surfaceContainerLowest = AshDeep,
    surfaceContainerLow = Ash,
    surfaceContainer = AshRaised,
    surfaceContainerHigh = AshHigh,
    surfaceContainerHighest = Color(0xFF2E3548),
)

@Composable
fun AshstrideTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AshColors, typography = AshTypography, content = content)
}
