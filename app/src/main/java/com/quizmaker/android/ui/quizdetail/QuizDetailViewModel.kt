package com.quizmaker.android.ui.quizdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val quiz: Quiz? = null,
    val questionCount: Int = 0,
    val responseCount: Int = 0,
    val actionInProgress: Boolean = false,
    val deleted: Boolean = false
)

@HiltViewModel
class QuizDetailViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: String = checkNotNull(savedStateHandle["quizId"])

    private val _uiState = MutableStateFlow(QuizDetailUiState())
    val uiState: StateFlow<QuizDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val quizResult = quizRepository.getQuizById(quizId)
            if (quizResult is AppResult.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = quizResult.message)
                return@launch
            }
            val questionCountResult = quizRepository.getQuestionCountForQuiz(quizId)
            val responseCountResult = quizRepository.getResponseCountForQuiz(quizId)

            val questionCount = when (questionCountResult) {
                is AppResult.Success -> questionCountResult.data
                is AppResult.Error -> 0
            }
            val responseCount = when (responseCountResult) {
                is AppResult.Success -> responseCountResult.data
                is AppResult.Error -> 0
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                quiz = (quizResult as AppResult.Success).data,
                questionCount = questionCount,
                responseCount = responseCount
            )
        }
    }

    fun closeQuiz() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            quizRepository.closeQuiz(quizId)
            _uiState.value = _uiState.value.copy(actionInProgress = false)
            refresh()
        }
    }

    fun deleteQuiz() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            when (quizRepository.deleteQuiz(quizId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(actionInProgress = false, deleted = true)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(actionInProgress = false)
            }
        }
    }
}
