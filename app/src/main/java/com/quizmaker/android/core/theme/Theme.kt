package com.quizmaker.android.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = Color.White,
    primaryContainer = BrandIndigoLight,
    onPrimaryContainer = BrandIndigoDark,
    secondary = BrandIndigoDark,
    background = AppBackground,
    surface = SurfaceWhite,
    surfaceVariant = AppBackground,
    outline = BorderGray,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = BrandIndigo,
    onPrimary = Color.White,
    primaryContainer = BrandIndigoDark,
    onPrimaryContainer = BrandIndigoLight,
    secondary = BrandIndigoLight,
    background = SurfaceDark,
    surface = SurfaceDark,
    error = ErrorRed
)

@Composable
fun QuizMakerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QuizMakerTypography,
        content = content
    )
}
