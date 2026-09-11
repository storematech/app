package com.quizmaker.android.ui.fulltest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.analytics.AnalyticsLogger
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.FULL_TEST_EXAM_SUGGESTIONS
import com.quizmaker.android.data.model.FullTestChapterConfig
import com.quizmaker.android.data.model.FullTestExamPattern
import com.quizmaker.android.data.model.FullTestGeneratedQuestion
import com.quizmaker.android.data.model.FullTestNegativeMarking
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionDifficulty
import com.quizmaker.android.data.model.QuestionFormat
import com.quizmaker.android.data.model.QuestionOption
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.repository.AiQuizRepository
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.FullTestRepository
import com.quizmaker.android.repository.QuizRepository
import com.quizmaker.android.ui.dashboard.DashboardStateCache
import com.quizmaker.android.util.TrialStatus
import com.quizmaker.android.util.trialStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

enum class FullTestStep { SELECT_EXAM, RESEARCHING, CONFIGURE, GENERATING, REVIEW }

/** Max questions any one chapter's config allows via the stepper — keeps the per-chapter table's
 *  +/- controls from producing absurd single-chapter counts; the server enforces its own overall
 *  MAX_TOTAL_QUESTIONS/MAX_CHAPTERS caps independently regardless. */
private const val MAX_CHAPTER_QUESTIONS = 30

/** How many of the user's most recent quizzes the SELECT_EXAM step's "Recent Quiz" strip shows
 *  before falling back to "View all" (Screen.QuizList). */
private const val RECENT_QUIZZES_LIMIT = 5

data class FullTestUiState(
    val step: FullTestStep = FullTestStep.SELECT_EXAM,
    val examName: String = "",
    val errorMessage: String? = null,

    // Configure step — chapters is the source of truth; totalQuestions is derived (see the getter
    // below), not stored separately, so a manual per-chapter edit and the total always agree.
    val examPattern: FullTestExamPattern? = null,
    val chapters: List<FullTestChapterConfig> = emptyList(),
    val selectedFormats: Set<QuestionFormat> = setOf(QuestionFormat.MCQ),
    val negativeMarkingEnabled: Boolean = false,

    // Review step
    val reviewQuestions: List<Question> = emptyList(),
    val selectedReviewIds: Set<String> = emptySet(),
    val failedChapters: List<String> = emptyList(),
    // Server-assigned "<exam> - Set N" title (generate-full-test numbers each successful
    // generation of the same exam so repeats aren't all identically titled) — carried through to
    // confirmSelection() instead of recomputing a generic title client-side.
    val quizTitle: String? = null,
    val isSaving: Boolean = false,
    val navigateToCreateQuizWith: List<String>? = null,
    val navigateToCreateQuizTitle: String? = null,

    // Same trial gate every other AI entry point in the app enforces.
    val isCreationBlocked: Boolean = false,
    val showTrialPaywall: Boolean = false,

    // Recent Quiz strip on the SELECT_EXAM step — up to 5 of the user's most recently created
    // quizzes (any source, not just Full Test, since the quizzes table doesn't distinguish origin).
    val recentQuizzes: List<Quiz> = emptyList(),
    val recentQuizQuestionCounts: Map<String, Int> = emptyMap()
) {
    val totalQuestions: Int get() = chapters.sumOf { it.questionCount }
    val canResearch: Boolean get() = examName.isNotBlank()
    val canGenerate: Boolean get() = chapters.any { it.questionCount > 0 } && selectedFormats.isNotEmpty()
    val hasReview: Boolean get() = reviewQuestions.isNotEmpty()

    /** Only meaningful once examPattern is loaded — the negative-marking toggle is only shown to
     *  the user at all when the researched exam actually has one (see FullTestScreen). */
    val negativeMarking get() = examPattern?.negativeMarking
}

/**
 * Drives the "Full Test" mode's whole wizard: pick/name an exam -> AI researches its real
 * structure -> user configures (question counts, formats, negative marking) -> AI generates the
 * full test, batched per chapter -> user reviews/selects which generated questions to keep, same
 * as Quick Test's review step. Nothing is written to the question bank until confirmSelection()
 * — reuses AiQuizRepository.saveQuestions() for that, same as AiQuizViewModel, rather than
 * duplicating persistence logic here.
 */
