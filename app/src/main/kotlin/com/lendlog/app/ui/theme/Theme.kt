package com.lendlog.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LendLogColorScheme = lightColorScheme(
    primary             = Ink,
    onPrimary           = N0,
    primaryContainer    = InkSoft,
    onPrimaryContainer  = Ink,
    secondary           = N100,
    onSecondary         = N800,
    secondaryContainer  = N100,
    onSecondaryContainer = N700,
    tertiary            = Success,
    onTertiary          = N0,
    tertiaryContainer   = SuccessSoft,
    onTertiaryContainer = Success,
    background          = N50,
    onBackground        = N800,
    surface             = N0,
    onSurface           = N800,
    surfaceVariant      = N100,
    onSurfaceVariant    = N500,
    outline             = N200,
    outlineVariant      = N200,
    error               = Danger,
    onError             = N0,
    errorContainer      = DangerSoft,
    onErrorContainer    = Danger,
)

@Composable
fun LendLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LendLogColorScheme,
        typography  = LendLogTypography,
        content     = content
    )
}
