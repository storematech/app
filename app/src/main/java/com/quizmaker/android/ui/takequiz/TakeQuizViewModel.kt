package com.quizmaker.android.ui.takequiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.analytics.AnalyticsLogger
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.data.model.QuizForTaking
import com.quizmaker.android.repository.GradedAnswer
import com.quizmaker.android.repository.QuizTakingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TakeQuizPhase { LOADING, NOT_FOUND, ALREADY_COMPLETED, REGISTRATION, OTP, QUESTIONS, SUBMITTING, RESULT }

data class AnswerInput(
    val selectedOptionIds: Set<String> = emptySet(),
    val freeText: String = ""
)

data class TakeQuizUiState(
    val phase: TakeQuizPhase = TakeQuizPhase.LOADING,
    val errorMessage: String? = null,

    val quizForTaking: QuizForTaking? = null,

    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",

    val otpCode: String = "",
    val isSendingOtp: Boolean = false,
    val otpError: String? = null,

    val currentQuestionIndex: Int = 0,
    val answers: Map<String, AnswerInput> = emptyMap(),
    val overallSecondsRemaining: Int? = null,
    val perQuestionSecondsRemaining: Int? = null,

    val finalScore: Int = 0
) {
    val currentQuestion: Question? get() = quizForTaking?.questions?.getOrNull(currentQuestionIndex)
    val isLastQuestion: Boolean get() = quizForTaking?.let { currentQuestionIndex == it.questions.lastIndex } ?: true
    val maxPoints: Int get() = quizForTaking?.questions?.filter { !it.isUngraded }?.sumOf { it.points } ?: 0
    val scorePercent: Int get() = if (maxPoints == 0) 0 else (finalScore * 100 / maxPoints)
}

