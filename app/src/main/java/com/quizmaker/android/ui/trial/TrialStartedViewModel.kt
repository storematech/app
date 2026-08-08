package com.quizmaker.android.ui.trial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.prefs.TrialPrefs
import com.quizmaker.android.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrialStartedUiState(val isNavigating: Boolean = false)

@HiltViewModel
class TrialStartedViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val trialPrefs: TrialPrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrialStartedUiState())
    val uiState: StateFlow<TrialStartedUiState> = _uiState.asStateFlow()

    /** Marks the congrats screen as seen (so it never shows again for this account+device) then
     *  hands off to [onReady] after a short "getting things ready" beat. */
    fun startJourney(onReady: () -> Unit) {
        val userId = authRepository.currentUserId()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isNavigating = true)
            if (userId != null) trialPrefs.markTrialStartedShown(userId)
            delay(650)
            onReady()
        }
    }
}
