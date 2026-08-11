package com.quizmaker.android.ui.tools.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.OnboardingSubmission
import com.quizmaker.android.data.model.excludingIdentityFields
import com.quizmaker.android.repository.OnboardingFormRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingSubmissionsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val formTitle: String = "",
    /** field id -> label, so each submission's `answers` map (keyed by field id) reads as "Label: value". */
    val fieldLabels: Map<String, String> = emptyMap(),
    val submissions: List<OnboardingSubmission> = emptyList()
)

/** Read-only list of who filled out an onboarding form — same shape as ClassWeakLearnersViewModel. */
@HiltViewModel
class OnboardingSubmissionsViewModel @Inject constructor(
    private val repository: OnboardingFormRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val formId: String = checkNotNull(savedStateHandle["formId"])

    private val _uiState = MutableStateFlow(OnboardingSubmissionsUiState())
    val uiState: StateFlow<OnboardingSubmissionsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val formResult = repository.getForm(formId)
            val submissionsResult = repository.getSubmissions(formId)
            when {
                formResult is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = formResult.message)
                submissionsResult is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = submissionsResult.message)
                formResult is AppResult.Success && submissionsResult is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        formTitle = formResult.data.title,
                        fieldLabels = formResult.data.fields.excludingIdentityFields().associate { it.id to it.label },
                        submissions = submissionsResult.data
                    )
                }
            }
        }
    }
}
