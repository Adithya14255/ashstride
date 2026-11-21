package com.ashstride.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AshColors = darkColorScheme(
    primary = Ember,
    onPrimary = Ash,
    secondary = Gold,
    onSecondary = Ash,
    background = Ash,
    onBackground = Parchment,
    surface = AshRaised,
    onSurface = Parchment,
    surfaceVariant = AshHigh,
    onSurfaceVariant = Smoke,
    outline = Cinder,
)

@Composable
fun AshstrideTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AshColors, typography = AshTypography, content = content)
}
