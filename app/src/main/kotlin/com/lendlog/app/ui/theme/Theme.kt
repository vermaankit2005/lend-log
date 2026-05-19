package com.lendlog.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private val LightColorScheme = lightColorScheme(
    primary              = Brand,
    onPrimary            = N0,
    primaryContainer     = BrandSoft,
    onPrimaryContainer   = BrandDeep,
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
    outline              = N100,
    outlineVariant       = N100,
    error                = Danger,
    onError              = N0,
    errorContainer       = DangerSoft,
    onErrorContainer     = Danger,
)

@Composable
fun LendLogTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                activity.window.statusBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = true
            }
        }
    }
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = LendLogTypography,
        content     = content
    )
}
