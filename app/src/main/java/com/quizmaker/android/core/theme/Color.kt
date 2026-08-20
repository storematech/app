package com.quizmaker.android.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// YunoLMS brand palette — pure blue primary (#2563EB, the exact theme-color yunolms.com
// ships in its own <meta> tag / CSS), replacing the earlier indigo/pink gradient. Kept constant
// across light/dark: a saturated brand color reads fine on either background.
val BrandIndigo = Color(0xFF2563EB)
val BrandIndigoDark = Color(0xFF1D4ED8)
val GradientStart = Color(0xFF2563EB)
val GradientEnd = Color(0xFF2563EB)

// Semantic/status colors — also kept constant; already saturated enough to read on both themes.
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

// Stat-tile accent icon colors (paired with the *Bg tokens below), matching the Dashboard's
// Total Quizzes / Question Bank / Completions / Avg Score / Reported / Learners tiles. Kept constant.
val StatBlueIcon = Color(0xFF3B82F6)
val StatPurpleIcon = Color(0xFF9333EA)
val StatGreenIcon = Color(0xFF16A34A)
val StatAmberIcon = Color(0xFFF59E0B)
val StatRedIcon = Color(0xFFEF4444)
val StatTealIcon = Color(0xFF0D9488)

// Score pill colors used on the Leaderboard (green/orange/red bands) — always white text on top,
// kept constant across themes.
val ScoreHighBg = Color(0xFF22C55E)
val ScoreMidBg = Color(0xFFF59E0B)
val ScoreLowBg = Color(0xFFEF4444)

// Kept for the small round "AI" icon buttons (e.g. the quiz list row's AI toggle) — vibrant is
// fine at icon size. The AI Summary card itself uses the light AiCard* tokens below instead.
val AiGradientStart = Color(0xFFFF5C7A)
val AiGradientMid = Color(0xFF9B5DE5)
val AiGradientEnd = Color(0xFF5B6EF5)
val AiAccent = Color(0xFF7C5CFC)

// Always-dark gradient for the branded splash/session-loading screen — intentionally NOT tied to
// the light/dark theme toggle, since it's meant to look like the app's icon/splash regardless.
val SurfaceDark = Color(0xFF1C1B1F)

/**
 * Tokens below this point flip between a light and dark value based on [LocalDarkTheme] (see
 * Theme.kt) — these are the "neutral" surface/background/text/pale-tint tokens where light vs.
 * dark genuinely need different colors, as opposed to the saturated brand/semantic colors above
 * which read fine unchanged on either background. Each is a `@Composable get()` property so every
 * existing call site (`color = TextPrimary`, `Modifier.background(AppBackground)`, etc.) keeps
 * working unchanged — the only requirement is that the call site itself be inside a @Composable
 * function, which is true everywhere these are used in this app except two spots (see
 * DesignSystem.kt's `elevatedSurface`) that were adjusted separately for this.
 */

val BrandIndigoLight: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFF1E3A5F) else Color(0xFFDBEAFE)

val AppBackground: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFF121218) else Color(0xFFF5F5FB)

val SurfaceWhite: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFF1E1E26) else Color(0xFFFFFFFF)

val BorderGray: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFF33333F) else Color(0xFFE5E7EB)

val TextPrimary: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFFF3F4F6) else Color(0xFF111827)

val TextSecondary: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFFA1A1AA) else Color(0xFF6B7280)

val TextMuted: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFF71717A) else Color(0xFF9CA3AF)

// Soft red tint — the sale-day equivalent of BrandIndigoLight, used for the Pricing screen's plan
// card header background while a sale is live.
val SaleRedLight: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFF3F1414) else Color(0xFFFEE2E2)

// Stat-tile accent backgrounds (paired with the *Icon tokens above).
val StatBlueBg: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFF1E293B) else Color(0xFFDDE8FE)
val StatPurpleBg: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFF2E1F47) else Color(0xFFEDE4FE)
val StatGreenBg: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFF14301F) else Color(0xFFD7F5E3)
val StatAmberBg: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFF3A2E10) else Color(0xFFFCF0D2)
val StatRedBg: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFF3A1414) else Color(0xFFFBDFDF)
val StatTealBg: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFF102E2B) else Color(0xFFD9F2EF)

// Light lavender tint + border for the "AI Summary" card's text/typing content — a full loud
// gradient fill read as too much for a block of text people are meant to actually read.
val AiCardBg: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFF221D33) else Color(0xFFF6F3FF)
val AiCardBorder: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFF3B3152) else Color(0xFFE4DAFF)
