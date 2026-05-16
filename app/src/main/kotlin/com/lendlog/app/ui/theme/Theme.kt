package com.lendlog.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LendLogColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = CardSurface,
    primaryContainer = TealLight.copy(alpha = 0.15f),
    onPrimaryContainer = TealDeep,
    secondary = SandSecondary,
    onSecondary = CharcoalText,
    secondaryContainer = AccentAmber,
    onSecondaryContainer = CharcoalText,
    tertiary = SuccessGreen,
    onTertiary = CardSurface,
    background = WarmBackground,
    onBackground = CharcoalText,
    surface = CardSurface,
    onSurface = CharcoalText,
    surfaceVariant = SandSecondary,
    onSurfaceVariant = MutedText,
    outline = BorderColor,
    error = OverdueRed,
    onError = CardSurface,
)

@Composable
fun LendLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LendLogColorScheme,
        typography = LendLogTypography,
        content = content
    )
}
