package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AdminBluePrimaryDark,
    secondary = AdminBlueSecondaryDark,
    tertiary = AdminTealDark,
    background = AdminBackgroundDark,
    surface = AdminSurfaceDark,
    onPrimary = AdminBackgroundDark,
    onSecondary = AdminBackgroundDark,
    onTertiary = AdminBackgroundDark,
    onBackground = AdminTextPrimaryDark,
    onSurface = AdminTextPrimaryDark,
    error = RedError
)

private val LightColorScheme = lightColorScheme(
    primary = AdminBluePrimary,
    secondary = AdminBlueSecondary,
    tertiary = AdminTeal,
    background = AdminBackgroundLight,
    surface = AdminSurfaceLight,
    onPrimary = AdminSurfaceLight,
    onSecondary = AdminSurfaceLight,
    onTertiary = AdminSurfaceLight,
    onBackground = AdminTextPrimaryLight,
    onSurface = AdminTextPrimaryLight,
    error = RedError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
