package com.quizmaker.android.ui.quizanalysis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.alert.AlertBus
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.QuizAnalysisData
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.QuizAnalysisRepository
import com.quizmaker.android.repository.RevisionRepository
import com.quizmaker.android.util.PdfBranding
import com.quizmaker.android.util.PdfBrandingProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizAnalysisUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val data: QuizAnalysisData? = null,
    // Which of this quiz's questions the signed-in user has already bookmarked "for revision" —
    // see RevisionRepository. Loaded independently of [data] so a slow/failed bookmark check
    // never blocks the main per-question breakdown from showing.
    val bookmarkedQuestionIds: Set<String> = emptySet()
)

@HiltViewModel
class QuizAnalysisViewModel @Inject constructor(
    private val repository: QuizAnalysisRepository,
    private val revisionRepository: RevisionRepository,
    private val authRepository: AuthRepository,
    private val pdfBrandingProvider: PdfBrandingProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: String = checkNotNull(savedStateHandle["quizId"])

    private val _uiState = MutableStateFlow(QuizAnalysisUiState())
    val uiState: StateFlow<QuizAnalysisUiState> = _uiState.asStateFlow()

    init {
        refresh()
        loadBookmarks()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.getAnalysis(quizId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, data = result.data)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    private fun loadBookmarks() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            val result = revisionRepository.getRevisionItems(userId)
            if (result is AppResult.Success) {
                _uiState.value = _uiState.value.copy(bookmarkedQuestionIds = result.data.map { it.question.id }.toSet())
            }
        }
    }

    /** "Add for revision" toggle on a question row — optimistic, since a bookmark is low-stakes and reversible. */
    fun toggleRevision(questionId: String) {
        val userId = authRepository.currentUserId() ?: return
        val isBookmarked = questionId in _uiState.value.bookmarkedQuestionIds
        _uiState.value = _uiState.value.copy(
            bookmarkedQuestionIds = if (isBookmarked) {
                _uiState.value.bookmarkedQuestionIds - questionId
            } else {
                _uiState.value.bookmarkedQuestionIds + questionId
            }
        )
        viewModelScope.launch {
            if (isBookmarked) {
                revisionRepository.removeFromRevision(userId, questionId)
            } else {
                val result = revisionRepository.addForRevision(userId, questionId, quizId)
                if (result is AppResult.Success) {
                    // Calling toggleRevision again on undo naturally removes it — it re-reads
                    // current bookmark state each call, no separate "undo" method needed.
                    AlertBus.success("Added to revision", actionLabel = "UNDO") { toggleRevision(questionId) }
                }
            }
        }
    }

    suspend fun getPdfBranding(): PdfBranding = pdfBrandingProvider.get()
}
