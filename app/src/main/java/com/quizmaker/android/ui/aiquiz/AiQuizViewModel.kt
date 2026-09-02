package com.quizmaker.android.ui.aiquiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.analytics.AnalyticsLogger
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.AI_PROMPT_TEMPLATES
import com.quizmaker.android.data.model.AiPromptTemplate
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.repository.AiGeneratedQuiz
import com.quizmaker.android.repository.AiQuizRepository
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.ui.dashboard.DashboardStateCache
import com.quizmaker.android.util.TrialStatus
import com.quizmaker.android.util.trialStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TRENDING_TEMPLATE_COUNT = 10

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
    // Review step: AI questions generated so far (not yet saved anywhere — see AiQuizRepository's
    // KDoc), and which of them the user wants to keep.
    val reviewQuestions: List<Question> = emptyList(),
    val selectedReviewIds: Set<String> = emptySet(),
    // The AI's own suggested title for this batch (may be null if it didn't return one) — shown
    // nowhere in this screen, just carried forward to prefill Create Quiz's title field.
    val generatedQuizTitle: String? = null,
    // True while confirmSelection() is writing the selected questions to the question bank.
    val isSaving: Boolean = false,
    val navigateToCreateQuizWith: List<String>? = null,
    val navigateToCreateQuizTitle: String? = null,
    val addQuestionsCompleted: Boolean = false,
    // Random 10-of-50 "trending" prompt starters shown as a carousel — see reshuffleTemplates().
    val trendingTemplates: List<AiPromptTemplate> = emptyList(),
    // Same trial gate as QuestionBankViewModel/QuizListViewModel — AI generation is a create action
    // too, so a trial-expired free account shouldn't be able to use it just because this screen was
    // reached directly (e.g. the bottom-nav AI tab, which skips those screens' own onOpenAiClick gate).
    val isCreationBlocked: Boolean = false,
    val showTrialPaywall: Boolean = false
) {
    val canGenerate: Boolean get() = (prompt.isNotBlank() || attachmentKind != null) && !isGenerating
    val hasReview: Boolean get() = reviewQuestions.isNotEmpty()
}

@HiltViewModel
class AiQuizViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val aiQuizRepository: AiQuizRepository,
    private val analyticsLogger: AnalyticsLogger,
    private val dashboardStateCache: DashboardStateCache,
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

    init {
        loadTrialGate()
    }

    /** Independent of everything else here — a failed/slow trial check shouldn't block the composer from showing. */
    private fun loadTrialGate() {
        viewModelScope.launch {
            val profile = (authRepository.getCurrentProfile() as? AppResult.Success)?.data ?: return@launch
            _uiState.value = _uiState.value.copy(isCreationBlocked = profile.trialStatus() is TrialStatus.Expired)
        }
    }

    fun dismissTrialPaywall() {
        _uiState.value = _uiState.value.copy(showTrialPaywall = false)
    }

    fun onPromptChange(value: String) {
        _uiState.value = _uiState.value.copy(prompt = value, errorMessage = null)
    }

    /** Called once per screen visit (see AiQuizScreen's LaunchedEffect) — a fresh random 10 of the
     *  50 templates each time, including every time the user taps the bottom-nav AI tab back in. */
    fun reshuffleTemplates() {
        _uiState.value = _uiState.value.copy(trendingTemplates = AI_PROMPT_TEMPLATES.shuffled().take(TRENDING_TEMPLATE_COUNT))
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

    /** Discards the current review batch without saving anything — none of it was ever written to
     *  the question bank in the first place (see AiQuizRepository's KDoc), so this is just a
     *  client-side reset. Leaves the prompt text as-is so the user can edit and re-send it. */
    fun clearReview() {
        _uiState.value = _uiState.value.copy(reviewQuestions = emptyList(), selectedReviewIds = emptySet(), errorMessage = null)
    }

    fun generate() {
        val state = _uiState.value
        val prompt = state.prompt.trim()
        if (prompt.isBlank() || state.isGenerating) return

        runGeneration(source = "prompt") { aiQuizRepository.generateQuestionsFromPrompt(prompt, state.questionCount) }
    }

    fun generateFromPdf(pdfBase64: String) {
        val state = _uiState.value
        if (state.isGenerating) return

        runGeneration(source = "pdf") { aiQuizRepository.generateQuestionsFromPdf(state.prompt.trim(), pdfBase64, state.questionCount) }
    }

    fun generateFromImages(images: List<Pair<String, String>>) {
        val state = _uiState.value
        if (state.isGenerating) return

        runGeneration(source = "images") { aiQuizRepository.generateQuestionsFromImages(state.prompt.trim(), images, state.questionCount) }
    }

    private fun runGeneration(source: String, block: suspend () -> AppResult<AiGeneratedQuiz>) {
        if (_uiState.value.isCreationBlocked) {
            _uiState.value = _uiState.value.copy(showTrialPaywall = true)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, errorMessage = null)
            when (val result = block()) {
                is AppResult.Success -> {
                    analyticsLogger.logAiQuizGenerated(source = source, questionCount = result.data.questions.size)
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        reviewQuestions = result.data.questions,
                        selectedReviewIds = result.data.questions.map { it.id }.toSet(),
                        generatedQuizTitle = result.data.quizTitle
                    )
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isGenerating = false, errorMessage = result.message)
            }
        }
    }

    /**
     * The one point in this whole flow that actually writes to the question bank — everything
     * before this (generating, reviewing, checking/unchecking) is purely local. Only the
     * currently-selected questions are saved, using their real database ids from here on: for
     * "Create Quiz" mode those ids are what preselects them on the normal Create Quiz screen; for
     * "Add Questions" mode they're simply now sitting in the bank like any manually-added question.
     */
    fun confirmSelection() {
        val state = _uiState.value
        if (state.selectedReviewIds.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Select at least one question.")
            return
        }
        if (state.isSaving) return
        val userId = authRepository.currentUserId() ?: return
        val selected = state.reviewQuestions.filter { it.id in state.selectedReviewIds }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            when (val result = aiQuizRepository.saveQuestions(userId, selected)) {
                is AppResult.Success -> {
                    // Both modes write new rows into the question bank here (see AiQuizRepository's
                    // saveQuestions KDoc) — Dashboard's totalQuestions changes either way, regardless
                    // of whether this batch is about to become a quiz too.
                    dashboardStateCache.needsRefresh = true
                    _uiState.value = if (isAddQuestionsMode) {
                        _uiState.value.copy(isSaving = false, addQuestionsCompleted = true)
                    } else {
                        _uiState.value.copy(
                            isSaving = false,
                            navigateToCreateQuizWith = result.data.map { it.id },
                            navigateToCreateQuizTitle = state.generatedQuizTitle
                        )
                    }
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
            }
        }
    }

    fun consumeNavigation() {
        _uiState.value = AiQuizUiState()
    }
}
