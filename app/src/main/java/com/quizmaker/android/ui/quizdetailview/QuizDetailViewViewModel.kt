package com.quizmaker.android.ui.quizdetailview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.QuizDetailViewData
import com.quizmaker.android.data.model.QuizDetailViewSummary
import com.quizmaker.android.data.model.StudentAnswerRow
import com.quizmaker.android.repository.QuizDetailViewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizDetailViewUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val summary: QuizDetailViewSummary? = null,
    val rows: List<StudentAnswerRow> = emptyList(),
    val hasMore: Boolean = true
)

@HiltViewModel
class QuizDetailViewViewModel @Inject constructor(
    private val repository: QuizDetailViewRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: String = checkNotNull(savedStateHandle["quizId"])

    private val _uiState = MutableStateFlow(QuizDetailViewUiState())
    val uiState: StateFlow<QuizDetailViewUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = QuizDetailViewUiState(isLoading = true)

            val summaryResult = repository.getSummary(quizId)
            val summary = when (summaryResult) {
                is AppResult.Success -> summaryResult.data
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = summaryResult.message)
                    return@launch
                }
            }

            when (val pageResult = repository.getPage(quizId, summary.gradedCount, offset = 0)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    summary = summary,
                    rows = pageResult.data,
                    hasMore = pageResult.data.size >= QuizDetailViewRepository.PAGE_SIZE
                )
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = pageResult.message)
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        val summary = state.summary ?: return
        if (state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            when (val result = repository.getPage(quizId, summary.gradedCount, offset = _uiState.value.rows.size)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    rows = _uiState.value.rows + result.data,
                    hasMore = result.data.size >= QuizDetailViewRepository.PAGE_SIZE
                )
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoadingMore = false, errorMessage = result.message)
            }
        }
    }

    /** Fetches every respondent (not just what's scrolled into view) for CSV/PDF export. */
    suspend fun loadAllForExport(): QuizDetailViewData? {
        val summary = _uiState.value.summary ?: return null
        val result = repository.getAllRows(quizId, summary.gradedCount)
        val rows = (result as? AppResult.Success)?.data ?: return null
        return QuizDetailViewData(
            quizTitle = summary.quizTitle,
            gradedCount = summary.gradedCount,
            allowMultipleAttempts = summary.allowMultipleAttempts,
            totalResponses = summary.totalResponses,
            averageScore = summary.averageScore,
            averageTimeSeconds = summary.averageTimeSeconds,
            rows = rows
        )
    }
}
