package com.quizmaker.android.ui.tools.feedback

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.FeedbackSubmission
import com.quizmaker.android.data.model.excludingIdentityFields
import com.quizmaker.android.repository.FeedbackFormRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedbackSubmissionsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val formTitle: String = "",
    /** field id -> label, so each submission's `answers` map (keyed by field id) reads as "Label: value". */
    val fieldLabels: Map<String, String> = emptyMap(),
    val submissions: List<FeedbackSubmission> = emptyList()
)

/** Read-only list of who submitted a feedback form — same shape as OnboardingSubmissionsViewModel. */
@HiltViewModel
class FeedbackSubmissionsViewModel @Inject constructor(
    private val repository: FeedbackFormRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val formId: String = checkNotNull(savedStateHandle["formId"])

    private val _uiState = MutableStateFlow(FeedbackSubmissionsUiState())
    val uiState: StateFlow<FeedbackSubmissionsUiState> = _uiState.asStateFlow()

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
                        fieldLabels = formResult.data.questions.excludingIdentityFields().associate { it.id to it.label },
                        submissions = submissionsResult.data
                    )
                }
            }
        }
    }
}
