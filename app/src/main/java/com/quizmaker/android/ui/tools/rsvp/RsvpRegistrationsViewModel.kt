package com.quizmaker.android.ui.tools.rsvp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.RsvpEvent
import com.quizmaker.android.data.model.RsvpRegistration
import com.quizmaker.android.data.model.RsvpSummary
import com.quizmaker.android.data.model.summarize
import com.quizmaker.android.repository.RsvpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RsvpRegistrationsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val event: RsvpEvent? = null,
    val summary: RsvpSummary? = null,
    val registrations: List<RsvpRegistration> = emptyList()
)

/** Who registered for an event, plus a quick attendance summary — same shape as OnboardingSubmissionsViewModel/PollResultsViewModel. */
@HiltViewModel
class RsvpRegistrationsViewModel @Inject constructor(
    private val repository: RsvpRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val _uiState = MutableStateFlow(RsvpRegistrationsUiState())
    val uiState: StateFlow<RsvpRegistrationsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val eventResult = repository.getEvent(eventId)
            val registrationsResult = repository.getRegistrations(eventId)
            when {
                eventResult is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = eventResult.message)
                registrationsResult is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = registrationsResult.message)
                eventResult is AppResult.Success && registrationsResult is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        event = eventResult.data,
                        summary = eventResult.data.summarize(registrationsResult.data),
                        registrations = registrationsResult.data
                    )
                }
            }
        }
    }
}
