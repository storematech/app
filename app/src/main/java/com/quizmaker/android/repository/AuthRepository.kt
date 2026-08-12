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
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
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

    /**
     * Native "Sign in with Google" via Android Credential Manager (see LoginScreen.kt for where
     * [googleIdToken]/[rawNonce] come from) — no Firebase involved. This is the same Google OAuth
     * identity the website's Supabase Google sign-in already uses, so an existing web account logs
     * straight into its existing profile/data here rather than creating a new one.
     *
     * A brand-new account was previously left relying solely on [getCurrentProfile]'s
     * create-if-missing fallback to populate its `profiles` row — in practice that fallback wasn't
     * reliably succeeding for Google sign-ins (RLS/timing right after a fresh session, or a DB
     * trigger keyed off email-signup's metadata shape), leaving the user with no profile row at
     * all: a blank "My Profile", a "?" avatar, a repeated generic error toast (every profile-
     * dependent screen retrying and re-failing the same fallback insert), and quiz/question
     * creation failing wherever it's foreign-keyed to `profiles.id`. Upserting explicitly here,
     * right after sign-in, mirrors exactly what [signUp] already does for the email/password path.
     */
    suspend fun signInWithGoogleIdToken(googleIdToken: String, rawNonce: String): AppResult<Unit> = safeCall {
        supabase.auth.signInWith(IDToken) {
            idToken = googleIdToken
            provider = Google
            nonce = rawNonce
        }
        val user = supabase.auth.currentUserOrNull()
        if (user != null) {
            // Create-if-missing only — every sign-in (not just the first) reaches this point, so a
            // blind upsert here would silently clobber a name the user later set via My Profile.
            val existing = supabase.from("profiles")
                .select { filter { eq("id", user.id) } }
                .decodeSingleOrNull<ProfileDto>()
            if (existing == null) {
                val fallbackName = user.email?.substringBefore("@").orEmpty()
                supabase.from("profiles").upsert(
                    ProfileInsertDto(id = user.id, email = user.email, name = fallbackName)
                )
            }
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

    suspend fun getCurrentProfile(notifyOnError: Boolean = true): AppResult<Profile> = safeCall(notifyOnError) {
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
            createdAt = profileDto.createdAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
            businessLogo = profileDto.businessLogo,
            address = profileDto.address
        )
    }
}
