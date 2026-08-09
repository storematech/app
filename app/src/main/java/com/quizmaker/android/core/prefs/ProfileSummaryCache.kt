package com.quizmaker.android.core.prefs

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Synchronous, best-effort local mirror of a few `profiles` fields — just enough to paint the
 * More screen's premium banner correctly on the very first frame, before the real network fetch
 * resolves. Plain SharedPreferences rather than DataStore specifically because this needs a
 * synchronous read at ViewModel-construction time; a Flow-based store can't seed a
 * MutableStateFlow's initial value without an async round trip, which is exactly the gap (the
 * banner popping in gold, or briefly showing the wrong one, right as the screen appears) this
 * exists to close. Unlike [MoreStateCache] in MoreViewModel.kt (in-memory, cleared on every
 * process restart), this survives app restarts too. Always overwritten with the latest server
 * truth right after every successful profile fetch; never used to grant access itself.
 */
@Singleton
class ProfileSummaryCache @Inject constructor(
    @ApplicationContext context: Context
) {
    data class Snapshot(val name: String, val email: String, val isPremium: Boolean, val licenseExpiredDate: String?)

    private val prefs = context.getSharedPreferences("profile_summary_cache", Context.MODE_PRIVATE)

    fun read(userId: String): Snapshot? {
        if (!prefs.contains(key(userId, FIELD_IS_PREMIUM))) return null
        return Snapshot(
            name = prefs.getString(key(userId, FIELD_NAME), "").orEmpty(),
            email = prefs.getString(key(userId, FIELD_EMAIL), "").orEmpty(),
            isPremium = prefs.getBoolean(key(userId, FIELD_IS_PREMIUM), false),
            licenseExpiredDate = prefs.getString(key(userId, FIELD_LICENSE_EXPIRY), null)
        )
    }

    fun write(userId: String, snapshot: Snapshot) {
        prefs.edit {
            putString(key(userId, FIELD_NAME), snapshot.name)
            putString(key(userId, FIELD_EMAIL), snapshot.email)
            putBoolean(key(userId, FIELD_IS_PREMIUM), snapshot.isPremium)
            putString(key(userId, FIELD_LICENSE_EXPIRY), snapshot.licenseExpiredDate)
        }
    }

    private fun key(userId: String, field: String) = "${userId}_$field"

    private companion object {
        const val FIELD_NAME = "name"
        const val FIELD_EMAIL = "email"
        const val FIELD_IS_PREMIUM = "isPremium"
        const val FIELD_LICENSE_EXPIRY = "licenseExpiredDate"
    }
}
