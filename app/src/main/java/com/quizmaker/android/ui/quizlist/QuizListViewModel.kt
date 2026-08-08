package com.quizmaker.android.ui.quizlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.QuizRepository
import com.quizmaker.android.util.TrialStatus
import com.quizmaker.android.util.trialStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizListUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val allQuizzes: List<Quiz> = emptyList(),
    // Non-null while a search is active — a separate server-side result set rather than a client
    // filter over whatever page happens to be loaded, so search still covers every quiz the user
    // has, not just the ones scrolled into view so far.
    val searchResults: List<Quiz>? = null,
    val questionCounts: Map<String, Int> = emptyMap(),
    val responseCounts: Map<String, Int> = emptyMap(),
    val isCreationBlocked: Boolean = false,
    val showTrialPaywall: Boolean = false
) {
    val filteredQuizzes: List<Quiz>
        get() = searchResults ?: allQuizzes
}

@HiltViewModel
class QuizListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizListUiState())
    val uiState: StateFlow<QuizListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        refresh()
        loadTrialGate()
    }

    /** Independent of the quiz list load — a failed/slow trial check shouldn't block the list from showing. */
    private fun loadTrialGate() {
        viewModelScope.launch {
            val profile = (authRepository.getCurrentProfile() as? AppResult.Success)?.data ?: return@launch
            _uiState.value = _uiState.value.copy(isCreationBlocked = profile.trialStatus() is TrialStatus.Expired)
        }
    }

    /** Gate for the "Create Quiz" button — shows the paywall sheet instead of navigating when the trial's expired. */
    fun onCreateQuizClick(onAllowed: () -> Unit) {
        if (_uiState.value.isCreationBlocked) {
            _uiState.value = _uiState.value.copy(showTrialPaywall = true)
        } else {
            onAllowed()
        }
    }

    fun dismissTrialPaywall() {
        _uiState.value = _uiState.value.copy(showTrialPaywall = false)
    }

    /** Reloads the first page from scratch — used on entry and after a create/edit/delete/duplicate. */
    fun refresh() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, endReached = false)
            when (val result = quizRepository.getQuizzesPage(userId, offset = 0)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        allQuizzes = result.data,
                        endReached = result.data.size < QuizRepository.PAGE_SIZE
                    )
                    loadCounts(result.data.map { it.id })
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    /** Called when the list scrolls near the bottom. No-ops while searching — search is a capped, non-paginated fetch. */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || state.endReached || state.searchQuery.isNotBlank()) return
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            when (val result = quizRepository.getQuizzesPage(userId, offset = _uiState.value.allQuizzes.size)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        allQuizzes = _uiState.value.allQuizzes + result.data,
                        endReached = result.data.size < QuizRepository.PAGE_SIZE
                    )
                    loadCounts(result.data.map { it.id })
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoadingMore = false, errorMessage = result.message)
            }
        }
    }

    /** Two requests total (one per table), not one pair per quiz — see getQuestionCountsForQuizzes(). */
    private fun loadCounts(quizIds: List<String>) {
        if (quizIds.isEmpty()) return
        viewModelScope.launch {
            val questionCounts = (quizRepository.getQuestionCountsForQuizzes(quizIds) as? AppResult.Success)?.data
            if (questionCounts != null) {
                _uiState.value = _uiState.value.copy(questionCounts = _uiState.value.questionCounts + questionCounts)
            }
        }
        viewModelScope.launch {
            val responseCounts = (quizRepository.getResponseCountsForQuizzes(quizIds) as? AppResult.Success)?.data
            if (responseCounts != null) {
                _uiState.value = _uiState.value.copy(responseCounts = _uiState.value.responseCounts + responseCounts)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = null)
            return
        }
        val userId = authRepository.currentUserId() ?: return
        searchJob = viewModelScope.launch {
            delay(300)
            when (val result = quizRepository.searchQuizzesForUser(userId, query.trim())) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(searchResults = result.data)
                    loadCounts(result.data.map { it.id })
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
            }
        }
    }

    fun closeQuiz(quizId: String) {
        viewModelScope.launch {
            when (quizRepository.closeQuiz(quizId)) {
                is AppResult.Success -> refresh()
                is AppResult.Error -> Unit
            }
        }
    }

    fun deleteQuiz(quizId: String) {
        viewModelScope.launch {
            when (quizRepository.deleteQuiz(quizId)) {
                is AppResult.Success -> refresh()
                is AppResult.Error -> Unit
            }
        }
    }

    fun duplicateQuiz(quizId: String) {
        viewModelScope.launch {
            when (quizRepository.duplicateQuiz(quizId)) {
                is AppResult.Success -> refresh()
                is AppResult.Error -> Unit
            }
        }
    }
}