@HiltViewModel
class TakeQuizViewModel @Inject constructor(
    private val repository: QuizTakingRepository,
    private val analyticsLogger: AnalyticsLogger,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val shareId: String = checkNotNull(savedStateHandle["shareId"])

    private val _uiState = MutableStateFlow(TakeQuizUiState())
    val uiState: StateFlow<TakeQuizUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadQuiz()
    }

    private fun loadQuiz() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(phase = TakeQuizPhase.LOADING)
            when (val result = repository.getQuizByShareId(shareId)) {
                is AppResult.Success -> {
                    if (result.data.quiz.isClosed) {
                        _uiState.value = _uiState.value.copy(
                            phase = TakeQuizPhase.NOT_FOUND,
                            errorMessage = "This quiz is closed and no longer accepting responses."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            phase = TakeQuizPhase.REGISTRATION,
                            quizForTaking = result.data,
                            overallSecondsRemaining = result.data.quiz.timeLimit?.let { it * 60 }
                        )
                    }
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(phase = TakeQuizPhase.NOT_FOUND, errorMessage = result.message)
            }
        }
    }

    fun onNameChange(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun onEmailChange(value: String) { _uiState.value = _uiState.value.copy(email = value) }
    fun onPhoneChange(value: String) { _uiState.value = _uiState.value.copy(phone = value) }
    fun onAddressChange(value: String) { _uiState.value = _uiState.value.copy(address = value) }
    fun onOtpCodeChange(value: String) { _uiState.value = _uiState.value.copy(otpCode = value) }

    fun submitRegistration() {
        val quiz = _uiState.value.quizForTaking?.quiz ?: return
        val email = _uiState.value.email.trim()
        if (quiz.collectEmail && (email.isBlank() || !email.contains("@"))) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter a valid email address.")
            return
        }
        if (_uiState.value.name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter your name.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(errorMessage = null)

            if (!quiz.allowMultipleAttempts && email.isNotBlank()) {
                val completed = repository.hasCompletedQuiz(quiz.id, email)
                if (completed is AppResult.Success && completed.data) {
                    _uiState.value = _uiState.value.copy(
                        phase = TakeQuizPhase.ALREADY_COMPLETED,
                        errorMessage = "You've already completed this quiz."
                    )
                    return@launch
                }
            }

            if (quiz.requireOtpVerification && email.isNotBlank()) {
                _uiState.value = _uiState.value.copy(isSendingOtp = true)
                when (val result = repository.sendOtp(email, quiz.id, quiz.title)) {
                    is AppResult.Success -> _uiState.value =
                        _uiState.value.copy(isSendingOtp = false, phase = TakeQuizPhase.OTP)
                    is AppResult.Error -> _uiState.value =
                        _uiState.value.copy(isSendingOtp = false, errorMessage = result.message)
                }
            } else {
                startQuiz()
            }
        }
    }

    fun resendOtp() {
        val quiz = _uiState.value.quizForTaking?.quiz ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingOtp = true, otpError = null)
            when (val result = repository.sendOtp(_uiState.value.email.trim(), quiz.id, quiz.title)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isSendingOtp = false)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSendingOtp = false, otpError = result.message)
            }
        }
    }

    fun verifyOtp() {
        val quiz = _uiState.value.quizForTaking?.quiz ?: return
        val code = _uiState.value.otpCode.trim()
        if (code.length != 6) {
            _uiState.value = _uiState.value.copy(otpError = "Enter the 6-digit code.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(otpError = null, isSendingOtp = true)
            when (val result = repository.verifyOtp(_uiState.value.email.trim(), quiz.id, code)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSendingOtp = false)
                    startQuiz()
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isSendingOtp = false, otpError = result.message)
            }
        }
    }

    private fun startQuiz() {
        _uiState.value = _uiState.value.copy(
            phase = TakeQuizPhase.QUESTIONS,
            currentQuestionIndex = 0,
            perQuestionSecondsRemaining = _uiState.value.quizForTaking?.quiz?.timePerQuestion
        )
        startTimerIfNeeded()
    }

    private fun startTimerIfNeeded() {
        timerJob?.cancel()
        val quiz = _uiState.value.quizForTaking?.quiz ?: return
        if (quiz.timeLimitType == "overall" && _uiState.value.overallSecondsRemaining != null) {
            timerJob = viewModelScope.launch {
                while (_uiState.value.phase == TakeQuizPhase.QUESTIONS) {
                    delay(1000)
                    val remaining = (_uiState.value.overallSecondsRemaining ?: 0) - 1
                    if (remaining <= 0) {
                        _uiState.value = _uiState.value.copy(overallSecondsRemaining = 0)
                        submitQuiz()
                        break
                    }
                    _uiState.value = _uiState.value.copy(overallSecondsRemaining = remaining)
                }
            }
        } else if (quiz.timeLimitType == "per_question" && quiz.timePerQuestion != null) {
            timerJob = viewModelScope.launch {
                while (_uiState.value.phase == TakeQuizPhase.QUESTIONS) {
                    delay(1000)
                    val remaining = (_uiState.value.perQuestionSecondsRemaining ?: 0) - 1
                    if (remaining <= 0) {
                        if (_uiState.value.isLastQuestion) {
                            submitQuiz()
                        } else {
                            goToNextQuestion()
                        }
                        break
                    }
                    _uiState.value = _uiState.value.copy(perQuestionSecondsRemaining = remaining)
                }
            }
        }
    }

    fun selectSingleOption(questionId: String, optionId: String) {
        updateAnswer(questionId) { it.copy(selectedOptionIds = setOf(optionId)) }
    }

    fun toggleMultiOption(questionId: String, optionId: String) {
        updateAnswer(questionId) {
            val updated = if (optionId in it.selectedOptionIds) it.selectedOptionIds - optionId else it.selectedOptionIds + optionId
            it.copy(selectedOptionIds = updated)
        }
    }

    fun onFreeTextChange(questionId: String, text: String) {
        updateAnswer(questionId) { it.copy(freeText = text) }
    }

    private fun updateAnswer(questionId: String, transform: (AnswerInput) -> AnswerInput) {
        val current = _uiState.value.answers[questionId] ?: AnswerInput()
        _uiState.value = _uiState.value.copy(answers = _uiState.value.answers + (questionId to transform(current)))
    }

    fun goToNextQuestion() {
        val state = _uiState.value
        if (state.isLastQuestion) {
            submitQuiz()
        } else {
            _uiState.value = state.copy(
                currentQuestionIndex = state.currentQuestionIndex + 1,
                perQuestionSecondsRemaining = state.quizForTaking?.quiz?.timePerQuestion
            )
            startTimerIfNeeded()
        }
    }

    fun goToPreviousQuestion() {
        val state = _uiState.value
        if (state.currentQuestionIndex > 0) {
            _uiState.value = state.copy(
                currentQuestionIndex = state.currentQuestionIndex - 1,
                perQuestionSecondsRemaining = state.quizForTaking?.quiz?.timePerQuestion
            )
            startTimerIfNeeded()
        }
    }

    fun submitQuiz() {
        timerJob?.cancel()
        val quizForTaking = _uiState.value.quizForTaking ?: return
        val state = _uiState.value

        val gradedAnswers = quizForTaking.questions.map { question ->
            grade(question, state.answers[question.id] ?: AnswerInput())
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(phase = TakeQuizPhase.SUBMITTING)
            val result = repository.submitQuizResponse(
                quizId = quizForTaking.quiz.id,
                userEmail = state.email.trim().ifBlank { "anonymous@yunolms.com" },
                userName = state.name.trim().ifBlank { null },
                phoneNumber = state.phone.trim().ifBlank { null },
                answers = gradedAnswers,
                maxPoints = state.maxPoints
            )
            when (result) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(phase = TakeQuizPhase.RESULT, finalScore = result.data)
                    val scorePercent = if (state.maxPoints == 0) 0 else (result.data * 100 / state.maxPoints)
                    analyticsLogger.logQuizSubmitted(quizForTaking.quiz.id, scorePercent)
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(phase = TakeQuizPhase.QUESTIONS, errorMessage = result.message)
            }
        }
    }

    private fun grade(question: Question, input: AnswerInput): GradedAnswer {
        if (question.isUngraded) {
            return GradedAnswer(
                questionId = question.id,
                questionText = question.text,
                questionType = question.type.value,
                answerText = input.freeText.ifBlank { null },
                selectedOptionId = null,
                selectedOptionText = null,
                correctOptionId = null,
                correctOptionText = null,
                isCorrect = false,
                pointsEarned = 0,
                status = "ungraded"
            )
        }

        return when (question.type) {
            QuestionType.SINGLE_CHOICE -> {
                val selectedId = input.selectedOptionIds.firstOrNull()
                val selectedOption = question.options.firstOrNull { it.id == selectedId }
                val correctOption = question.options.firstOrNull { it.isCorrect }
                val isCorrect = selectedId != null && selectedId == correctOption?.id
                GradedAnswer(
                    questionId = question.id,
                    questionText = question.text,
                    questionType = question.type.value,
                    answerText = null,
                    selectedOptionId = selectedId,
                    selectedOptionText = selectedOption?.text,
                    correctOptionId = correctOption?.id,
                    correctOptionText = correctOption?.text,
                    isCorrect = isCorrect,
                    pointsEarned = if (isCorrect) question.points else 0,
                    status = if (selectedId == null) "incorrect" else if (isCorrect) "correct" else "incorrect"
                )
            }

            QuestionType.MULTI_CHOICE -> {
                val correctIds = question.correctOptionIds.toSet()
                val isCorrect = input.selectedOptionIds.isNotEmpty() && input.selectedOptionIds == correctIds
                GradedAnswer(
                    questionId = question.id,
                    questionText = question.text,
                    questionType = question.type.value,
                    answerText = input.selectedOptionIds.joinToString(","),
                    selectedOptionId = null,
                    selectedOptionText = question.options.filter { it.id in input.selectedOptionIds }.joinToString(", ") { it.text },
                    correctOptionId = null,
                    correctOptionText = question.options.filter { it.isCorrect }.joinToString(", ") { it.text },
                    isCorrect = isCorrect,
                    pointsEarned = if (isCorrect) question.points else 0,
                    status = if (isCorrect) "correct" else "incorrect"
                )
            }

            QuestionType.FREE_TEXT, QuestionType.FILL_IN_BLANK -> GradedAnswer(
                questionId = question.id,
                questionText = question.text,
                questionType = question.type.value,
                answerText = input.freeText.ifBlank { null },
                selectedOptionId = null,
                selectedOptionText = null,
                correctOptionId = null,
                correctOptionText = question.correctAnswer,
                isCorrect = false,
                pointsEarned = 0,
                status = "ungraded"
            )
        }
    }
}
