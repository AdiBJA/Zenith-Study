package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NatSage,
    onPrimary = Color.White,
    primaryContainer = NatLightBeige,
    onPrimaryContainer = NatTextDark,
    secondary = NatRose,
    onSecondary = Color.White,
    tertiary = NatBurntOrange,
    background = NatBg,
    onBackground = NatTextDark,
    surface = NatWhite,
    onSurface = NatTextDark,
    surfaceVariant = NatLightBeige,
    onSurfaceVariant = NatTaupe
)

private val LightColorScheme = lightColorScheme(
    primary = NatSage,
    onPrimary = Color.White,
    primaryContainer = NatLightBeige,
    onPrimaryContainer = NatTextDark,
    secondary = NatRose,
    onSecondary = Color.White,
    tertiary = NatBurntOrange,
    background = NatBg,
    onBackground = NatTextDark,
    surface = NatWhite,
    onSurface = NatTextDark,
    surfaceVariant = NatLightBeige,
    onSurfaceVariant = NatTaupe
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark focus theme by default for immersive study
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
