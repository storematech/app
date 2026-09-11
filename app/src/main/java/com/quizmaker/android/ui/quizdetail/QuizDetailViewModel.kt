package com.quizmaker.android.ui.quizdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.analytics.AnalyticsLogger
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.repository.ManualMarkingRepository
import com.quizmaker.android.repository.QuizRepository
import com.quizmaker.android.util.PdfBranding
import com.quizmaker.android.util.PdfBrandingProvider
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
    val deleted: Boolean = false,
    /** Whether this quiz has at least one Free Text question that isn't marked "Ungraded" — drives
     *  whether the Manual Marking card shows at all. */
    val hasGradedFreeTextQuestions: Boolean = false,
    val pendingMarkingCount: Int = 0
)

@HiltViewModel
class QuizDetailViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val manualMarkingRepository: ManualMarkingRepository,
    private val analyticsLogger: AnalyticsLogger,
    private val pdfBrandingProvider: PdfBrandingProvider,
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

            // Only worth the extra queries when the quiz actually has a Free Text or Fill in the
            // Blank question that isn't marked "Ungraded" — the common case (no manual marking
            // needed at all) skips straight past this. (Property/param names below still say
            // "FreeText" to keep this diff minimal, but both question types gate the same way now —
            // see ManualMarkingRepository.getMarkingItems, which was widened to match.)
            val questions = (quizRepository.getQuestionsForQuiz(quizId) as? AppResult.Success)?.data.orEmpty()
            val hasGradedFreeText = questions.any {
                (it.type == QuestionType.FREE_TEXT || it.type == QuestionType.FILL_IN_BLANK) && !it.isUngraded
            }
            val pendingMarkingCount = if (hasGradedFreeText) {
                (manualMarkingRepository.getMarkingItems(quizId) as? AppResult.Success)?.data?.count { it.isPending } ?: 0
            } else {
                0
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                quiz = (quizResult as AppResult.Success).data,
                questionCount = questionCount,
                responseCount = responseCount,
                hasGradedFreeTextQuestions = hasGradedFreeText,
                pendingMarkingCount = pendingMarkingCount
            )
        }
    }

    fun closeQuiz() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            when (quizRepository.closeQuiz(quizId)) {
                is AppResult.Success -> analyticsLogger.logQuizClosed()
                is AppResult.Error -> Unit
            }
            _uiState.value = _uiState.value.copy(actionInProgress = false)
            refresh()
        }
    }

    fun deleteQuiz() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            when (quizRepository.deleteQuiz(quizId)) {
                is AppResult.Success -> {
                    analyticsLogger.logQuizDeleted()
                    _uiState.value = _uiState.value.copy(actionInProgress = false, deleted = true)
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(actionInProgress = false)
            }
        }
    }

    suspend fun getPdfBranding(): PdfBranding = pdfBrandingProvider.get()
}
