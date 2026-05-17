package com.lendlog.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.lendlog.app.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)

// Single family — DM Sans in 3 weights. No display serif.
val DmSans = FontFamily(
    Font(googleFont = GoogleFont("DM Sans"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("DM Sans"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("DM Sans"), fontProvider = provider, weight = FontWeight.SemiBold),
)

val LendLogTypography = Typography(
    // Only used on paywall / onboarding hero
    displayLarge = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 34.sp,
        lineHeight    = 40.sp,
        letterSpacing = (-0.6).sp
    ),
    displayMedium = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 28.sp,
        lineHeight    = 34.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineLarge = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 28.sp,
        lineHeight    = 34.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 24.sp,
        lineHeight    = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 22.sp,
        lineHeight    = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 17.sp,
        lineHeight    = 24.sp,
        letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.Medium,
        fontSize      = 15.sp,
        lineHeight    = 22.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 13.sp,
        lineHeight    = 18.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.Normal,
        fontSize      = 13.sp,
        lineHeight    = 18.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 15.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.3.sp
    ),
    // Eyebrow labels — call .uppercase() at use sites
    labelSmall = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 11.sp,
        lineHeight    = 14.sp,
        letterSpacing = 0.5.sp
    )
)
