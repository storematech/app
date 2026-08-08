package com.quizmaker.android.ui.aiquiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.repository.AiQuizRepository
import com.quizmaker.android.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

const val MIN_AI_QUESTION_COUNT = 3
const val MAX_AI_QUESTION_COUNT = 15

enum class AiAttachmentKind { PDF, IMAGES }

data class AiQuizUiState(
    val prompt: String = "",
    val questionCount: Int = 5,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val attachmentKind: AiAttachmentKind? = null,
    val attachmentLabel: String? = null,
    // Review step: AI questions generated so far, and which of them the user wants in the quiz.
    val reviewQuestions: List<Question> = emptyList(),
    val selectedReviewIds: Set<String> = emptySet(),
    val navigateToCreateQuizWith: List<String>? = null,
    val addQuestionsCompleted: Boolean = false
) {
    val canGenerate: Boolean get() = (prompt.isNotBlank() || attachmentKind != null) && !isGenerating
    val hasReview: Boolean get() = reviewQuestions.isNotEmpty()
}

@HiltViewModel
class AiQuizViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val aiQuizRepository: AiQuizRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Where the user tapped "AI" from — drives whether a mode strip shows up top, its label,
    // and what the review step's confirm button does. The bottom-nav AI tab passes no source
    // (ambient default, no strip); Question Bank and Quiz List pass an explicit one.
    private val launchSource: String? = savedStateHandle.get<String>("source")?.takeIf { it.isNotBlank() }

    /** True when launched from the Question Bank's AI button — questions get added to the bank, not turned into a quiz. */
    val isAddQuestionsMode: Boolean = launchSource == "questions"
    val showModeStrip: Boolean = launchSource != null
    val modeStripLabel: String = if (isAddQuestionsMode) "Add Questions" else "Create Quiz"

    private val _uiState = MutableStateFlow(AiQuizUiState())
    val uiState: StateFlow<AiQuizUiState> = _uiState.asStateFlow()

    fun onPromptChange(value: String) {
        _uiState.value = _uiState.value.copy(prompt = value, errorMessage = null)
    }

    fun onQuestionCountChange(value: Int) {
        _uiState.value = _uiState.value.copy(questionCount = value.coerceIn(MIN_AI_QUESTION_COUNT, MAX_AI_QUESTION_COUNT))
    }

    fun setPdfAttachment(fileName: String) {
        _uiState.value = _uiState.value.copy(attachmentKind = AiAttachmentKind.PDF, attachmentLabel = fileName, errorMessage = null)
    }

    fun setImagesAttachment(count: Int) {
        _uiState.value = _uiState.value.copy(
            attachmentKind = AiAttachmentKind.IMAGES,
            attachmentLabel = if (count == 1) "1 photo" else "$count photos",
            errorMessage = null
        )
    }

    fun clearAttachment() {
        _uiState.value = _uiState.value.copy(attachmentKind = null, attachmentLabel = null)
    }

    fun toggleReviewQuestion(questionId: String) {
        val current = _uiState.value.selectedReviewIds
        val updated = if (questionId in current) current - questionId else current + questionId
        _uiState.value = _uiState.value.copy(selectedReviewIds = updated)
    }

    fun selectAllReview() {
        _uiState.value = _uiState.value.copy(selectedReviewIds = _uiState.value.reviewQuestions.map { it.id }.toSet())
    }

    fun deselectAllReview() {
        _uiState.value = _uiState.value.copy(selectedReviewIds = emptySet())
    }

    fun generate() {
        val state = _uiState.value
        val prompt = state.prompt.trim()
        if (prompt.isBlank() || state.isGenerating) return
        val userId = authRepository.currentUserId() ?: return

        runGeneration { aiQuizRepository.generateQuestionsFromPrompt(userId, prompt, state.questionCount) }
    }

    fun generateFromPdf(pdfBase64: String) {
        val state = _uiState.value
        if (state.isGenerating) return
        val userId = authRepository.currentUserId() ?: return

        runGeneration { aiQuizRepository.generateQuestionsFromPdf(userId, state.prompt.trim(), pdfBase64, state.questionCount) }
    }

    fun generateFromImages(images: List<Pair<String, String>>) {
        val state = _uiState.value
        if (state.isGenerating) return
        val userId = authRepository.currentUserId() ?: return

        runGeneration { aiQuizRepository.generateQuestionsFromImages(userId, state.prompt.trim(), images, state.questionCount) }
    }

    private fun runGeneration(block: suspend () -> AppResult<List<Question>>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, errorMessage = null)
            when (val result = block()) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    reviewQuestions = result.data,
                    selectedReviewIds = result.data.map { it.id }.toSet()
                )
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isGenerating = false, errorMessage = result.message)
            }
        }
    }

    fun confirmSelection() {
        val state = _uiState.value
        if (state.selectedReviewIds.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Select at least one question.")
            return
        }
        _uiState.value = if (isAddQuestionsMode) {
            state.copy(addQuestionsCompleted = true)
        } else {
            state.copy(navigateToCreateQuizWith = state.selectedReviewIds.toList())
        }
    }

    fun consumeNavigation() {
        _uiState.value = AiQuizUiState()
    }
}
