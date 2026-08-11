package com.quizmaker.android.core.alert

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class AlertType { SUCCESS, ERROR }

data class AppAlert(
    val message: String,
    val type: AlertType,
    // Optional "UNDO"-style trailing action — only success alerts use this today (e.g. bookmarking
    // a question, marking a revision item done). null on every plain alert, including all errors.
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

/**
 * App-wide toast/strip alert channel. A plain object rather than a Hilt-injected singleton
 * because safeCall() (core/network/Result.kt) — the one place that needs to push an alert for
 * *every* API error app-wide — is a top-level suspend function with no DI container to pull from.
 * ViewModels use this same object directly for success alerts, so there's one consistent way to
 * reach it everywhere instead of "injected here, global there."
 */
object AlertBus {
    private val _alerts = MutableSharedFlow<AppAlert>(extraBufferCapacity = 8)
    val alerts: SharedFlow<AppAlert> = _alerts.asSharedFlow()

    suspend fun success(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        _alerts.emit(AppAlert(message, AlertType.SUCCESS, actionLabel, onAction))
    }

    suspend fun error(message: String) {
        _alerts.emit(AppAlert(message, AlertType.ERROR))
    }
}
