package com.quizmaker.android.core.theme

import androidx.compose.ui.graphics.Color

// YunoLMS brand palette — pure blue primary (#2563EB, the exact theme-color yunolms.com
// ships in its own <meta> tag / CSS), replacing the earlier indigo/pink gradient.
val BrandIndigo = Color(0xFF2563EB)
val BrandIndigoDark = Color(0xFF1D4ED8)
val BrandIndigoLight = Color(0xFFDBEAFE)
val GradientStart = Color(0xFF2563EB)
val GradientEnd = Color(0xFF2563EB)

val AppBackground = Color(0xFFF5F5FB)
val SurfaceWhite = Color(0xFFFFFFFF)
val BorderGray = Color(0xFFE5E7EB)

val TextPrimary = Color(0xFF111827)
val TextSecondary = Color(0xFF6B7280)
val TextMuted = Color(0xFF9CA3AF)

val SuccessGreen = Color(0xFF16A34A)
val WarningAmber = Color(0xFFD97706)
val ErrorRed = Color(0xFFEF4444)

// Classic Adobe-style red used for PDF export/open icons, so they read as a file type at a glance.
val PdfRed = Color(0xFFE53935)

// Gold gradient for the "Get Premium" promo banner, deliberately distinct from the brand indigo
// used elsewhere so it reads as a separate, premium upsell rather than a regular action.
val PremiumGoldStart = Color(0xFFF59E0B)
val PremiumGoldEnd = Color(0xFFFBBF24)

// Deep red gradient for the sale-day banner — reads as "sale" without tipping into a cheap,
// clearance-bin red; kept distinct from the gold "Get Premium" banner (different call to action).
val SaleRedStart = Color(0xFFDC2626)
val SaleRedEnd = Color(0xFF9F1239)
// Soft red tint — the sale-day equivalent of BrandIndigoLight, used for the Pricing screen's plan
// card header background while a sale is live.
val SaleRedLight = Color(0xFFFEE2E2)

// Stat-tile accent pairs (soft background + saturated icon color), matching the
// Dashboard's Total Quizzes / Question Bank / Completions / Avg Score / Reported tiles.
val StatBlueBg = Color(0xFFDDE8FE)
val StatBlueIcon = Color(0xFF3B82F6)
val StatPurpleBg = Color(0xFFEDE4FE)
val StatPurpleIcon = Color(0xFF9333EA)
val StatGreenBg = Color(0xFFD7F5E3)
val StatGreenIcon = Color(0xFF16A34A)
val StatAmberBg = Color(0xFFFCF0D2)
val StatAmberIcon = Color(0xFFF59E0B)
val StatRedBg = Color(0xFFFBDFDF)
val StatRedIcon = Color(0xFFEF4444)

// Score pill colors used on the Leaderboard (green/orange/red bands).
val ScoreHighBg = Color(0xFF22C55E)
val ScoreMidBg = Color(0xFFF59E0B)
val ScoreLowBg = Color(0xFFEF4444)

val SurfaceDark = Color(0xFF1C1B1F)
