package com.quizmaker.android.core.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.featureTourBannerDataStore by preferencesDataStore(name = "feature_tour_banner_prefs")

/**
 * Device-local dismiss count for the Dashboard's "View Feature" banner close (X) button. The
 * first close only hides it for the rest of the current app session (see
 * DashboardStateCache.featureTourBannerDismissedThisSession) — it's back next launch. A second
 * close, in any later session, is the one that sticks: once the count reaches
 * [PERMANENT_DISMISS_THRESHOLD] the banner is gone for good.
 */
@Singleton
class FeatureTourBannerPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun countKey(userId: String) = intPreferencesKey("feature_tour_banner_dismiss_count_$userId")

    suspend fun getDismissCount(userId: String): Int =
        context.featureTourBannerDataStore.data.first()[countKey(userId)] ?: 0

    suspend fun incrementDismissCount(userId: String) {
        context.featureTourBannerDataStore.edit { prefs ->
            prefs[countKey(userId)] = (prefs[countKey(userId)] ?: 0) + 1
        }
    }

    companion object {
        const val PERMANENT_DISMISS_THRESHOLD = 2
    }
}
