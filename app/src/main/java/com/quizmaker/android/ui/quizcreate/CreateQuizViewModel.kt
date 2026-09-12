package com.quizmaker.android.ui.quizcreate

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.alert.AlertBus
import com.quizmaker.android.core.analytics.AnalyticsLogger
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Group
import com.quizmaker.android.data.model.NewQuizSpec
import com.quizmaker.android.data.model.QUIZ_NAME_SUGGESTIONS
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionDifficulty
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.data.model.QuizNameSuggestion
import com.quizmaker.android.repository.AiQuizRepository
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.LearnersRepository
import com.quizmaker.android.repository.QuestionRepository
import com.quizmaker.android.repository.QuizRepository
import com.quizmaker.android.ui.aiquiz.MAX_AI_QUESTION_COUNT
import com.quizmaker.android.ui.aiquiz.MIN_AI_QUESTION_COUNT
import com.quizmaker.android.ui.dashboard.DashboardStateCache
import com.quizmaker.android.util.TrialStatus
import com.quizmaker.android.util.trialStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

private const val QUIZ_NAME_SUGGESTION_COUNT = 5

/** The available quiz accent color presets — a custom color can also be picked separately (see
 *  CreateQuizScreen's color picker dialog), so [quizColor] isn't restricted to just these five. */
val QUIZ_COLOR_SWATCHES = listOf("#8b5cf6", "#3b82f6", "#10b981", "#f59e0b", "#ef4444")

/** Longest an "overall" quiz timer can run — 3.5 hours, in minutes. */
const val MAX_TIME_LIMIT_MINUTES = 210

enum class CreateQuizStep { DETAILS, QUESTIONS, SETTINGS, REVIEW }

data class NewQuestionDraft(
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
    /** Excluded from scoring entirely — see the "Ungraded" checkbox next to Points. A Free Text
     *  question left unchecked here needs manual marking after each submission (see
     *  ManualMarkingScreen), since free text can't be auto-graded. */
    val isUngraded: Boolean = false
)

