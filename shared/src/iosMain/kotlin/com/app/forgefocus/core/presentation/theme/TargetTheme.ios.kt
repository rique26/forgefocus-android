package com.app.forgefocus.core.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun rememberAppColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean
): ColorScheme {
    return if (darkTheme) DarkColorScheme else LightColorScheme
}