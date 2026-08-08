package com.quizmaker.android.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.prefs.TrialPrefs
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.util.TrialStatus
import com.quizmaker.android.util.trialStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Whether the initial nav graph should show the loading gate, the auth flow, the main app, or one of the one-time/recurring post-login interstitials. */
enum class SessionGate { LOADING, LOGGED_OUT, LOGGED_IN, NEEDS_PHONE, TRIAL_STARTED, TRIAL_ENDED }

/** Drives which nav graph (auth vs. main) is shown, based on the restored Supabase session. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val trialPrefs: TrialPrefs
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
        val profile = (authRepository.getCurrentProfile() as? AppResult.Success)?.data
            ?: return SessionGate.LOGGED_IN
        if (profile.phoneNumber.isBlank()) return SessionGate.NEEDS_PHONE
        return when (profile.trialStatus()) {
            TrialStatus.Premium -> SessionGate.LOGGED_IN
            TrialStatus.Expired -> SessionGate.TRIAL_ENDED
            is TrialStatus.Active ->
                if (trialPrefs.hasShownTrialStarted(profile.id)) SessionGate.LOGGED_IN else SessionGate.TRIAL_STARTED
        }
    }
}
