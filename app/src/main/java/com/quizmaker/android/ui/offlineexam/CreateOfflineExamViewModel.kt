package com.quizmaker.android.ui.offlineexam

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.alert.AlertBus
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionDifficulty
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.repository.AiQuizRepository
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.QuestionRepository
import com.quizmaker.android.repository.QuizRepository
import com.quizmaker.android.ui.aiquiz.MAX_AI_QUESTION_COUNT
import com.quizmaker.android.ui.aiquiz.MIN_AI_QUESTION_COUNT
import com.quizmaker.android.util.TrialStatus
import com.quizmaker.android.util.trialStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Same shape as CreateQuizViewModel's NewQuestionDraft — duplicated rather than shared/extracted,
 *  same convention that file's own KDoc already follows for NewQuestionSheet's copy in
 *  QuestionEditSheet.kt. */
data class NewOfflineExamQuestionDraft(
    val text: String = "",
    val type: QuestionType = QuestionType.SINGLE_CHOICE,
    val options: List<String> = listOf("", "", "", ""),
    val correctOptionIndex: Int = 0,
    val correctOptionIndices: Set<Int> = setOf(0),
    val freeTextAnswer: String = "",
    val points: Double = 1.0,
    val negativePoints: Double = 0.0,
    val difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
    val tags: List<String> = emptyList(),
    val explanation: String? = null,
    val isUngraded: Boolean = false
)

data class CreateOfflineExamUiState(
    val isEditMode: Boolean = false,
    val isLoadingForEdit: Boolean = false,
    val isLoadingQuestionBank: Boolean = true,
    val errorMessage: String? = null,

    val title: String = "",
    val description: String = "",

    // Question bank + selection — same shape as CreateQuizUiState's Step 2.
    val questionBank: List<Question> = emptyList(),
    val selectedQuestionIds: List<String> = emptyList(),
    val questionDraft: NewOfflineExamQuestionDraft? = null,
    val isSavingQuestion: Boolean = false,
    val questionSearchQuery: String = "",

    // "AI" button sheet — topic-based generation, mirrors CreateQuizViewModel's in-wizard AI sheet.
    val showAiQuestionSheet: Boolean = false,
    val aiPrompt: String = "",
    val aiQuestionCount: Int = 5,
    val isGeneratingAiQuestions: Boolean = false,
    val aiQuestionError: String? = null,

    // "Scanner" button sheet — photo-based generation, mirrors AiQuizViewModel's image path.
    val showScannerSheet: Boolean = false,
    val photoQuestionCount: Int = 10,
    val isGeneratingFromPhotos: Boolean = false,
    val photoScanError: String? = null,

    val isDownloading: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    // One-shot: set once the pre-preview save lands, consumed by the Screen to navigate to
    // OfflineExamPaperPreviewScreen (see openPaperPreview/consumePaperPreviewNavigation).
    val paperPreviewQuizId: String? = null,

    // Same trial gate every other AI-generation entry point in the app enforces (see
    // CreateQuizViewModel/QuestionBankViewModel) — without this, Offline Exam's AI/Scanner buttons
    // would be an unintended way around the paywall.
    val isAiCreationBlocked: Boolean = false,
    val showTrialPaywall: Boolean = false
) {
    val filteredQuestionBank: List<Question>
        get() = questionBank.filter { question ->
            questionSearchQuery.isBlank() ||
                question.text.contains(questionSearchQuery, ignoreCase = true) ||
                question.tags.any { it.contains(questionSearchQuery, ignoreCase = true) }
        }
}

/**
 * Backs the single-screen Offline Exam creator/editor. An Offline Exam is a normal `quizzes` row
 * flagged `is_offline_exam = true` (see QuizRepository.createOfflineExam/updateOfflineExam) — this
 * ViewModel only ever collects a title/description/question selection, unlike CreateQuizViewModel's
 * full 4-step wizard (timing/settings/etc. are all fixed defaults baked into offlineExamSpec() on
 * the repository side).
 */
