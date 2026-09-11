package com.quizmaker.android.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.quizmaker.android.R

// Poppins is the bold, rounded display font used across app.yunolms.com's headings
// ("Dashboard", "Your Quizzes", "Questions"...). Body text keeps the platform default
// sans-serif to preserve readability at small sizes, matching the site's own pairing.
val PoppinsFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_extrabold, FontWeight.ExtraBold)
)

// Certificate Designer decorative fonts (see ui/certificate/CertificateDrawing.kt) — all four are
// variable fonts, so each is deliberately a single-instance FontFamily with one Font() call rather
// than a multi-weight family like PoppinsFamily above (risk of resource-compiler issues with
// variable fonts that can't be build-verified in this environment).
val UnifrakturMaguntiaFamily = FontFamily(Font(R.font.unifraktur_maguntia_regular))
val DancingScriptFamily = FontFamily(Font(R.font.dancing_script))
val EbGaramondFamily = FontFamily(Font(R.font.eb_garamond))
val EbGaramondItalicFamily = FontFamily(Font(R.font.eb_garamond_italic))

val QuizMakerTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)
