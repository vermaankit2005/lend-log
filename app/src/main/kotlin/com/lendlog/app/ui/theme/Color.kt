package com.lendlog.app.ui.theme

import androidx.compose.ui.graphics.Color

// === Brand ===
val Ink        = Color(0xFF1B2A4E)
val InkPressed = Color(0xFF13203D)
val InkSoft    = Color(0xFFEEF1F8)

// === Neutral scale (warm-tinted, Linear/Wise inspired) ===
val N0   = Color(0xFFFFFFFF)
val N50  = Color(0xFFFAFAFA)
val N100 = Color(0xFFF4F4F5)
val N200 = Color(0xFFE9E9EC)
val N300 = Color(0xFFD4D4D8)
val N400 = Color(0xFFA1A1AA)
val N500 = Color(0xFF71717A)
val N600 = Color(0xFF52525B)
val N700 = Color(0xFF3F3F46)
val N800 = Color(0xFF27272A)
val N900 = Color(0xFF18181B)

// === Semantic ===
val Danger      = Color(0xFFDC2626)
val DangerSoft  = Color(0xFFFEF2F2)
val Success     = Color(0xFF15803D)
val SuccessSoft = Color(0xFFF0FDF4)
val Warning     = Color(0xFFB45309)
val WarningSoft = Color(0xFFFFFBEB)

// === Aliases — keep old names pointing to new tokens so other files compile ===
val TealPrimary    = Ink
val TealDeep       = InkPressed
val TealLight      = Color(0xFF5B7DB5)
val WarmBackground = N50
val CardSurface    = N0
val SandSecondary  = N100
val MutedText      = N500
val BorderColor    = N200
val OverdueRed     = Danger
val SuccessGreen   = Success
val CharcoalText   = N800
val AccentAmber    = N100
