package com.quizmaker.android.ui.masterpaper

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MasterPaperUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val quizTitle: String = "",
    val questions: List<Question> = emptyList()
)

@HiltViewModel
class MasterPaperViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: String = checkNotNull(savedStateHandle["quizId"])

    private val _uiState = MutableStateFlow(MasterPaperUiState())
    val uiState: StateFlow<MasterPaperUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val quizResult = quizRepository.getQuizById(quizId)
            val questionsResult = quizRepository.getQuestionsForQuiz(quizId)
            when {
                quizResult is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoading = false, errorMessage = quizResult.message)
                questionsResult is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoading = false, errorMessage = questionsResult.message)
                quizResult is AppResult.Success && questionsResult is AppResult.Success -> _uiState.value =
                    _uiState.value.copy(isLoading = false, quizTitle = quizResult.data.title, questions = questionsResult.data)
            }
        }
    }
}
