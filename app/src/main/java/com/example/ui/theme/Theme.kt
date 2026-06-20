package com.sahand.wooadmin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary               = PrimaryIndigoDark,
    onPrimary             = OnPrimaryDark,
    primaryContainer      = PrimaryContainerDark,
    onPrimaryContainer    = OnPrimaryContainerDark,
    secondary             = SecondaryVioletDark,
    onSecondary           = OnPrimaryDark,
    tertiary              = TertiaryEmeraldDark,
    onTertiary            = OnPrimaryDark,
    background            = BackgroundDark,
    onBackground          = OnSurfaceDark,
    surface               = SurfaceDark,
    onSurface             = OnSurfaceDark,
    surfaceVariant        = SurfaceVarDark,
    onSurfaceVariant      = OnSurfaceVarDark,
    error                 = RedError,
    outline               = OnSurfaceVarDark.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary               = PrimaryIndigo,
    onPrimary             = OnPrimaryLight,
    primaryContainer      = PrimaryContainerLight,
    onPrimaryContainer    = OnPrimaryContainerLight,
    secondary             = SecondaryViolet,
    onSecondary           = OnPrimaryLight,
    tertiary              = TertiaryEmerald,
    onTertiary            = OnPrimaryLight,
    background            = BackgroundLight,
    onBackground          = OnSurfaceLight,
    surface               = SurfaceLight,
    onSurface             = OnSurfaceLight,
    surfaceVariant        = SurfaceVarLight,
    onSurfaceVariant      = OnSurfaceVarLight,
    error                 = RedError,
    outline               = OnSurfaceVarLight.copy(alpha = 0.5f)
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
