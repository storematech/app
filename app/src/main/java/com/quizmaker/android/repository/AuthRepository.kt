package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.Profile
import com.quizmaker.android.data.remote.dto.CheckEmailExistsRequest
import com.quizmaker.android.data.remote.dto.CheckEmailExistsResponse
import com.quizmaker.android.data.remote.dto.ProfileDto
import com.quizmaker.android.data.remote.dto.ProfileInsertDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Supabase Auth + the `profiles` table, mirroring src/contexts/AuthContext.tsx from the
 * web app: sign up/in with email+password, keep a `profiles` row in sync with the auth user,
 * and expose the current session so the UI can react to sign-in/sign-out.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    val sessionStatus: StateFlow<SessionStatus> get() = supabase.auth.sessionStatus

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun awaitSessionRestore() {
        runCatching { supabase.auth.awaitInitialization() }
    }

    /** Powers the email-first auth screen: whether to prompt for a password (sign in) or a new one (sign up). */
    suspend fun checkEmailExists(email: String): AppResult<Boolean> = safeCall {
        val response = supabase.functions.invoke("check-email-exists") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CheckEmailExistsRequest(email = email)))
        }
        val result = json.decodeFromString<CheckEmailExistsResponse>(response.bodyAsText())
        if (!result.success) {
            error(result.error ?: "Couldn't check that email. Please try again.")
        }
        result.exists
    }

    /** @return true if the Supabase project requires email confirmation (no session yet), false if the user is already signed in. */
    suspend fun signUp(email: String, password: String, name: String): AppResult<Boolean> = safeCall {
        val user = supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            this.data = buildJsonObject { put("name", name) }
        }
        // The web app's `profiles` table is normally populated by a DB trigger on auth.users;
        // upsert defensively in case the trigger hasn't run yet (mirrors AuthContext.tsx's
        // PGRST116 "no profile found, creating one" fallback).
        val userId = user?.id ?: supabase.auth.currentUserOrNull()?.id
        if (userId != null) {
            supabase.from("profiles").upsert(
                ProfileInsertDto(id = userId, email = email, name = name)
            )
        }
        supabase.auth.currentSessionOrNull() == null
    }

    suspend fun signIn(email: String, password: String): AppResult<Unit> = safeCall {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut(): AppResult<Unit> = safeCall {
        supabase.auth.signOut()
    }

    suspend fun resetPassword(email: String): AppResult<Unit> = safeCall {
        supabase.auth.resetPasswordForEmail(email)
    }

    suspend fun changePassword(newPassword: String): AppResult<Unit> = safeCall {
        supabase.auth.updateUser { password = newPassword }
        Unit
    }

    suspend fun getCurrentProfile(): AppResult<Profile> = safeCall {
        val user = requireNotNull(supabase.auth.currentUserOrNull()) { "Not signed in" }
        val existing = supabase.from("profiles")
            .select { filter { eq("id", user.id) } }
            .decodeSingleOrNull<ProfileDto>()

        val profileDto = existing ?: run {
            val fallbackName = user.email?.substringBefore("@").orEmpty()
            supabase.from("profiles")
                .upsert(ProfileInsertDto(id = user.id, email = user.email, name = fallbackName)) { select() }
                .decodeSingle<ProfileDto>()
        }

        Profile(
            id = user.id,
            email = profileDto.email ?: user.email.orEmpty(),
            name = profileDto.name.orEmpty(),
            businessName = profileDto.businessName.orEmpty(),
            phoneNumber = profileDto.phoneNumber.orEmpty(),
            country = profileDto.country.orEmpty(),
            role = profileDto.role ?: "user",
            userType = profileDto.userType,
            licenseExpiredDate = profileDto.licenseExpiredDate,
            createdAt = profileDto.createdAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
        )
    }
}
