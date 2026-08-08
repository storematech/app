package com.quizmaker.android.ui.quizcreated

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizCreatedUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val quiz: Quiz? = null,
    val questions: List<Question> = emptyList()
)

/** Same shape as MasterPaperViewModel — fetches the quiz + its full questions once, for both the
 *  QR/share flyer and the embedded Master Paper export section. */
@HiltViewModel
class QuizCreatedViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: String = checkNotNull(savedStateHandle["quizId"])

    private val _uiState = MutableStateFlow(QuizCreatedUiState())
    val uiState: StateFlow<QuizCreatedUiState> = _uiState.asStateFlow()

    init {
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
                    _uiState.value.copy(isLoading = false, quiz = quizResult.data, questions = questionsResult.data)
            }
        }
    }
}
