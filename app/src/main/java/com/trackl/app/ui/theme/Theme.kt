package com.trackl.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandYellow = Color(0xFFFFC928)

private val DarkColors = darkColorScheme(
    primary = BrandYellow,
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF4A3B00),
    onPrimaryContainer = Color(0xFFFFE08A),
    secondary = Color(0xFFE6C66B),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF3A3420),
    onSecondaryContainer = Color(0xFFFFE9A8),
    background = Color(0xFF111114),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF111114),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF26262C),
    onSurfaceVariant = Color(0xFFB8B8C0),
    surfaceContainer = Color(0xFF1C1C21),
    surfaceContainerHigh = Color(0xFF232329),
    surfaceContainerHighest = Color(0xFF2B2B32),
    surfaceContainerLow = Color(0xFF17171B),
    outline = Color(0xFF55555E),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A5F00),
    onPrimary = Color.White,
    primaryContainer = BrandYellow,
    onPrimaryContainer = Color(0xFF241A00),
    secondary = Color(0xFF6A5D3F),
    secondaryContainer = Color(0xFFF3E1BB),
)

@Composable
fun TracklTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
