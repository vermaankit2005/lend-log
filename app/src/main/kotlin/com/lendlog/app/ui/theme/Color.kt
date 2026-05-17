package com.lendlog.app.ui.theme

import androidx.compose.ui.graphics.Color

// === Brand (teal) ===
val Brand        = Color(0xFF0E9AA7)   // primary teal
val BrandDeep    = Color(0xFF0A7882)   // text on white (accessible contrast)
val BrandSoft    = Color(0xFFE6F6F7)   // 10% tint for bubbles / chips

// === Neutral scale (warm, not blue-tinted) ===
val N0   = Color(0xFFFFFFFF)
val N50  = Color(0xFFF4F3F5)   // warm off-white background
val N100 = Color(0xFFEAEAEC)   // hairlines, card borders
val N200 = Color(0xFFD4D4D8)   // dividers
val N300 = Color(0xFFA1A1AA)   // disabled icons
val N400 = Color(0xFF71717A)   // placeholder text
val N500 = Color(0xFF52525B)   // secondary text
val N600 = Color(0xFF3F3F46)
val N700 = Color(0xFF27272A)
val N800 = Color(0xFF18181B)   // primary text (soft black)

// === Semantic ===
val Danger      = Color(0xFFDC2626)
val DangerSoft  = Color(0xFFFEF2F2)
val Success     = Color(0xFF16A34A)
val SuccessSoft = Color(0xFFF0FDF4)
val Warning     = Color(0xFFB45309)   // amber – text on light bg
val WarningSoft = Color(0xFFFFFBEB)   // amber – tinted card bg

// === Aliases – keep old names so other files compile without changes ===
val Ink        = Brand
val InkPressed = BrandDeep
val InkSoft    = BrandSoft
val TealPrimary    = Brand
val TealDeep       = BrandDeep
val TealLight      = Color(0xFF5BB5C0)
val WarmBackground = N50
val CardSurface    = N0
val SandSecondary  = N100
val MutedText      = N500
val BorderColor    = N100
val OverdueRed     = Danger
val SuccessGreen   = Success
val CharcoalText   = N800
val AccentAmber    = Warning
