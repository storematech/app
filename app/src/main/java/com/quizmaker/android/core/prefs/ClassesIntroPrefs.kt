package com.quizmaker.android.core.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.classesIntroDataStore by preferencesDataStore(name = "classes_intro_prefs")

/**
 * Device-local bookkeeping for the one-time "what Classes can do" interstitial shown the first
 * time an account opens More → Classes — same one-shot-per-account+device pattern as
 * [NotificationPermissionPrefs] / [ToolsIntroPrefs]. Worst case on a reinstall this flag resets
 * and the interstitial shows once more.
 */
@Singleton
class ClassesIntroPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun shownKey(userId: String) = booleanPreferencesKey("classes_intro_shown_$userId")

    suspend fun hasShownIntro(userId: String): Boolean =
        context.classesIntroDataStore.data.first()[shownKey(userId)] ?: false

    suspend fun markIntroShown(userId: String) {
        context.classesIntroDataStore.edit { prefs -> prefs[shownKey(userId)] = true }
    }
}
