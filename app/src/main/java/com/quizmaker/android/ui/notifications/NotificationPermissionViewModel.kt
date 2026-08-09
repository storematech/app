package com.quizmaker.android.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.analytics.AnalyticsLogger
import com.quizmaker.android.core.prefs.NotificationPermissionPrefs
import com.quizmaker.android.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** How the user left the interstitial — drives which analytics event [NotificationPermissionViewModel.markPromptShown] logs. */
enum class NotificationPermissionOutcome { GRANTED, DENIED, SKIPPED }

@HiltViewModel
class NotificationPermissionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val notificationPermissionPrefs: NotificationPermissionPrefs,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    /** Marks the interstitial as seen (so it never shows again for this account+device) regardless
     *  of whether the user actually granted the system permission or dismissed it. */
    fun markPromptShown(outcome: NotificationPermissionOutcome) {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch { notificationPermissionPrefs.markPromptShown(userId) }
        when (outcome) {
            NotificationPermissionOutcome.GRANTED -> analyticsLogger.logNotificationPermissionResult(granted = true)
            NotificationPermissionOutcome.DENIED -> analyticsLogger.logNotificationPermissionResult(granted = false)
            NotificationPermissionOutcome.SKIPPED -> analyticsLogger.logNotificationPermissionSkipped()
        }
    }
}
