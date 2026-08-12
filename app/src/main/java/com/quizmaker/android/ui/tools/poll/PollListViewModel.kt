package com.quizmaker.android.ui.tools.poll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.alert.AlertBus
import com.quizmaker.android.core.analytics.AnalyticsLogger
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Poll
import com.quizmaker.android.data.model.PollOption
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.PollRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PollListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val polls: List<Poll> = emptyList(),
    val voteCounts: Map<String, Int> = emptyMap(),
    val isEditSheetOpen: Boolean = false,
    val editingPoll: Poll? = null,
    val isSaving: Boolean = false
)

/** Powers Tools → Poll — the management list, same shape as OnboardingFormListViewModel/FeedbackFormListViewModel. */
@HiltViewModel
class PollListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val repository: PollRepository,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(PollListUiState())
    val uiState: StateFlow<PollListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.getPolls(userId)) {
                is AppResult.Success -> {
                    val polls = result.data
                    val counts = (repository.getVoteCounts(polls.map { it.id }) as? AppResult.Success)?.data.orEmpty()
                    _uiState.value = _uiState.value.copy(isLoading = false, polls = polls, voteCounts = counts)
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun openCreateSheet() {
        _uiState.value = _uiState.value.copy(isEditSheetOpen = true, editingPoll = null)
    }

    fun openEditSheet(poll: Poll) {
        _uiState.value = _uiState.value.copy(isEditSheetOpen = true, editingPoll = poll)
    }

    fun dismissEditSheet() {
        _uiState.value = _uiState.value.copy(isEditSheetOpen = false, editingPoll = null)
    }

    fun savePoll(
        question: String,
        description: String,
        options: List<PollOption>,
        allowMultiple: Boolean,
        showResults: Boolean,
        closesAt: String?,
        isActive: Boolean
    ) {
        val userId = authRepository.currentUserId() ?: return
        if (question.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter the poll question.")
            return
        }
        if (options.any { it.label.isBlank() } || options.size < 2) {
            _uiState.value = _uiState.value.copy(errorMessage = "Add at least 2 answer options.")
            return
        }
        val editingId = _uiState.value.editingPoll?.id
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val result = if (editingId != null) {
                repository.updatePoll(
                    pollId = editingId,
                    question = question.trim(),
                    description = description.trim().ifBlank { null },
                    options = options,
                    allowMultiple = allowMultiple,
                    showResults = showResults,
                    closesAt = closesAt,
                    isActive = isActive
                )
            } else {
                repository.createPoll(
                    userId = userId,
                    question = question.trim(),
                    description = description.trim().ifBlank { null },
                    options = options,
                    allowMultiple = allowMultiple,
                    showResults = showResults,
                    closesAt = closesAt,
                    isActive = isActive
                )
            }
            when (result) {
                is AppResult.Success -> {
                    if (editingId == null) analyticsLogger.logToolCreated("poll")
                    val updatedPolls = if (editingId != null) {
                        _uiState.value.polls.map { if (it.id == editingId) result.data else it }
                    } else {
                        listOf(result.data) + _uiState.value.polls
                    }
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isEditSheetOpen = false,
                        editingPoll = null,
                        polls = updatedPolls
                    )
                    AlertBus.success(if (editingId != null) "Poll updated" else "Poll created")
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
            }
        }
    }

    fun toggleActive(poll: Poll) {
        val newActive = !poll.isActive
        _uiState.value = _uiState.value.copy(
            polls = _uiState.value.polls.map { if (it.id == poll.id) it.copy(isActive = newActive) else it }
        )
        analyticsLogger.logToolActiveToggled("poll", newActive)
        viewModelScope.launch { repository.setActive(poll.id, newActive) }
    }

    fun deletePoll(pollId: String) {
        _uiState.value = _uiState.value.copy(polls = _uiState.value.polls.filterNot { it.id == pollId })
        analyticsLogger.logToolDeleted("poll")
        viewModelScope.launch {
            repository.deletePoll(pollId)
            AlertBus.success("Poll deleted")
        }
    }
}
