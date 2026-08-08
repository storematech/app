package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.remote.dto.ProfileUpdateDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/** Updates the `profiles` table — mirrors the Account tab in the web app's Settings.tsx. */
@Singleton
class ProfileRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun updateProfile(
        userId: String,
        name: String,
        businessName: String,
        phoneNumber: String,
        country: String
    ): AppResult<Unit> = safeCall {
        supabase.from("profiles")
            .update(
                ProfileUpdateDto(
                    name = name,
                    businessName = businessName,
                    phoneNumber = phoneNumber,
                    country = country
                )
            ) { filter { eq("id", userId) } }
        Unit
    }
}
