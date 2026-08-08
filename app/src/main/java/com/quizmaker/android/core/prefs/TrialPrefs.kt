package com.quizmaker.android.core.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.trialDataStore by preferencesDataStore(name = "trial_prefs")

/**
 * Device-local bookkeeping for the one-time trial-started congrats screen — NOT the trial gate
 * itself (that's always derived server-side from profiles.created_at via [com.quizmaker.android.util.trialStatus]).
 * Worst case on a reinstall this flag resets and the congrats screen shows once more; it can
 * never be used to dodge the paywall since the paywall doesn't read this at all.
 */
@Singleton
class TrialPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun shownKey(userId: String) = booleanPreferencesKey("trial_started_shown_$userId")

    suspend fun hasShownTrialStarted(userId: String): Boolean =
        context.trialDataStore.data.first()[shownKey(userId)] ?: false

    suspend fun markTrialStartedShown(userId: String) {
        context.trialDataStore.edit { prefs -> prefs[shownKey(userId)] = true }
    }
}
