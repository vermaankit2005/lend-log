package com.lendlog.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val LightColorScheme = lightColorScheme(
    primary              = Ink,
    onPrimary            = N0,
    primaryContainer     = InkSoft,
    onPrimaryContainer   = Ink,
    secondary            = N100,
    onSecondary          = N800,
    secondaryContainer   = N100,
    onSecondaryContainer = N700,
    tertiary             = Success,
    onTertiary           = N0,
    tertiaryContainer    = SuccessSoft,
    onTertiaryContainer  = Success,
    background           = N50,
    onBackground         = N800,
    surface              = N0,
    onSurface            = N800,
    surfaceVariant       = N100,
    onSurfaceVariant     = N500,
    outline              = N200,
    outlineVariant       = N200,
    error                = Danger,
    onError              = N0,
    errorContainer       = DangerSoft,
    onErrorContainer     = Danger,
)

private val DarkColorScheme = darkColorScheme(
    primary              = Color(0xFF93B8FF),
    onPrimary            = Color(0xFF0F1E3D),
    primaryContainer     = Color(0xFF1E3560),
    onPrimaryContainer   = Color(0xFFB8D4FF),
    secondary            = Color(0xFF2A2A2D),
    onSecondary          = Color(0xFFEFEFF1),
    secondaryContainer   = Color(0xFF2A2A2D),
    onSecondaryContainer = Color(0xFFD4D4D8),
    tertiary             = Color(0xFF4ADE80),
    onTertiary           = Color(0xFF052E16),
    tertiaryContainer    = Color(0xFF14532D),
    onTertiaryContainer  = Color(0xFFBBF7D0),
    background           = Color(0xFF0F0F11),
    onBackground         = Color(0xFFEFEFF1),
    surface              = Color(0xFF1A1A1D),
    onSurface            = Color(0xFFEFEFF1),
    surfaceVariant       = Color(0xFF2A2A2D),
    onSurfaceVariant     = Color(0xFFA1A1AA),
    outline              = Color(0xFF3F3F46),
    outlineVariant       = Color(0xFF3F3F46),
    error                = Color(0xFFF87171),
    onError              = Color(0xFF3B0C0C),
    errorContainer       = Color(0xFF5B1C1C),
    onErrorContainer     = Color(0xFFFCA5A5),
    scrim                = Color(0xFF000000),
    inverseSurface       = Color(0xFFEFEFF1),
    inverseOnSurface     = Color(0xFF18181B),
    inversePrimary       = Ink,
)

@Composable
fun LendLogTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = LendLogTypography,
        content     = content
    )
}
