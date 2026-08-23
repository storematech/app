package com.quizmaker.android.core.network

import android.util.Log
import com.quizmaker.android.core.alert.AlertBus
import io.github.jan.supabase.exceptions.RestException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * Wraps the outcome of a repository call so ViewModels never deal with raw
 * exceptions from the Supabase SDK directly.
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : AppResult<Nothing>()
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onError(action: (String) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(message)
    return this
}

/** Shown for every API/network-layer failure app-wide — see toUserMessage() for why this is a single fixed string. */
const val GENERIC_API_ERROR_MESSAGE = "We're facing high demand. Please try again."

/**
 * [notifyOnError] defaults to true (the app-wide banner). Pass false only for calls whose
 * caller already handles the failure silently/gracefully on its own (e.g. a background gate
 * check that fails open) — a raw failure there is expected/handled, not something the user
 * needs to be told about, and popping the banner for it would just be noise.
 */
suspend fun <T> safeCall(notifyOnError: Boolean = true, block: suspend () -> T): AppResult<T> {
    return try {
        AppResult.Success(block())
    } catch (t: Throwable) {
        // The user only ever sees GENERIC_API_ERROR_MESSAGE (see toUserMessage()'s KDoc for why) —
        // this is the one place the real cause survives, for `adb logcat -s AppResult` while debugging.
        Log.e("AppResult", "safeCall failed", t)
        val message = t.toUserMessage()
        if (notifyOnError) AlertBus.error(message)
        AppResult.Error(message, t)
    }
}

@Serializable
private data class EdgeFunctionErrorBody(val success: Boolean = true, val error: String? = null)

private val edgeFunctionErrorJson = Json { ignoreUnknownKeys = true }

/**
 * [RestException.error] is the failed response's raw BODY text only — never its `.message`, which
 * the SDK builds by appending the full request URL and every header (including the Authorization/
 * apikey bearer token — this leaked once, in a raw AI-generation error) for debugging. Every one of
 * our own Edge Functions replies with the same `{"success": false, "error": "..."}` shape on
 * failure (see generate-quiz-ai, verify-razorpay-payment, etc.), so this just reads that one
 * already-user-facing field back out — nothing about the request itself is ever exposed. Returns
 * null (falls back to the generic message) for any body that isn't that exact shape, e.g. an
 * upstream 502/504 HTML error page.
 */
private fun RestException.edgeFunctionMessage(): String? =
    runCatching { edgeFunctionErrorJson.decodeFromString<EdgeFunctionErrorBody>(error) }
        .getOrNull()
        ?.takeIf { !it.success }
        ?.error
        ?.trim()
        ?.takeIf { it.isNotBlank() }

/**
 * Our own repositories only ever throw IllegalStateException/IllegalArgumentException, via
 * error(...)/require(...), with a short hand-written message that's already meant for the user
 * (e.g. "Enter a valid email address.") — those pass through unchanged. A RestException (thrown by
 * supabase.functions.invoke(...) for any non-2xx response — see edgeFunctionMessage()'s KDoc) tries
 * to recover our own Edge Function's own error text; anything else came from the network/SDK layer
 * more generally, which can embed sensitive request details in its exception message. Never show
 * those verbatim: one consistent, friendly fallback instead, regardless of the underlying cause
 * (timeout, offline, upstream 503, whatever).
 */
private fun Throwable.toUserMessage(): String {
    if (this is IllegalStateException || this is IllegalArgumentException) {
        return message?.trim()?.takeIf { it.isNotBlank() } ?: GENERIC_API_ERROR_MESSAGE
    }
    if (this is RestException) {
        return edgeFunctionMessage() ?: GENERIC_API_ERROR_MESSAGE
    }
    return GENERIC_API_ERROR_MESSAGE
}
