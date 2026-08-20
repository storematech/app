package com.quizmaker.android.core.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The user's explicit theme choice — SYSTEM (the default) follows the device's own light/dark setting. */
enum class AppThemeMode { LIGHT, DARK, SYSTEM }

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

/**
 * Device-local (not per-account — a device-level display preference like this shouldn't require
 * being signed in, and should stay put across sign-out/sign-in). Read by MainActivity to resolve
 * QuizMakerTheme's darkTheme param, written from More → Theme.
 */
@Singleton
class ThemePrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<AppThemeMode> = context.themeDataStore.data.map { prefs ->
        prefs[themeModeKey]?.let { stored -> runCatching { AppThemeMode.valueOf(stored) }.getOrNull() }
            ?: AppThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.themeDataStore.edit { prefs -> prefs[themeModeKey] = mode.name }
    }
}
