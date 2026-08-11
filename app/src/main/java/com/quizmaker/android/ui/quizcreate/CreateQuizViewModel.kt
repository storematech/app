package com.quizmaker.android.ui.quizcreate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.alert.AlertBus
import com.quizmaker.android.core.analytics.AnalyticsLogger
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.NewQuizSpec
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionDifficulty
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.QuestionRepository
import com.quizmaker.android.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The available quiz accent colors — same swatches CreateQuiz.tsx offers on the web. */
val QUIZ_COLOR_SWATCHES = listOf("#8b5cf6", "#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#ec4899")

enum class CreateQuizStep { DETAILS, QUESTIONS, SETTINGS, REVIEW }

data class NewQuestionDraft(
    val text: String = "",
    val type: QuestionType = QuestionType.SINGLE_CHOICE,
    val options: List<String> = listOf("", "", "", ""),
    val correctOptionIndex: Int = 0,
    val correctOptionIndices: Set<Int> = setOf(0),
    val freeTextAnswer: String = "",
    val points: Int = 1,
    val difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
    val tags: List<String> = emptyList(),
    val explanation: String? = null
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
    val timeLimitMinutes: Int = 5,
    val timePerQuestionSeconds: Int = 30,
    val shuffleQuestions: Boolean = false,

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

    // Step 3: settings
    val showResults: Boolean = true,
    val sendResultEmail: Boolean = true,
    val allowResultPdf: Boolean = true,
    val showLeaderboard: Boolean = false,
    val showContactDetails: Boolean = false,
    val instructions: String = "",
    val quizColor: String = QUIZ_COLOR_SWATCHES.first(),
    val collectEmail: Boolean = true,
    val collectAddress: Boolean = false,
    val collectPhone: Boolean = false,
    val requireOtpVerification: Boolean = false,
    val allowMultipleAttempts: Boolean = true,

    // Step 4: review/create
    val isSubmitting: Boolean = false,
    val resultQuiz: Quiz? = null
) {
    val selectedQuestions: List<Question> get() = questionBank.filter { it.id in selectedQuestionIds }
    val canGoNextFromDetails: Boolean get() = title.isNotBlank()
    val canGoNextFromQuestions: Boolean get() = selectedQuestionIds.isNotEmpty()

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
    private val analyticsLogger: AnalyticsLogger,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editQuizId: String? = savedStateHandle["quizId"]

    // Arriving from the AI quiz flow: these questions (already saved to the bank) come pre-checked.
    private val preselectedQuestionIds: List<String> =
        savedStateHandle.get<String>("preselectedIds").orEmpty().split(",").filter { it.isNotBlank() }

    private val _uiState = MutableStateFlow(
        CreateQuizUiState(
            isEditMode = editQuizId != null,
            selectedQuestionIds = preselectedQuestionIds,
            pinnedQuestionIds = preselectedQuestionIds.toSet()
        )
    )
    val uiState: StateFlow<CreateQuizUiState> = _uiState.asStateFlow()

    init {
        val quizId = editQuizId
        if (quizId != null) {
            loadQuizForEdit(quizId)
        }
        loadQuestionBank()
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
                        selectedQuestionIds = questionsResult.data.map { it.id },
                        showResults = quiz.showResults,
                        sendResultEmail = quiz.sendResultEmail,
                        allowResultPdf = quiz.allowResultPdf,
                        showLeaderboard = quiz.showLeaderboard,
                        showContactDetails = quiz.showContactDetails,
                        instructions = quiz.instructions.orEmpty(),
                        quizColor = quiz.quizColor,
                        collectEmail = quiz.collectEmail,
                        collectAddress = quiz.collectAddress,
                        collectPhone = quiz.collectPhone,
                        requireOtpVerification = quiz.requireOtpVerification,
                        allowMultipleAttempts = quiz.allowMultipleAttempts
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
    fun onTitleChange(value: String) { _uiState.value = _uiState.value.copy(title = value) }
    fun onDescriptionChange(value: String) { _uiState.value = _uiState.value.copy(description = value) }
    fun onTimeLimitTypeChange(value: String) { _uiState.value = _uiState.value.copy(timeLimitType = value) }
    fun onTimeLimitMinutesChange(value: Int) { _uiState.value = _uiState.value.copy(timeLimitMinutes = value) }
    fun onTimePerQuestionChange(value: Int) { _uiState.value = _uiState.value.copy(timePerQuestionSeconds = value) }
    fun onShuffleChange(value: Boolean) { _uiState.value = _uiState.value.copy(shuffleQuestions = value) }

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
        _uiState.value = _uiState.value.copy(questionDraft = NewQuestionDraft())
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
                difficulty = draft.difficulty,
                explanation = draft.explanation,
                tags = draft.tags,
                options = optionPairs,
                freeTextAnswer = draft.freeTextAnswer.ifBlank { null },
                imageUrl = null,
                isUngraded = draft.type == QuestionType.FREE_TEXT
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

    // ---- Step 3 ----
    fun onShowResultsChange(value: Boolean) { _uiState.value = _uiState.value.copy(showResults = value) }
    fun onSendResultEmailChange(value: Boolean) { _uiState.value = _uiState.value.copy(sendResultEmail = value) }
    fun onAllowResultPdfChange(value: Boolean) { _uiState.value = _uiState.value.copy(allowResultPdf = value) }
    fun onShowLeaderboardChange(value: Boolean) { _uiState.value = _uiState.value.copy(showLeaderboard = value) }
    fun onShowContactDetailsChange(value: Boolean) { _uiState.value = _uiState.value.copy(showContactDetails = value) }
    fun onInstructionsChange(value: String) { _uiState.value = _uiState.value.copy(instructions = value) }
    fun onQuizColorChange(value: String) { _uiState.value = _uiState.value.copy(quizColor = value) }
    fun onCollectEmailChange(value: Boolean) { _uiState.value = _uiState.value.copy(collectEmail = value) }
    fun onCollectAddressChange(value: Boolean) { _uiState.value = _uiState.value.copy(collectAddress = value) }
    fun onCollectPhoneChange(value: Boolean) { _uiState.value = _uiState.value.copy(collectPhone = value) }
    fun onRequireOtpChange(value: Boolean) { _uiState.value = _uiState.value.copy(requireOtpVerification = value) }
    fun onAllowMultipleAttemptsChange(value: Boolean) { _uiState.value = _uiState.value.copy(allowMultipleAttempts = value) }

    // ---- Step 4 ----
    fun submit() {
        val state = _uiState.value
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
                showContactDetails = state.showContactDetails,
                instructions = state.instructions.trim().ifBlank { null },
                quizColor = state.quizColor,
                collectEmail = state.collectEmail,
                collectAddress = state.collectAddress,
                collectPhone = state.collectPhone,
                requireOtpVerification = state.requireOtpVerification,
                allowMultipleAttempts = state.allowMultipleAttempts
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
