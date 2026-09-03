package com.quizmaker.android.core.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appIntroDataStore by preferencesDataStore(name = "app_intro_prefs")

/**
 * Device-local bookkeeping for the one-time, full-screen "what Yuno LMS can do" slide carousel
 * shown right after a brand-new account clears phone collection / notification permission — same
 * one-shot-per-account+device pattern as [ToolsIntroPrefs]/[NotificationPermissionPrefs]. Worst
 * case on a reinstall this flag resets and the carousel shows once more.
 */
@Singleton
class AppIntroPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun shownKey(userId: String) = booleanPreferencesKey("app_intro_shown_$userId")

    suspend fun hasShownIntro(userId: String): Boolean =
        context.appIntroDataStore.data.first()[shownKey(userId)] ?: false

    suspend fun markIntroShown(userId: String) {
        context.appIntroDataStore.edit { prefs -> prefs[shownKey(userId)] = true }
    }
}