data class CreateQuizUiState(
    val step: CreateQuizStep = CreateQuizStep.DETAILS,
    val isLoadingQuestionBank: Boolean = true,
    val isEditMode: Boolean = false,
    val isLoadingForEdit: Boolean = false,
    val errorMessage: String? = null,

    // Step 1: details
    val title: String = "",
    val description: String = "",
    val timeLimitType: String = "overall",
    val timeLimitMinutes: Int = 10,
    val timePerQuestionSeconds: Int = 30,
    val shuffleQuestions: Boolean = false,
    /** 'none' | 'uniform' | 'per_question' — see quiz_negative_marking_mode.sql. */
    val negativeMarkingMode: String = "none",
    /** Only meaningful in 'uniform' mode — the deduction pre-filled into every question added
     *  to this quiz while it's selected. */
    val negativeMarkingValue: Double = 1.0,
    // Random 5-of-50 tap-to-fill title/description starters shown on Details for a brand-new
    // (non-edit) quiz — see reshuffleQuizNameSuggestions().
    val quizNameSuggestions: List<QuizNameSuggestion> = emptyList(),

    // Step 2: questions
    val questionBank: List<Question> = emptyList(),
    val selectedQuestionIds: List<String> = emptyList(),
    // Fixed at arrival (e.g. "Create A Re-Test" from Revision, or the AI quiz flow) — never
    // touched afterward, so later manually checking/unchecking other questions never reshuffles
    // the list out from under the user. See filteredQuestionBank.
    val pinnedQuestionIds: Set<String> = emptySet(),
    val questionDraft: NewQuestionDraft? = null,
    val isSavingQuestion: Boolean = false,
    val questionSearchQuery: String = "",
    val questionTagFilter: String? = null,
    val questionDifficultyFilter: QuestionDifficulty? = null,
    // In-wizard "AI" button on the Questions step — generates straight into questionBank/
    // selectedQuestionIds below, no separate review step and no leaving this screen.
    val showAiQuestionSheet: Boolean = false,
    val aiPrompt: String = "",
    val aiQuestionCount: Int = 5,
    val isGeneratingAiQuestions: Boolean = false,
    val aiQuestionError: String? = null,
    // Same trial gate every other create action in the app enforces (see QuestionBankViewModel/
    // AiQuizViewModel) — without this, the in-wizard AI button would be an unintended bypass of
    // the paywall those other entry points already enforce.
    val isAiCreationBlocked: Boolean = false,
    val showTrialPaywall: Boolean = false,

    // Step 3: settings
    val showResults: Boolean = true,
    val sendResultEmail: Boolean = true,
    val allowResultPdf: Boolean = true,
    val showLeaderboard: Boolean = false,
    val issueCertificate: Boolean = false,
    val certificatePassScore: Int = 70,
    val showContactDetails: Boolean = false,
    val instructions: String = "",
    val quizColor: String = QUIZ_COLOR_SWATCHES.first(),
    val collectEmail: Boolean = true,
    val collectAddress: Boolean = false,
    val collectPhone: Boolean = false,
    val requireOtpVerification: Boolean = false,
    val allowMultipleAttempts: Boolean = false,
    /** Optional attempt window — null means no restriction (today's only behavior). */
    val startsAt: Instant? = null,
    val endsAt: Instant? = null,
    /** 'public' | 'all_learners' | 'group' — see NewQuizSpec's own doc comment. */
    val visibilityType: String = "public",
    val selectedGroupId: String? = null,
    val groups: List<Group> = emptyList(),
    val isLoadingGroups: Boolean = false,

    // Step 4: review/create
    val isSubmitting: Boolean = false,
    val resultQuiz: Quiz? = null
) {
    val selectedQuestions: List<Question> get() = questionBank.filter { it.id in selectedQuestionIds }
    val canGoNextFromDetails: Boolean get() = title.isNotBlank()
    val canGoNextFromQuestions: Boolean get() = selectedQuestionIds.isNotEmpty()

    /** Shown next to the schedule fields when the picked window is backwards — doesn't block
     *  typing, only submit (mirrors DashboardDateRangeSheet's from<=to gating). */
    val scheduleError: String?
        get() = if (startsAt != null && endsAt != null && endsAt <= startsAt) {
            "Closing time must be after the opening time."
        } else null

    val availableTags: List<String> get() = questionBank.flatMap { it.tags }.distinct().sorted()

    val filteredQuestionBank: List<Question>
        get() = questionBank.filter { question ->
            val matchesSearch = questionSearchQuery.isBlank() ||
                question.text.contains(questionSearchQuery, ignoreCase = true) ||
                question.tags.any { it.contains(questionSearchQuery, ignoreCase = true) }
            val matchesTag = questionTagFilter == null || questionTagFilter in question.tags
            val matchesDifficulty = questionDifficultyFilter == null || question.difficulty == questionDifficultyFilter
            matchesSearch && matchesTag && matchesDifficulty
        }.sortedByDescending { it.id in pinnedQuestionIds } // stable sort — pinned questions float to the top, everything else keeps its order
}

