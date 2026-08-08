package com.quizmaker.android.ui.quizlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val allQuizzes: List<Quiz> = emptyList(),
    val questionCounts: Map<String, Int> = emptyMap(),
    val responseCounts: Map<String, Int> = emptyMap()
) {
    val filteredQuizzes: List<Quiz>
        get() = if (searchQuery.isBlank()) {
            allQuizzes
        } else {
            allQuizzes.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
}

@HiltViewModel
class QuizListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizListUiState())
    val uiState: StateFlow<QuizListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = quizRepository.getQuizzesForUser(userId)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, allQuizzes = result.data)
                    loadCounts(result.data.map { it.id })
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    private fun loadCounts(quizIds: List<String>) {
        quizIds.forEach { quizId ->
            viewModelScope.launch {
                val questionCount = (quizRepository.getQuestionCountForQuiz(quizId) as? AppResult.Success)?.data
                val responseCount = (quizRepository.getResponseCountForQuiz(quizId) as? AppResult.Success)?.data
                _uiState.value = _uiState.value.copy(
                    questionCounts = if (questionCount != null) _uiState.value.questionCounts + (quizId to questionCount) else _uiState.value.questionCounts,
                    responseCounts = if (responseCount != null) _uiState.value.responseCounts + (quizId to responseCount) else _uiState.value.responseCounts
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
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