@HiltViewModel
class CreateOfflineExamViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val questionRepository: QuestionRepository,
    private val quizRepository: QuizRepository,
    private val aiQuizRepository: AiQuizRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editQuizId: String? = savedStateHandle.get<String>("editQuizId")?.takeIf { it.isNotBlank() }

    // Tracks which quiz row "Preview Paper" writes to — starts as [editQuizId] (edit mode) or
    // null (create mode), then latches onto the just-created id after the first successful save so
    // tapping Preview again updates that same exam instead of creating a duplicate one each time.
    private var savedQuizId: String? = editQuizId

    private val _uiState = MutableStateFlow(CreateOfflineExamUiState(isEditMode = editQuizId != null))
    val uiState: StateFlow<CreateOfflineExamUiState> = _uiState.asStateFlow()

    init {
        editQuizId?.let { loadQuizForEdit(it) }
        loadQuestionBank()
        loadTrialGate()
    }

    /** Independent of everything else here — a failed/slow trial check shouldn't block the rest
     *  of this screen from working. Mirrors CreateQuizViewModel.loadTrialGate() exactly. */
    private fun loadTrialGate() {
        viewModelScope.launch {
            val profile = (authRepository.getCurrentProfile() as? AppResult.Success)?.data ?: return@launch
            _uiState.value = _uiState.value.copy(isAiCreationBlocked = profile.trialStatus() is TrialStatus.Expired)
        }
    }

    fun dismissTrialPaywall() {
        _uiState.value = _uiState.value.copy(showTrialPaywall = false)
    }

    private fun loadQuizForEdit(quizId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingForEdit = true)
            val quizResult = quizRepository.getQuizById(quizId)
            val questionsResult = quizRepository.getQuestionsForQuiz(quizId)
            when {
                quizResult is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoadingForEdit = false, errorMessage = quizResult.message)
                questionsResult is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoadingForEdit = false, errorMessage = questionsResult.message)
                quizResult is AppResult.Success && questionsResult is AppResult.Success -> {
                    val quiz = quizResult.data
                    _uiState.value = _uiState.value.copy(
                        isLoadingForEdit = false,
                        title = quiz.title,
                        description = quiz.description,
                        selectedQuestionIds = questionsResult.data.map { it.id }
                    )
                }
            }
        }
    }

    private fun loadQuestionBank() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingQuestionBank = true)
            when (val result = questionRepository.getQuestionsForUser(userId)) {
                is AppResult.Success -> _uiState.value =
                    _uiState.value.copy(isLoadingQuestionBank = false, questionBank = result.data)
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoadingQuestionBank = false, errorMessage = result.message)
            }
        }
    }

    // ---- Details ----

    fun onTitleChange(value: String) {
        val capitalized = value.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        _uiState.value = _uiState.value.copy(title = capitalized, errorMessage = null)
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value)
    }

    // ---- Question selection ----

    fun onQuestionSearchChange(query: String) {
        _uiState.value = _uiState.value.copy(questionSearchQuery = query)
    }

    fun toggleQuestionSelected(questionId: String) {
        val current = _uiState.value.selectedQuestionIds
        val updated = if (questionId in current) current - questionId else current + questionId
        _uiState.value = _uiState.value.copy(selectedQuestionIds = updated)
    }

    fun startNewQuestionDraft() {
        _uiState.value = _uiState.value.copy(questionDraft = NewOfflineExamQuestionDraft())
    }

    fun cancelNewQuestionDraft() {
        _uiState.value = _uiState.value.copy(questionDraft = null)
    }

    fun updateDraft(transform: (NewOfflineExamQuestionDraft) -> NewOfflineExamQuestionDraft) {
        _uiState.value.questionDraft?.let {
            _uiState.value = _uiState.value.copy(questionDraft = transform(it))
        }
    }

    fun saveNewQuestion() {
        val draft = _uiState.value.questionDraft ?: return
        val userId = authRepository.currentUserId() ?: return
        if (draft.text.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter the question text.")
            return
        }

        val optionPairs: List<Pair<String, Boolean>> = when (draft.type) {
            QuestionType.SINGLE_CHOICE -> draft.options.mapIndexed { index, text -> text to (index == draft.correctOptionIndex) }
            QuestionType.MULTI_CHOICE -> draft.options.mapIndexed { index, text -> text to (index in draft.correctOptionIndices) }
            else -> emptyList()
        }
        if (draft.type == QuestionType.SINGLE_CHOICE || draft.type == QuestionType.MULTI_CHOICE) {
            if (draft.options.any { it.isBlank() } || draft.options.size < 2) {
                _uiState.value = _uiState.value.copy(errorMessage = "Fill in at least 2 answer options.")
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingQuestion = true, errorMessage = null)
            val result = questionRepository.createQuestion(
                userId = userId,
                text = draft.text.trim(),
                type = draft.type,
                points = draft.points,
                negativePoints = draft.negativePoints,
                difficulty = draft.difficulty,
                explanation = draft.explanation,
                tags = draft.tags,
                options = optionPairs,
                freeTextAnswer = draft.freeTextAnswer.ifBlank { null },
                imageUrl = null,
                isUngraded = draft.isUngraded
            )
            when (result) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    isSavingQuestion = false,
                    questionDraft = null,
                    questionBank = listOf(result.data) + _uiState.value.questionBank,
                    selectedQuestionIds = _uiState.value.selectedQuestionIds + result.data.id
                )
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isSavingQuestion = false, errorMessage = result.message)
            }
        }
    }

    // ---- "AI" button: topic-based generation (mirrors CreateQuizViewModel.generateAiQuestions) ----

    fun openAiQuestionSheet() {
        if (_uiState.value.isAiCreationBlocked) {
            _uiState.value = _uiState.value.copy(showTrialPaywall = true)
            return
        }
        _uiState.value = _uiState.value.copy(
            showAiQuestionSheet = true,
            aiPrompt = _uiState.value.title,
            aiQuestionError = null
        )
    }

    fun dismissAiQuestionSheet() {
        if (_uiState.value.isGeneratingAiQuestions) return
        _uiState.value = _uiState.value.copy(showAiQuestionSheet = false)
    }

    fun onAiPromptChange(value: String) {
        _uiState.value = _uiState.value.copy(aiPrompt = value, aiQuestionError = null)
    }

    fun onAiQuestionCountChange(value: Int) {
        _uiState.value = _uiState.value.copy(aiQuestionCount = value.coerceIn(MIN_AI_QUESTION_COUNT, MAX_AI_QUESTION_COUNT))
    }

    fun generateAiQuestions() {
        val state = _uiState.value
        val prompt = state.aiPrompt.trim()
        if (prompt.isBlank() || state.isGeneratingAiQuestions) return
        val userId = authRepository.currentUserId() ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingAiQuestions = true, aiQuestionError = null)
            when (val result = aiQuizRepository.generateQuestionsFromPrompt(prompt, state.aiQuestionCount)) {
                is AppResult.Success -> {
                    when (val saved = aiQuizRepository.saveQuestions(userId, result.data.questions)) {
                        is AppResult.Success -> _uiState.value = _uiState.value.copy(
                            isGeneratingAiQuestions = false,
                            showAiQuestionSheet = false,
                            questionBank = saved.data + _uiState.value.questionBank,
                            selectedQuestionIds = _uiState.value.selectedQuestionIds + saved.data.map { it.id }
                        )
                        is AppResult.Error -> _uiState.value =
                            _uiState.value.copy(isGeneratingAiQuestions = false, aiQuestionError = saved.message)
                    }
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isGeneratingAiQuestions = false, aiQuestionError = result.message)
            }
        }
    }

    // ---- "Scanner" button: photo-based generation (mirrors AiQuizViewModel.generateFromImages) ----

    fun openScannerSheet() {
        if (_uiState.value.isAiCreationBlocked) {
            _uiState.value = _uiState.value.copy(showTrialPaywall = true)
            return
        }
        _uiState.value = _uiState.value.copy(showScannerSheet = true, photoScanError = null)
    }

    fun dismissScannerSheet() {
        if (_uiState.value.isGeneratingFromPhotos) return
        _uiState.value = _uiState.value.copy(showScannerSheet = false)
    }

    fun onPhotoQuestionCountChange(value: Int) {
        _uiState.value = _uiState.value.copy(photoQuestionCount = value.coerceIn(MIN_AI_QUESTION_COUNT, MAX_AI_QUESTION_COUNT))
    }

    /** [images] are (base64, mimeType) pairs, same shape AiQuizViewModel.generateFromImages takes —
     *  the screen does the actual photo capture/compression (AiAttachmentUtils), this just relays
     *  the encoded result to the same generate-then-save pipeline as the topic-based AI sheet. */
    fun generateFromPhotos(images: List<Pair<String, String>>) {
        val state = _uiState.value
        if (images.isEmpty() || state.isGeneratingFromPhotos) return
        val userId = authRepository.currentUserId() ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingFromPhotos = true, photoScanError = null)
            when (val result = aiQuizRepository.generateQuestionsFromImages("", images, state.photoQuestionCount)) {
                is AppResult.Success -> {
                    when (val saved = aiQuizRepository.saveQuestions(userId, result.data.questions)) {
                        is AppResult.Success -> _uiState.value = _uiState.value.copy(
                            isGeneratingFromPhotos = false,
                            showScannerSheet = false,
                            questionBank = saved.data + _uiState.value.questionBank,
                            selectedQuestionIds = _uiState.value.selectedQuestionIds + saved.data.map { it.id }
                        )
                        is AppResult.Error -> _uiState.value =
                            _uiState.value.copy(isGeneratingFromPhotos = false, photoScanError = saved.message)
                    }
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isGeneratingFromPhotos = false, photoScanError = result.message)
            }
        }
    }

    // ---- Create Exam / Preview Paper ----
    // The bottom button is "Create Exam" until the exam has been saved once, then switches to
    // "Preview Paper" (see CreateOfflineExamScreen — driven by uiState.isEditMode). The first tap
    // only saves the row so the teacher can keep adding/reviewing questions before ever seeing the
    // paper preview; only once it exists does tapping the (now-relabeled) button open
    // OfflineExamPaperPreviewScreen, which is where the PDF actually gets exported.

    /** First tap ("Create Exam"): saves the row only — no paper preview yet. */
    fun createExam() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Enter a title for this offline exam.")
            return
        }
        if (state.isDownloading || savedQuizId != null) return
        val userId = authRepository.currentUserId()
        if (userId == null) {
            _uiState.value = state.copy(errorMessage = "You're not signed in.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true, errorMessage = null)
            when (val result = quizRepository.createOfflineExam(userId, state.title.trim(), state.description.trim().ifBlank { null }, state.selectedQuestionIds)) {
                is AppResult.Success -> {
                    savedQuizId = result.data.id
                    _uiState.value = _uiState.value.copy(isDownloading = false, isEditMode = true)
                    AlertBus.success("Offline exam created")
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isDownloading = false, errorMessage = result.message)
            }
        }
    }

    /**
     * "Preview Paper" tap: re-saves whatever changed since creation, then signals
     * [CreateOfflineExamUiState.paperPreviewQuizId] once that save lands — the Screen observes it
     * to navigate to OfflineExamPaperPreviewScreen, which does the actual letterhead-edit/download
     * flow this ViewModel used to do directly (see git history for the old downloadPaper()).
     */
    fun openPaperPreview() {
        val state = _uiState.value
        val currentId = savedQuizId ?: return
        if (state.title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Enter a title for this offline exam.")
            return
        }
        if (state.isDownloading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true, errorMessage = null)
            val title = state.title.trim()
            val description = state.description.trim().ifBlank { null }

            when (val saveResult = quizRepository.updateOfflineExam(currentId, title, description, state.selectedQuestionIds)) {
                is AppResult.Success -> _uiState.value =
                    _uiState.value.copy(isDownloading = false, paperPreviewQuizId = saveResult.data.id)
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isDownloading = false, errorMessage = saveResult.message)
            }
        }
    }

    /** Clears the one-shot navigation signal once the Screen has consumed it — same pattern as
     *  [CreateOfflineExamUiState.isDeleted] driving onNavigateBack. */
    fun consumePaperPreviewNavigation() {
        _uiState.value = _uiState.value.copy(paperPreviewQuizId = null)
    }

    /** Same `quizzes` row delete as OfflineExamListViewModel.deleteExam/QuizListViewModel.deleteQuiz
     *  — only available once the exam has actually been saved (see the TopAppBar's three-dot menu,
     *  shown only in edit mode). The screen navigates back once [CreateOfflineExamUiState.isDeleted]
     *  flips, rather than this ViewModel holding a navigation callback. */
    fun deleteExam() {
        val quizId = savedQuizId ?: return
        if (_uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, errorMessage = null)
            when (val result = quizRepository.deleteQuiz(quizId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isDeleting = false, isDeleted = true)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isDeleting = false, errorMessage = result.message)
            }
        }
    }
}
