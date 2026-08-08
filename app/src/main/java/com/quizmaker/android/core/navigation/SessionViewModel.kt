package com.quizmaker.android.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Whether the initial nav graph should show the loading gate, the auth flow, the main app, or the one-time phone-collection interstitial. */
enum class SessionGate { LOADING, LOGGED_OUT, LOGGED_IN, NEEDS_PHONE }

/** Drives which nav graph (auth vs. main) is shown, based on the restored Supabase session. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository
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
            _gate.value = when {
                sessionStatus.value !is SessionStatus.Authenticated -> SessionGate.LOGGED_OUT
                authRepository.needsPhoneNumber() -> SessionGate.NEEDS_PHONE
                else -> SessionGate.LOGGED_IN
            }
        }
    }

    /** Used by the nav graph's live sign-in transition (cold-start restore already covered above). */
    suspend fun needsPhoneNumber(): Boolean = authRepository.needsPhoneNumber()
}