@HiltViewModel
class FullTestViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val fullTestRepository: FullTestRepository,
    private val aiQuizRepository: AiQuizRepository,
    private val quizRepository: QuizRepository,
    private val analyticsLogger: AnalyticsLogger,
    private val dashboardStateCache: DashboardStateCache
) : ViewModel() {

    val examSuggestions = FULL_TEST_EXAM_SUGGESTIONS

    private val _uiState = MutableStateFlow(FullTestUiState())
    val uiState: StateFlow<FullTestUiState> = _uiState.asStateFlow()

    init {
        loadTrialGate()
        loadRecentQuizzes()
    }

    private fun loadRecentQuizzes() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            val quizzes = (quizRepository.getQuizzesPage(userId, offset = 0, limit = RECENT_QUIZZES_LIMIT) as? AppResult.Success)?.data.orEmpty()
            _uiState.value = _uiState.value.copy(recentQuizzes = quizzes)
            if (quizzes.isNotEmpty()) {
                val counts = (quizRepository.getQuestionCountsForQuizzes(quizzes.map { it.id }) as? AppResult.Success)?.data.orEmpty()
                _uiState.value = _uiState.value.copy(recentQuizQuestionCounts = counts)
            }
        }
    }

    private fun loadTrialGate() {
        viewModelScope.launch {
            val profile = (authRepository.getCurrentProfile() as? AppResult.Success)?.data ?: return@launch
            _uiState.value = _uiState.value.copy(isCreationBlocked = profile.trialStatus() is TrialStatus.Expired)
        }
    }

    fun dismissTrialPaywall() {
        _uiState.value = _uiState.value.copy(showTrialPaywall = false)
    }

    fun onExamNameChange(value: String) {
        _uiState.value = _uiState.value.copy(examName = value, errorMessage = null)
    }

    /** Tapping a suggestion chip/card goes straight to researching that exam — unlike a plain
     *  tap-to-fill chip, showing the name land in the search box first and then requiring a
     *  separate "Create Full Test" tap was an extra, confusing step for something the user already
     *  committed to by tapping a specific exam. */
    fun selectExamSuggestion(name: String) {
        _uiState.value = _uiState.value.copy(examName = name, errorMessage = null)
        startResearch()
    }

    fun startResearch() {
        val state = _uiState.value
        if (!state.canResearch || state.step == FullTestStep.RESEARCHING) return
        if (state.isCreationBlocked) {
            _uiState.value = state.copy(showTrialPaywall = true)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(step = FullTestStep.RESEARCHING, errorMessage = null)
            when (val result = fullTestRepository.researchExam(state.examName.trim())) {
                is AppResult.Success -> {
                    val pattern = result.data
                    _uiState.value = _uiState.value.copy(
                        step = FullTestStep.CONFIGURE,
                        examPattern = pattern,
                        chapters = distributeQuestions(pattern, pattern.totalQuestions.coerceAtLeast(1)),
                        negativeMarkingEnabled = pattern.negativeMarking?.enabled == true
                    )
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(step = FullTestStep.SELECT_EXAM, errorMessage = result.message)
            }
        }
    }

    fun onBackToSelectExam() {
        _uiState.value = FullTestUiState(examName = _uiState.value.examName, isCreationBlocked = _uiState.value.isCreationBlocked)
    }

    /** Redistributes every chapter's question count proportional to its own weightage within its
     *  subject (subjects themselves split the total evenly — the AI's research gives weightage
     *  *within* a subject, not a subject-level share of the whole exam, so an even subject split
     *  is the simplest defensible default). Each chapter's own easy/medium/hard percentages then
     *  split that chapter's count, rounding remainders into the medium bucket. */
    private fun distributeQuestions(pattern: FullTestExamPattern, totalQuestions: Int): List<FullTestChapterConfig> {
        val subjects = pattern.subjects.filter { it.chapters.isNotEmpty() }
        if (subjects.isEmpty()) return emptyList()

        val perSubject = totalQuestions / subjects.size
        var remainder = totalQuestions - perSubject * subjects.size

        return subjects.flatMap { subject ->
            val subjectTotal = perSubject + if (remainder-- > 0) 1 else 0
            val weightSum = subject.chapters.sumOf { it.weightagePercent }.takeIf { it > 0 } ?: subject.chapters.size
            var chapterRemainder = subjectTotal

            subject.chapters.mapIndexed { index, chapter ->
                val weight = chapter.weightagePercent.takeIf { it > 0 } ?: 1
                val count = if (index == subject.chapters.lastIndex) {
                    chapterRemainder
                } else {
                    (subjectTotal * weight / weightSum).coerceAtMost(chapterRemainder)
                }
                chapterRemainder -= count

                val easy = (count * chapter.easyPercent / 100.0).roundToInt()
                val hard = (count * chapter.hardPercent / 100.0).roundToInt()
                val medium = (count - easy - hard).coerceAtLeast(0)

                FullTestChapterConfig(
                    subject = subject.name,
                    chapter = chapter.name,
                    questionCount = count,
                    easyCount = easy,
                    mediumCount = medium,
                    hardCount = hard
                )
            }
        }
    }

    /** The Configure step's overall "Total Questions" +/- control — redistributes every chapter
     *  from scratch at the new total, same as [resetEvenly]. Any individual chapter counts the
     *  user had manually tweaked are reset along with it — simplest predictable behavior for a
     *  single control that's meant to represent "the whole test's size". */
    fun onTotalQuestionsChange(newTotal: Int) {
        val pattern = _uiState.value.examPattern ?: return
        _uiState.value = _uiState.value.copy(chapters = distributeQuestions(pattern, newTotal.coerceIn(1, MAX_TOTAL_QUESTIONS_CLIENT)))
    }

    fun resetEvenly() {
        val pattern = _uiState.value.examPattern ?: return
        _uiState.value = _uiState.value.copy(chapters = distributeQuestions(pattern, _uiState.value.totalQuestions.coerceAtLeast(1)))
    }

    /** One row's manual override in the Configure step's chapter table — recomputes just that
     *  chapter's own easy/medium/hard split from its researched percentages; every other chapter
     *  is left exactly as it was. */
    fun onChapterCountChange(chapterId: String, newCount: Int) {
        val pattern = _uiState.value.examPattern ?: return
        val clamped = newCount.coerceIn(0, MAX_CHAPTER_QUESTIONS)
        _uiState.value = _uiState.value.copy(
            chapters = _uiState.value.chapters.map { config ->
                if (config.id != chapterId) return@map config
                val chapter = pattern.subjects.firstOrNull { it.name == config.subject }
                    ?.chapters?.firstOrNull { it.name == config.chapter }
                val easy = chapter?.let { (clamped * it.easyPercent / 100.0).roundToInt() } ?: 0
                val hard = chapter?.let { (clamped * it.hardPercent / 100.0).roundToInt() } ?: 0
                config.copy(questionCount = clamped, easyCount = easy, mediumCount = (clamped - easy - hard).coerceAtLeast(0), hardCount = hard)
            }
        )
    }

    fun onFormatToggle(format: QuestionFormat) {
        val current = _uiState.value.selectedFormats
        val updated = if (format in current) current - format else current + format
        // At least one format must always stay selected — an empty set would leave the AI with
        // no allowed format to use at all.
        if (updated.isNotEmpty()) _uiState.value = _uiState.value.copy(selectedFormats = updated)
    }

    fun onNegativeMarkingToggle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(negativeMarkingEnabled = enabled)
    }

    fun generateFullTest() {
        val state = _uiState.value
        if (!state.canGenerate || state.step == FullTestStep.GENERATING) return
        if (state.isCreationBlocked) {
            _uiState.value = state.copy(showTrialPaywall = true)
            return
        }
        // Same session-not-ready guard as AiQuizViewModel.runGeneration() — without it, a tap
        // before the Supabase SDK finishes restoring/refreshing the session sends the edge
        // function a request with no valid token, which 401s in under a second and surfaces to
        // the user as the same generic "high demand" text a real AI outage would show.
        if (authRepository.currentUserId() == null) {
            _uiState.value = state.copy(errorMessage = "You're not signed in yet — please try again in a moment.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(step = FullTestStep.GENERATING, errorMessage = null)
            val activeChapters = state.chapters.filter { it.questionCount > 0 }
            when (
                val result = fullTestRepository.generateFullTest(
                    examName = state.examName.trim(),
                    chapters = activeChapters,
                    formats = state.selectedFormats.map { it.apiValue },
                    negativeMarking = if (state.negativeMarkingEnabled) state.negativeMarking else null
                )
            ) {
                is AppResult.Success -> {
                    val questions = result.data.questions.mapIndexed { index, q -> q.toQuestion(index) }
                    analyticsLogger.logAiQuizGenerated(source = "full_test", questionCount = questions.size)
                    _uiState.value = _uiState.value.copy(
                        step = FullTestStep.REVIEW,
                        reviewQuestions = questions,
                        selectedReviewIds = questions.map { it.id }.toSet(),
                        failedChapters = result.data.failedChapters,
                        quizTitle = result.data.quizTitle
                    )
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(step = FullTestStep.CONFIGURE, errorMessage = result.message)
            }
        }
    }

    /** [format]/"mcq"|"numerical"|"descriptive" maps onto the same Question shape the rest of the
     *  app already understands — numerical/descriptive both become FREE_TEXT. isUngraded defaults
     *  to false (gradable) here, same as every other question-creation path (AiQuizRepository,
     *  QuestionBankViewModel's NewQuestionDraft) — a submission still can't be auto-graded, but a
     *  gradable free-text question is what makes it show up in QuizDetailScreen's Manual Marking
     *  card at all. This used to hardcode `true` for every non-mcq question, which silently kept
     *  every Full Test free-text question permanently ungraded with no way to enable marking. */
    private fun FullTestGeneratedQuestion.toQuestion(index: Int): Question {
        val correctCount = options.count { it.second }
        val isMcq = format == "mcq" && options.size >= 2 && correctCount >= 1
        val difficultyEnum = QuestionDifficulty.entries.firstOrNull { it.value == difficulty } ?: QuestionDifficulty.MEDIUM
        return Question(
            id = "full-test-review-$index",
            text = text,
            // The prompt occasionally allows more than one correct mcq option for math/physics
            // "statements is/are true" style questions — reflect that instead of forcing every
            // mcq into single-choice regardless of how many options are actually marked correct.
            type = when {
                !isMcq -> QuestionType.FREE_TEXT
                correctCount > 1 -> QuestionType.MULTI_CHOICE
                else -> QuestionType.SINGLE_CHOICE
            },
            options = if (isMcq) {
                options.mapIndexed { optIndex, (optText, isCorrect) ->
                    QuestionOption(id = "full-test-review-$index-opt-$optIndex", text = optText, isCorrect = isCorrect)
                }
            } else {
                emptyList()
            },
            correctAnswer = if (isMcq) null else (numericalAnswer ?: explanation),
            explanation = explanation,
            difficulty = difficultyEnum,
            tags = listOf("AI Generated", subject, chapter),
            points = 1.0,
            negativePoints = 0.0,
            imageUrl = null,
            isUngraded = false,
            createdAt = null
        )
    }

    fun toggleReviewQuestion(questionId: String) {
        val current = _uiState.value.selectedReviewIds
        _uiState.value = _uiState.value.copy(selectedReviewIds = if (questionId in current) current - questionId else current + questionId)
    }

    fun selectAllReview() {
        _uiState.value = _uiState.value.copy(selectedReviewIds = _uiState.value.reviewQuestions.map { it.id }.toSet())
    }

    fun deselectAllReview() {
        _uiState.value = _uiState.value.copy(selectedReviewIds = emptySet())
    }

    fun confirmSelection() {
        val state = _uiState.value
        if (state.selectedReviewIds.isEmpty() || state.isSaving) return
        val userId = authRepository.currentUserId() ?: return
        val selected = state.reviewQuestions.filter { it.id in state.selectedReviewIds }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            when (val result = aiQuizRepository.saveQuestions(userId, selected)) {
                is AppResult.Success -> {
                    dashboardStateCache.needsRefresh = true
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        navigateToCreateQuizWith = result.data.map { it.id },
                        navigateToCreateQuizTitle = state.quizTitle ?: state.examPattern?.examName ?: state.examName
                    )
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
            }
        }
    }

    fun consumeNavigation() {
        _uiState.value = FullTestUiState()
    }

    private companion object {
        const val MAX_TOTAL_QUESTIONS_CLIENT = 250
    }
}
