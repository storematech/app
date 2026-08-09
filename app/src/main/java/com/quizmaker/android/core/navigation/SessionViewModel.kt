package com.quizmaker.android.core.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.prefs.NotificationPermissionPrefs
import com.quizmaker.android.core.prefs.TrialPrefs
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.util.TrialStatus
import com.quizmaker.android.util.trialStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Whether the initial nav graph should show the loading gate, the auth flow, the main app, or one of the one-time/recurring post-login interstitials. */
enum class SessionGate { LOADING, LOGGED_OUT, LOGGED_IN, NEEDS_PHONE, NEEDS_NOTIFICATION_PERMISSION, TRIAL_STARTED, TRIAL_ENDED }

/** Drives which nav graph (auth vs. main) is shown, based on the restored Supabase session. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val trialPrefs: TrialPrefs,
    private val notificationPermissionPrefs: NotificationPermissionPrefs,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val sessionStatus: StateFlow<SessionStatus> get() = authRepository.sessionStatus

    // Deciding "restoration is done" and reading the settled session value happen in the same
    // coroutine step below, so the UI can never observe isRestoringSession=false alongside a
    // stale/pre-restore sessionStatus read (which previously caused a brief Login-screen flash
    // on cold start before flipping to Dashboard once the real session was in).
    private val _gate = MutableStateFlow(SessionGate.LOADING)
    val gate: StateFlow<SessionGate> = _gate.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.awaitSessionRestore()
            _gate.value = if (sessionStatus.value !is SessionStatus.Authenticated) {
                SessionGate.LOGGED_OUT
            } else {
                resolvePostAuthGate()
            }
        }
    }

    /**
     * Single post-authentication gate check, reused for both the cold-start gate above and the
     * nav graph's live sign-in transition. Priority: phone number, then trial state (premium
     * accounts skip straight through). Fails open to LOGGED_IN on a profile-fetch error — a
     * network hiccup should never lock a signed-in user out of the app.
     */
    suspend fun resolvePostAuthGate(): SessionGate {
        // notifyOnError = false: this fires right after every sign-in/sign-up as an internal
        // gate check, and already fails open to LOGGED_IN below — a transient hiccup here isn't
        // a real error the user needs to see (was surfacing a confusing "high demand" toast on
        // top of the CollectPhone screen for brand-new accounts).
        val profile = (authRepository.getCurrentProfile(notifyOnError = false) as? AppResult.Success)?.data
            ?: return SessionGate.LOGGED_IN
        if (profile.phoneNumber.isBlank()) return SessionGate.NEEDS_PHONE
        if (needsNotificationPermissionPrompt(profile.id)) return SessionGate.NEEDS_NOTIFICATION_PERMISSION
        return when (profile.trialStatus()) {
            TrialStatus.Premium -> SessionGate.LOGGED_IN
            TrialStatus.Expired -> SessionGate.TRIAL_ENDED
            is TrialStatus.Active ->
                if (trialPrefs.hasShownTrialStarted(profile.id)) SessionGate.LOGGED_IN else SessionGate.TRIAL_STARTED
        }
    }

    /** True right after phone collection for a new account, and right after sign-in for a
     *  returning one whenever the system permission isn't already granted — but only once ever
     *  per account+device (see [NotificationPermissionPrefs]), and never below API 33 where this
     *  runtime permission doesn't exist. */
    private suspend fun needsNotificationPermissionPrompt(userId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return false
        return !notificationPermissionPrefs.hasShownPrompt(userId)
    }
}
