package com.quizmaker.android.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.quizmaker.android.BuildConfig
import java.security.MessageDigest
import java.util.UUID

/**
 * Launches Android's native account picker (Credential Manager — not Firebase, not the older/
 * deprecated GoogleSignInClient) to get a Google ID token, then hands it and the matching raw
 * nonce to [onIdToken] to complete the Supabase sign-in via AuthRepository.signInWithGoogleIdToken.
 * [onError] covers both a missing GOOGLE_WEB_CLIENT_ID (see local.properties) and a genuine
 * picker failure — but not a plain user-cancelled picker, which is silently a no-op like tapping
 * away from any other picker UI.
 */
suspend fun launchGoogleSignIn(
    context: Context,
    onIdToken: (idToken: String, rawNonce: String) -> Unit,
    onError: (String) -> Unit
) {
    if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
        onError("Google sign-in isn't configured yet.")
        return
    }

    // Supabase validates this nonce against the one embedded in the ID token's claims, so the
    // hashed version goes to Google (it only ever sees/embeds the hash) while Supabase gets the
    // raw value back to re-hash and compare itself.
    val rawNonce = UUID.randomUUID().toString()
    val hashedNonce = MessageDigest.getInstance("SHA-256")
        .digest(rawNonce.toByteArray())
        .joinToString("") { "%02x".format(it) }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .setNonce(hashedNonce)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val result = CredentialManager.create(context).getCredential(context = context, request = request)
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
        onIdToken(googleIdTokenCredential.idToken, rawNonce)
    } catch (e: GetCredentialCancellationException) {
        // User dismissed the account picker — not an error.
    } catch (e: GetCredentialException) {
        onError(e.message?.ifBlank { null } ?: "Couldn't sign in with Google. Please try again.")
    } catch (e: GoogleIdTokenParsingException) {
        onError("Couldn't verify that Google account. Please try again.")
    }
}
