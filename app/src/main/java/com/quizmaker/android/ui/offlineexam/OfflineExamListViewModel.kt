package com.quizmaker.android.ui.offlineexam

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

data class OfflineExamListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val exams: List<Quiz> = emptyList(),
    // Same batched-count convention as QuizListViewModel.loadCounts — one request for every exam's
    // question count instead of one round trip per row.
    val questionCounts: Map<String, Int> = emptyMap(),
    // Same convention as QuizListUiState.deletingQuizId — drives the row's inline "Deleting…" state.
    val deletingQuizId: String? = null
)

/**
 * Backs the Offline Exams list — a real `quizzes` row flagged `is_offline_exam = true`, listed
 * separately from the main Quiz List (see QuizRepository.getOfflineExams). Each row's own
 * "Download" button navigates to OfflineExamPaperPreviewScreen (see OfflineExamListScreen) rather
 * than exporting directly from here.
 */
@HiltViewModel
class OfflineExamListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineExamListUiState())
    val uiState: StateFlow<OfflineExamListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = quizRepository.getOfflineExams(userId)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, exams = result.data)
                    loadCounts(result.data.map { it.id })
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    private fun loadCounts(quizIds: List<String>) {
        if (quizIds.isEmpty()) return
        viewModelScope.launch {
            val counts = (quizRepository.getQuestionCountsForQuizzes(quizIds) as? AppResult.Success)?.data
            if (counts != null) {
                _uiState.value = _uiState.value.copy(questionCounts = _uiState.value.questionCounts + counts)
            }
        }
    }

    /** Same `quizzes` row delete `QuizListViewModel.deleteQuiz` already uses — an Offline Exam is
     *  a normal quiz row, so there's nothing offline-exam-specific about deleting one. */
    fun deleteExam(quizId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deletingQuizId = quizId)
            when (val result = quizRepository.deleteQuiz(quizId)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(deletingQuizId = null)
                    refresh()
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(deletingQuizId = null, errorMessage = result.message)
            }
        }
    }
}
