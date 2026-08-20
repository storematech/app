package com.quizmaker.android.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Whether the app-wide light/dark tokens in Color.kt should resolve to their dark variant —
 * provided once by [QuizMakerTheme] below. Reading this (rather than `isSystemInDarkTheme()`
 * directly) inside each color token is what lets a user's explicit Light/Dark override (see
 * ThemePrefs/ThemeViewModel, picked from More → Theme) take effect, not just the OS setting.
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun QuizMakerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        val context = LocalContext.current
        // Built here (inside the CompositionLocalProvider's content) rather than as top-level
        // vals, since primary/primaryContainer/etc. below now read the same dark-theme-aware
        // Color.kt tokens as the rest of the app, which requires a @Composable context with
        // LocalDarkTheme already provided.
        val colorScheme = when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            darkTheme -> darkColorScheme(
                primary = BrandIndigo,
                onPrimary = Color.White,
                primaryContainer = BrandIndigoDark,
                onPrimaryContainer = BrandIndigoLight,
                secondary = BrandIndigoLight,
                background = AppBackground,
                surface = SurfaceWhite,
                surfaceVariant = AppBackground,
                outline = BorderGray,
                error = ErrorRed
            )
            else -> lightColorScheme(
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
        }

        MaterialTheme(
            colorScheme = colorScheme,
            typography = QuizMakerTypography,
            content = content
        )
    }
}
