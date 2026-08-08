package com.quizmaker.android.core.network

import com.quizmaker.android.core.alert.AlertBus

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

suspend fun <T> safeCall(block: suspend () -> T): AppResult<T> {
    return try {
        AppResult.Success(block())
    } catch (t: Throwable) {
        val message = t.toUserMessage()
        AlertBus.error(message)
        AppResult.Error(message, t)
    }
}

/**
 * Our own repositories only ever throw IllegalStateException/IllegalArgumentException, via
 * error(...)/require(...), with a short hand-written message that's already meant for the user
 * (e.g. "Enter a valid email address.") — those pass through unchanged. Anything else came from
 * the network/SDK layer, which can embed the entire failed request — URL, headers, even the
 * Authorization/apikey bearer tokens — in its exception message (this leaked once, in a raw
 * AI-generation error). Never show those verbatim: one consistent, friendly fallback instead,
 * regardless of the underlying cause (timeout, offline, upstream 503, whatever).
 */
private fun Throwable.toUserMessage(): String {
    if (this is IllegalStateException || this is IllegalArgumentException) {
        return message?.trim()?.takeIf { it.isNotBlank() } ?: GENERIC_API_ERROR_MESSAGE
    }
    return GENERIC_API_ERROR_MESSAGE
}
