package com.quizmaker.android.core.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationPermissionDataStore by preferencesDataStore(name = "notification_permission_prefs")

/**
 * Device-local bookkeeping for the one-time "why we want notifications" interstitial — shown once
 * per account+device via the post-auth gate (SessionViewModel.resolvePostAuthGate), then never
 * again regardless of whether the user granted or denied the system permission. Worst case on a
 * reinstall this flag resets and the interstitial shows once more.
 */
@Singleton
class NotificationPermissionPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun shownKey(userId: String) = booleanPreferencesKey("notification_permission_shown_$userId")

    suspend fun hasShownPrompt(userId: String): Boolean =
        context.notificationPermissionDataStore.data.first()[shownKey(userId)] ?: false

    suspend fun markPromptShown(userId: String) {
        context.notificationPermissionDataStore.edit { prefs -> prefs[shownKey(userId)] = true }
    }
}
