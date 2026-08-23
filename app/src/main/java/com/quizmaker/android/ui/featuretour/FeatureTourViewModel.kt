package com.quizmaker.android.ui.featuretour

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.util.TrialStatus
import com.quizmaker.android.util.trialStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeatureTourUiState(
    val isCreationBlocked: Boolean = false,
    val showTrialPaywall: Boolean = false
)

/**
 * Only exists to gate the "Create Quiz with AI" / "Create Quiz" CTAs on this screen the same way
 * QuizList/QuestionBank/Dashboard already do — without it, an expired-trial account (still allowed
 * onto this screen; it's gated on "not premium," not "not expired") could use these CTAs as an
 * unintended back door around the paywall. The other five CTAs (Revision/Classes/Tools/Learners/
 * Reported Questions) aren't gated anywhere else in the app either, so they navigate directly.
 */
@HiltViewModel
class FeatureTourViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeatureTourUiState())
    val uiState: StateFlow<FeatureTourUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = (authRepository.getCurrentProfile() as? AppResult.Success)?.data ?: return@launch
            _uiState.value = _uiState.value.copy(isCreationBlocked = profile.trialStatus() is TrialStatus.Expired)
        }
    }

    fun onCreateQuizClick(onAllowed: () -> Unit) = gateOnTrial(onAllowed)

    fun onOpenAiClick(onAllowed: () -> Unit) = gateOnTrial(onAllowed)

    private fun gateOnTrial(onAllowed: () -> Unit) {
        if (_uiState.value.isCreationBlocked) {
            _uiState.value = _uiState.value.copy(showTrialPaywall = true)
        } else {
            onAllowed()
        }
    }

    fun dismissTrialPaywall() {
        _uiState.value = _uiState.value.copy(showTrialPaywall = false)
    }
}
