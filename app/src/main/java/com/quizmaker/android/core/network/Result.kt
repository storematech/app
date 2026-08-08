package com.quizmaker.android.core.network

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

suspend fun <T> safeCall(block: suspend () -> T): AppResult<T> {
    return try {
        AppResult.Success(block())
    } catch (t: Throwable) {
        AppResult.Error(t.message ?: "Something went wrong. Please try again.", t)
    }
}