@HiltViewModel
class CreateQuizViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val questionRepository: QuestionRepository,
    private val quizRepository: QuizRepository,
    private val learnersRepository: LearnersRepository,
    private val aiQuizRepository: AiQuizRepository,
    private val analyticsLogger: AnalyticsLogger,
    private val dashboardStateCache: DashboardStateCache,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editQuizId: String? = savedStateHandle["quizId"]

    // Arriving from the AI quiz flow (or a re-test/manual selection): these questions come pre-checked.
    private val preselectedQuestionIds: List<String> =
        savedStateHandle.get<String>("preselectedIds").orEmpty().split(",").filter { it.isNotBlank() }

    /** Lets Details offer a "Publish Now" shortcut straight past Questions/Settings — every field
     *  those two steps would otherwise ask for already has a sensible default (see
     *  CreateQuizUiState's defaults), and Questions is already satisfied by [preselectedQuestionIds]
     *  — so once a title exists, [submit] can legitimately fire from Details already. Only for a
     *  brand-new quiz (never edit mode) that actually arrived with preselected questions, e.g. the
     *  AI quiz flow's "1 message = 1 quiz" promise, or Revision's "Create A Re-Test". */
    val isQuickPublishAvailable: Boolean = editQuizId == null && preselectedQuestionIds.isNotEmpty()

    // Also from the AI quiz flow — the AI's own suggested title, percent-decoded (see
    // Screen.CreateQuiz.createRoute for why it's encoded). Empty when arriving any other way,
    // which is harmless: it just leaves the title field blank like today, and loadQuizForEdit()
    // below overwrites it anyway when editing an existing quiz.
    private val prefilledTitle: String = Uri.decode(savedStateHandle.get<String>("prefilledTitle").orEmpty())

    private val _uiState = MutableStateFlow(
        CreateQuizUiState(
            isEditMode = editQuizId != null,
            title = prefilledTitle,
            selectedQuestionIds = preselectedQuestionIds,
            pinnedQuestionIds = preselectedQuestionIds.toSet(),
            quizNameSuggestions = QUIZ_NAME_SUGGESTIONS.shuffled().take(QUIZ_NAME_SUGGESTION_COUNT)
        )
    )
    val uiState: StateFlow<CreateQuizUiState> = _uiState.asStateFlow()

    init {
        val quizId = editQuizId
        if (quizId != null) {
            loadQuizForEdit(quizId)
        }
        loadQuestionBank()
        loadTrialGate()
        loadGroups()
    }

    /** Populates the "Specific group" dropdown in the Settings step's audience picker — loaded
     *  eagerly like the question bank rather than only when that radio option is picked, so the
     *  dropdown has no separate loading flicker the first time it's opened. */
    private fun loadGroups() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingGroups = true)
            when (val result = learnersRepository.getGroups(userId)) {
                is AppResult.Success -> _uiState.value =
                    _uiState.value.copy(isLoadingGroups = false, groups = result.data)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoadingGroups = false)
            }
        }
    }

    /** A fresh random 5-of-50 every time this is called — the Details step's own shuffle button,
     *  independent of the one-shuffle-per-visit set already seeded into the initial state above. */
    fun reshuffleQuizNameSuggestions() {
        _uiState.value = _uiState.value.copy(quizNameSuggestions = QUIZ_NAME_SUGGESTIONS.shuffled().take(QUIZ_NAME_SUGGESTION_COUNT))
    }

    /** Tapping a suggestion chip fills both fields at once — capitalization already matches
     *  onTitleChange's own rule since these are all written pre-capitalized. */
    fun applyQuizNameSuggestion(suggestion: QuizNameSuggestion) {
        _uiState.value = _uiState.value.copy(title = suggestion.title, description = suggestion.description)
    }

    /** Independent of everything else here — a failed/slow trial check shouldn't block the rest
     *  of the wizard from working. */
    private fun loadTrialGate() {
        viewModelScope.launch {
            val profile = (authRepository.getCurrentProfile() as? AppResult.Success)?.data ?: return@launch
            _uiState.value = _uiState.value.copy(isAiCreationBlocked = profile.trialStatus() is TrialStatus.Expired)
        }
    }

    fun dismissTrialPaywall() {
        _uiState.value = _uiState.value.copy(showTrialPaywall = false)
    }

    // ---- In-wizard AI question generation (Questions step's "AI" button) ----

    fun openAiQuestionSheet() {
        if (_uiState.value.isAiCreationBlocked) {
            _uiState.value = _uiState.value.copy(showTrialPaywall = true)
            return
        }
        _uiState.value = _uiState.value.copy(
            showAiQuestionSheet = true,
            // Defaults to the quiz's own title as the topic — the common case is "generate
            // questions about whatever this quiz is already called" — but still fully editable.
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

    /** Generates straight into this wizard's own questionBank/selectedQuestionIds — no separate
     *  review step and no leaving this screen, unlike the standalone AI tab: the new questions
     *  just appear, already selected, in the same list the user was already looking at. */
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
                        is AppResult.Success -> {
                            dashboardStateCache.needsRefresh = true
                            analyticsLogger.logAiQuizGenerated(source = "quiz_wizard", questionCount = saved.data.size)
                            _uiState.value = _uiState.value.copy(
                                isGeneratingAiQuestions = false,
                                showAiQuestionSheet = false,
                                questionBank = saved.data + _uiState.value.questionBank,
                                selectedQuestionIds = _uiState.value.selectedQuestionIds + saved.data.map { it.id }
                            )
                        }
                        is AppResult.Error -> _uiState.value =
                            _uiState.value.copy(isGeneratingAiQuestions = false, aiQuestionError = saved.message)
                    }
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isGeneratingAiQuestions = false, aiQuestionError = result.message)
            }
        }
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
                        timeLimitType = quiz.timeLimitType,
                        timeLimitMinutes = quiz.timeLimit ?: _uiState.value.timeLimitMinutes,
                        timePerQuestionSeconds = quiz.timePerQuestion ?: _uiState.value.timePerQuestionSeconds,
                        shuffleQuestions = quiz.shuffleQuestions,
                        negativeMarkingMode = quiz.negativeMarkingMode,
                        negativeMarkingValue = quiz.negativeMarkingValue,
                        selectedQuestionIds = questionsResult.data.map { it.id },
                        showResults = quiz.showResults,
                        sendResultEmail = quiz.sendResultEmail,
                        allowResultPdf = quiz.allowResultPdf,
                        showLeaderboard = quiz.showLeaderboard,
                        issueCertificate = quiz.issueCertificate,
                        certificatePassScore = quiz.certificatePassScore ?: _uiState.value.certificatePassScore,
                        showContactDetails = quiz.showContactDetails,
                        instructions = quiz.instructions.orEmpty(),
                        quizColor = quiz.quizColor,
                        collectEmail = quiz.collectEmail,
                        collectAddress = quiz.collectAddress,
                        collectPhone = quiz.collectPhone,
                        requireOtpVerification = quiz.requireOtpVerification,
                        allowMultipleAttempts = quiz.allowMultipleAttempts,
                        startsAt = quiz.startsAt,
                        endsAt = quiz.endsAt,
                        visibilityType = quiz.visibilityType,
                        selectedGroupId = quiz.assignedGroupId
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

    // ---- Step navigation ----
    fun goToStep(step: CreateQuizStep) {
        _uiState.value = _uiState.value.copy(step = step, errorMessage = null)
    }

    fun goNext() {
        val next = when (_uiState.value.step) {
            CreateQuizStep.DETAILS -> CreateQuizStep.QUESTIONS
            CreateQuizStep.QUESTIONS -> CreateQuizStep.SETTINGS
            CreateQuizStep.SETTINGS -> CreateQuizStep.REVIEW
            CreateQuizStep.REVIEW -> CreateQuizStep.REVIEW
        }
        goToStep(next)
    }

    fun goBack() {
        val previous = when (_uiState.value.step) {
            CreateQuizStep.DETAILS -> CreateQuizStep.DETAILS
            CreateQuizStep.QUESTIONS -> CreateQuizStep.DETAILS
            CreateQuizStep.SETTINGS -> CreateQuizStep.QUESTIONS
            CreateQuizStep.REVIEW -> CreateQuizStep.SETTINGS
        }
        goToStep(previous)
    }

    // ---- Step 1 ----
    /** Auto-capitalizes the first letter as the user types, so the title always starts capitalized
     *  without them having to remember to do it themselves. */
    fun onTitleChange(value: String) {
        val capitalized = value.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        _uiState.value = _uiState.value.copy(title = capitalized)
    }
    fun onDescriptionChange(value: String) { _uiState.value = _uiState.value.copy(description = value) }
    fun onTimeLimitTypeChange(value: String) { _uiState.value = _uiState.value.copy(timeLimitType = value) }
    fun onTimeLimitMinutesChange(value: Int) {
        _uiState.value = _uiState.value.copy(timeLimitMinutes = value.coerceIn(1, MAX_TIME_LIMIT_MINUTES))
    }
    fun onTimePerQuestionChange(value: Int) { _uiState.value = _uiState.value.copy(timePerQuestionSeconds = value) }
    fun onShuffleChange(value: Boolean) { _uiState.value = _uiState.value.copy(shuffleQuestions = value) }

    /** Toggling this off always clears back to 'none'; toggling on defaults to 'uniform' (the
     *  simpler of the two systems) rather than leaving the mode ambiguous. */
    fun onNegativeMarkingEnabledChange(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(negativeMarkingMode = if (enabled) "uniform" else "none")
    }

    fun onNegativeMarkingModeChange(mode: String) {
        _uiState.value = _uiState.value.copy(negativeMarkingMode = mode)
    }

    fun onNegativeMarkingValueChange(value: Double) {
        _uiState.value = _uiState.value.copy(negativeMarkingValue = value)
    }

    // ---- Step 2 ----
    fun onQuestionSearchChange(query: String) {
        _uiState.value = _uiState.value.copy(questionSearchQuery = query)
    }

    fun onQuestionTagFilterChange(tag: String?) {
        _uiState.value = _uiState.value.copy(questionTagFilter = tag)
    }

    fun onQuestionDifficultyFilterChange(difficulty: QuestionDifficulty?) {
        _uiState.value = _uiState.value.copy(questionDifficultyFilter = difficulty)
    }

    fun toggleQuestionSelected(questionId: String) {
        val current = _uiState.value.selectedQuestionIds
        val updated = if (questionId in current) current - questionId else current + questionId
        _uiState.value = _uiState.value.copy(selectedQuestionIds = updated)
    }

    fun startNewQuestionDraft() {
        val state = _uiState.value
        // Uniform mode: every question added to this quiz starts pre-marked with the quiz's
        // chosen deduction (still editable per question). Per-question/none mode: same blank
        // starting point as today — see NewQuestionSheet's reminder banner for per_question.
        val negativePoints = if (state.negativeMarkingMode == "uniform") state.negativeMarkingValue else 0.0
        _uiState.value = state.copy(questionDraft = NewQuestionDraft(negativePoints = negativePoints))
    }

    fun cancelNewQuestionDraft() {
        _uiState.value = _uiState.value.copy(questionDraft = null)
    }

    fun updateDraft(transform: (NewQuestionDraft) -> NewQuestionDraft) {
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
                is AppResult.Success -> {
                    dashboardStateCache.needsRefresh = true
                    _uiState.value = _uiState.value.copy(
                        isSavingQuestion = false,
                        questionDraft = null,
                        questionBank = listOf(result.data) + _uiState.value.questionBank,
                        selectedQuestionIds = _uiState.value.selectedQuestionIds + result.data.id
                    )
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isSavingQuestion = false, errorMessage = result.message)
            }
        }
    }

    // ---- Step 3 ----
    fun onShowResultsChange(value: Boolean) { _uiState.value = _uiState.value.copy(showResults = value) }
    fun onSendResultEmailChange(value: Boolean) { _uiState.value = _uiState.value.copy(sendResultEmail = value) }
    fun onAllowResultPdfChange(value: Boolean) { _uiState.value = _uiState.value.copy(allowResultPdf = value) }
    fun onShowLeaderboardChange(value: Boolean) { _uiState.value = _uiState.value.copy(showLeaderboard = value) }
    fun onIssueCertificateChange(value: Boolean) { _uiState.value = _uiState.value.copy(issueCertificate = value) }
    fun onCertificatePassScoreChange(value: Int) { _uiState.value = _uiState.value.copy(certificatePassScore = value) }
    fun onShowContactDetailsChange(value: Boolean) { _uiState.value = _uiState.value.copy(showContactDetails = value) }
    fun onInstructionsChange(value: String) { _uiState.value = _uiState.value.copy(instructions = value) }
    fun onQuizColorChange(value: String) { _uiState.value = _uiState.value.copy(quizColor = value) }
    fun onCollectEmailChange(value: Boolean) { _uiState.value = _uiState.value.copy(collectEmail = value) }
    fun onCollectAddressChange(value: Boolean) { _uiState.value = _uiState.value.copy(collectAddress = value) }
    fun onCollectPhoneChange(value: Boolean) { _uiState.value = _uiState.value.copy(collectPhone = value) }
    fun onRequireOtpChange(value: Boolean) { _uiState.value = _uiState.value.copy(requireOtpVerification = value) }
    fun onAllowMultipleAttemptsChange(value: Boolean) { _uiState.value = _uiState.value.copy(allowMultipleAttempts = value) }
    fun onStartsAtChange(value: Instant?) { _uiState.value = _uiState.value.copy(startsAt = value) }
    fun onEndsAtChange(value: Instant?) { _uiState.value = _uiState.value.copy(endsAt = value) }

    /** Switching away from "group" clears the picked group so a stale selection can't silently
     *  ship if the teacher flips back and forth without re-picking. */
    fun onVisibilityTypeChange(value: String) {
        _uiState.value = _uiState.value.copy(
            visibilityType = value,
            selectedGroupId = if (value == "group") _uiState.value.selectedGroupId else null
        )
    }

    fun onSelectedGroupIdChange(value: String) { _uiState.value = _uiState.value.copy(selectedGroupId = value) }

    // ---- Step 4 ----
    fun submit() {
        val state = _uiState.value
        if (state.scheduleError != null) {
            _uiState.value = state.copy(errorMessage = state.scheduleError)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            val spec = NewQuizSpec(
                title = state.title.trim(),
                description = state.description.trim().ifBlank { null },
                timeLimit = if (state.timeLimitType == "overall") state.timeLimitMinutes else null,
                timeLimitType = state.timeLimitType,
                timePerQuestion = if (state.timeLimitType == "per_question") state.timePerQuestionSeconds else null,
                shuffleQuestions = state.shuffleQuestions,
                showResults = state.showResults,
                sendResultEmail = state.sendResultEmail,
                allowResultPdf = state.allowResultPdf,
                showLeaderboard = state.showLeaderboard,
                issueCertificate = state.issueCertificate,
                certificatePassScore = if (state.issueCertificate) state.certificatePassScore else null,
                showContactDetails = state.showContactDetails,
                instructions = state.instructions.trim().ifBlank { null },
                quizColor = state.quizColor,
                collectEmail = state.collectEmail,
                collectAddress = state.collectAddress,
                collectPhone = state.collectPhone,
                requireOtpVerification = state.requireOtpVerification,
                allowMultipleAttempts = state.allowMultipleAttempts,
                negativeMarkingMode = state.negativeMarkingMode,
                negativeMarkingValue = state.negativeMarkingValue,
                startsAt = state.startsAt,
                endsAt = state.endsAt,
                visibilityType = state.visibilityType,
                assignedGroupId = if (state.visibilityType == "group") state.selectedGroupId else null
            )

            val result = if (editQuizId != null) {
                quizRepository.updateQuiz(editQuizId, spec, state.selectedQuestionIds)
            } else {
                val userId = authRepository.currentUserId()
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, errorMessage = "You're not signed in.")
                    return@launch
                }
                quizRepository.createQuiz(userId, spec, state.selectedQuestionIds)
            }

            when (result) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, resultQuiz = result.data)
                    dashboardStateCache.needsRefresh = true
                    if (editQuizId == null) {
                        analyticsLogger.logQuizCreated(
                            quizId = result.data.id,
                            questionCount = state.selectedQuestionIds.size,
                            source = if (preselectedQuestionIds.isNotEmpty()) "ai" else "manual"
                        )
                    }
                    AlertBus.success(if (editQuizId != null) "Quiz updated" else "Quiz created")
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isSubmitting = false, errorMessage = result.message)
            }
        }
    }
}
