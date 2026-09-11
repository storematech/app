package com.quizmaker.android.ui.omr

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Learner
import com.quizmaker.android.data.model.OmrLayout
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.LearnersRepository
import com.quizmaker.android.repository.OmrGradedAnswer
import com.quizmaker.android.repository.OmrRepository
import com.quizmaker.android.repository.QuizRepository
import com.quizmaker.android.util.PdfBranding
import com.quizmaker.android.util.PdfBrandingProvider
import com.quizmaker.android.util.omr.OmrBubbleResult
import com.quizmaker.android.util.omr.OmrGradingEngine
import com.quizmaker.android.util.omr.OmrScanQuestionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Where the capture/review flow currently is. */
enum class OmrScanStep { CAPTURE, SCANNING, SCAN_FAILED, REVIEW }

sealed class OmrSubmitState {
    object Idle : OmrSubmitState()
    object Submitting : OmrSubmitState()
    object Success : OmrSubmitState()
    data class Error(val message: String) : OmrSubmitState()
}

data class OmrScanUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val quizTitle: String = "",
    val questions: List<Question> = emptyList(),
    /** null while still checking; false = no sheet has ever been exported for this quiz yet. */
    val hasSheetLayout: Boolean? = null,
    val layout: OmrLayout? = null,
    val isExportingSheet: Boolean = false,

    val step: OmrScanStep = OmrScanStep.CAPTURE,
    val warpedBitmap: Bitmap? = null,
    /** Raw engine output for the currently-reviewed photo, keyed for lookup by questionId. */
    val bubbledResults: Map<String, OmrScanQuestionResult> = emptyMap(),
    /** questionId -> currently-selected optionId(s), editable by the teacher; pre-seeded from the
     *  engine's own [OmrBubbleResult.Detected] calls so every bubbled answer is editable, not just
     *  the ones flagged Ambiguous. */
    val selections: Map<String, List<String>> = emptyMap(),
    /** Bubbled questions whose engine call was Ambiguous and hasn't been manually resolved yet --
     *  "Save & Score" stays disabled while this is non-empty. */
    val unresolvedAmbiguous: Set<String> = emptySet(),

    val allLearners: List<Learner> = emptyList(),
    val learnerQuery: String = "",
    val filteredLearners: List<Learner> = emptyList(),
    val selectedLearner: Learner? = null,

    val submitState: OmrSubmitState = OmrSubmitState.Idle
)

@HiltViewModel
class OmrScanViewModel @Inject constructor(
    private val omrRepository: OmrRepository,
    private val quizRepository: QuizRepository,
    private val learnersRepository: LearnersRepository,
    private val authRepository: AuthRepository,
    private val pdfBrandingProvider: PdfBrandingProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: String = checkNotNull(savedStateHandle["quizId"])

    private val _uiState = MutableStateFlow(OmrScanUiState())
    val uiState: StateFlow<OmrScanUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val quizResult = quizRepository.getQuizById(quizId)
            val questionsResult = quizRepository.getQuestionsForQuiz(quizId)
            val layoutResult = omrRepository.getLatestLayout(quizId)

            val quizTitle = (quizResult as? AppResult.Success)?.data?.title.orEmpty()
            val questions = (questionsResult as? AppResult.Success)?.data.orEmpty()

            when {
                quizResult is AppResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = quizResult.message) }
                questionsResult is AppResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = questionsResult.message) }
                layoutResult is AppResult.Error -> _uiState.update {
                    it.copy(isLoading = false, quizTitle = quizTitle, questions = questions, hasSheetLayout = false, errorMessage = layoutResult.message)
                }
                layoutResult is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        quizTitle = quizTitle,
                        questions = questions,
                        hasSheetLayout = layoutResult.data != null,
                        layout = layoutResult.data
                    )
                }
            }

            val userId = authRepository.currentUserId()
            if (userId != null) {
                (learnersRepository.getLearners(userId) as? AppResult.Success)?.let { result ->
                    _uiState.update { it.copy(allLearners = result.data) }
                }
            }
        }
    }

    suspend fun getPdfBranding(): PdfBranding = pdfBrandingProvider.get()

    /** Called by the screen right after it exports+shares a freshly generated sheet PDF, to
     *  persist the [layout] the same way MasterPaperScreen's export flow would if it also needed
     *  to save one -- see OmrSheetPdfExporter/OmrRepository.saveLayout. */
    fun onSheetExported(layout: OmrLayout) {
        val userId = authRepository.currentUserId() ?: run {
            _uiState.update { it.copy(errorMessage = "You need to be signed in to generate an answer sheet.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingSheet = true, errorMessage = null) }
            when (val result = omrRepository.saveLayout(quizId, userId, layout)) {
                is AppResult.Success -> _uiState.update { it.copy(isExportingSheet = false, hasSheetLayout = true, layout = layout) }
                is AppResult.Error -> _uiState.update { it.copy(isExportingSheet = false, errorMessage = result.message) }
            }
        }
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        val layout = _uiState.value.layout ?: return
        _uiState.update { it.copy(step = OmrScanStep.SCANNING) }
        viewModelScope.launch {
            val scanResult = withContext(Dispatchers.Default) { OmrGradingEngine.scan(bitmap, layout) }
            if (!scanResult.fiducialsFound) {
                _uiState.update {
                    it.copy(
                        step = OmrScanStep.SCAN_FAILED,
                        warpedBitmap = null,
                        bubbledResults = emptyMap(),
                        selections = emptyMap(),
                        unresolvedAmbiguous = emptySet()
                    )
                }
                return@launch
            }

            val selections = scanResult.perQuestion.associate { qr ->
                qr.questionId to when (val r = qr.result) {
                    is OmrBubbleResult.Detected -> r.optionIds
                    else -> emptyList()
                }
            }
            val ambiguous = scanResult.perQuestion.filter { it.result is OmrBubbleResult.Ambiguous }.map { it.questionId }.toSet()

            _uiState.update {
                it.copy(
                    step = OmrScanStep.REVIEW,
                    warpedBitmap = scanResult.warpedBitmap,
                    bubbledResults = scanResult.perQuestion.associateBy { qr -> qr.questionId },
                    selections = selections,
                    unresolvedAmbiguous = ambiguous,
                    submitState = OmrSubmitState.Idle
                )
            }
        }
    }

    /** Teacher taps a different option letter than what the engine detected (or resolves an
     *  Ambiguous call). [optionIds] replaces the current selection outright -- callers decide
     *  single-vs-multi selection semantics (see OmrScanScreen's toggle logic). */
    fun onOverrideAnswer(questionId: String, optionIds: List<String>) {
        _uiState.update {
            it.copy(
                selections = it.selections + (questionId to optionIds),
                unresolvedAmbiguous = it.unresolvedAmbiguous - questionId
            )
        }
    }

    fun onLearnerQueryChange(text: String) {
        _uiState.update {
            val filtered = if (text.isBlank()) emptyList() else it.allLearners.filter { l -> l.name.contains(text, ignoreCase = true) }
            it.copy(learnerQuery = text, filteredLearners = filtered, selectedLearner = null)
        }
    }

    /** Kept as a distinct entry point per this feature's plan, but there's only one underlying name
     *  field: if nothing in [onLearnerQueryChange]'s filtered list matches, whatever the teacher
     *  typed is used verbatim as the free-text learner label -- there's no separate state to hold
     *  for that, so this just forwards to the same query update. */
    fun onFreeTextNameChange(text: String) = onLearnerQueryChange(text)

    fun onLearnerSelected(learner: Learner) {
        _uiState.update { it.copy(selectedLearner = learner, learnerQuery = learner.name, filteredLearners = emptyList()) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.unresolvedAmbiguous.isNotEmpty()) return
        val label = (state.selectedLearner?.name ?: state.learnerQuery).trim()
        if (label.isBlank()) {
            _uiState.update { it.copy(submitState = OmrSubmitState.Error("Enter or select a student name first.")) }
            return
        }
        val email = state.selectedLearner?.email?.takeIf { it.isNotBlank() } ?: "${slugify(label)}@offline.scan"
        val answers = buildGradedAnswers(state)

        _uiState.update { it.copy(submitState = OmrSubmitState.Submitting) }
        viewModelScope.launch {
            when (val result = omrRepository.submitScannedResponse(quizId, label, email, answers)) {
                is AppResult.Success -> _uiState.update { it.copy(submitState = OmrSubmitState.Success) }
                is AppResult.Error -> _uiState.update { it.copy(submitState = OmrSubmitState.Error(result.message)) }
            }
        }
    }

    /** Resets back to the capture step to scan the next physical paper, keeping the loaded
     *  quiz/layout/learner-roster state so a whole class's papers can be scanned in one sitting. */
    fun onScanNext() {
        _uiState.update {
            it.copy(
                step = OmrScanStep.CAPTURE,
                warpedBitmap = null,
                bubbledResults = emptyMap(),
                selections = emptyMap(),
                unresolvedAmbiguous = emptySet(),
                learnerQuery = "",
                filteredLearners = emptyList(),
                selectedLearner = null,
                submitState = OmrSubmitState.Idle
            )
        }
    }

    /**
     * Reconciles the current photo's selections against the quiz's real [Question] list into the
     * [OmrGradedAnswer]s [OmrRepository.submitScannedResponse] needs. Mirrors
     * TakeQuizViewModel.grade()'s exact scoring rules (confirmed by reading it):
     *  - SINGLE_CHOICE: correct iff the one selected option's `isCorrect`; a skipped/blank question
     *    scores 0 with status "incorrect" (TakeQuizViewModel never uses a separate "unanswered"
     *    status); an attempted-but-wrong pick costs `negativePoints`.
     *  - MULTI_CHOICE: correct only on an EXACT set match against the correct option ids (no partial
     *    credit) -- `selectedIds == correctIds`; empty selection scores 0, any other wrong subset
     *    costs `negativePoints`.
     *  - FREE_TEXT/FILL_IN_BLANK: never auto-graded -- status "ungraded", 0 points, `answerLabel`
     *    null, `correctOptionText` = the question's own `correctAnswer` -- same shape
     *    TakeQuizViewModel.grade() produces for an online free-text submission, so it lands in the
     *    same Manual Marking queue.
     * Only questions present on `layout.pages[0]` (this v1's single photographed page -- see
     * OmrGradingEngine's KDoc) are included.
     */
    private fun buildGradedAnswers(state: OmrScanUiState): List<OmrGradedAnswer> {
        val page = state.layout?.pages?.firstOrNull() ?: return emptyList()
        val questionsById = state.questions.associateBy { it.id }

        return page.questions.mapNotNull { layoutQuestion ->
            val question = questionsById[layoutQuestion.questionId] ?: return@mapNotNull null

            if (layoutQuestion.bubbles.isEmpty()) {
                // FREE_TEXT / FILL_IN_BLANK -- never bubbled, never auto-graded.
                return@mapNotNull OmrGradedAnswer(
                    questionId = question.id,
                    questionText = question.text,
                    questionType = question.type,
                    answerLabel = null,
                    selectedOptionId = null,
                    selectedOptionText = null,
                    correctOptionId = null,
                    correctOptionText = question.correctAnswer,
                    isCorrect = false,
                    pointsEarned = 0.0,
                    status = "ungraded"
                )
            }

            val selectedIds = state.selections[question.id].orEmpty()
            val bubblesByOptionId = layoutQuestion.bubbles.associateBy { it.optionId }

            if (question.type == QuestionType.MULTI_CHOICE) {
                val selectedSet = selectedIds.toSet()
                val correctSet = question.correctOptionIds.toSet()
                val isCorrect = selectedSet.isNotEmpty() && selectedSet == correctSet
                val label = selectedIds.mapNotNull { bubblesByOptionId[it]?.label }.takeIf { it.isNotEmpty() }?.joinToString(", ")
                OmrGradedAnswer(
                    questionId = question.id,
                    questionText = question.text,
                    questionType = question.type,
                    answerLabel = label,
                    selectedOptionId = null,
                    selectedOptionText = question.options.filter { it.id in selectedSet }.joinToString(", ") { it.text }.ifBlank { null },
                    correctOptionId = null,
                    correctOptionText = question.options.filter { it.isCorrect }.joinToString(", ") { it.text }.ifBlank { null },
                    isCorrect = isCorrect,
                    pointsEarned = when {
                        isCorrect -> question.points
                        selectedSet.isEmpty() -> 0.0
                        else -> -question.negativePoints
                    },
                    status = if (isCorrect) "correct" else "incorrect"
                )
            } else {
                // SINGLE_CHOICE (the only other bubbled type).
                val selectedId = selectedIds.firstOrNull()
                val selectedOption = question.options.firstOrNull { it.id == selectedId }
                val correctOption = question.options.firstOrNull { it.isCorrect }
                val isCorrect = selectedId != null && selectedId == correctOption?.id
                OmrGradedAnswer(
                    questionId = question.id,
                    questionText = question.text,
                    questionType = question.type,
                    answerLabel = selectedId?.let { bubblesByOptionId[it]?.label },
                    selectedOptionId = selectedId,
                    selectedOptionText = selectedOption?.text,
                    correctOptionId = correctOption?.id,
                    correctOptionText = correctOption?.text,
                    isCorrect = isCorrect,
                    pointsEarned = when {
                        isCorrect -> question.points
                        selectedId == null -> 0.0
                        else -> -question.negativePoints
                    },
                    status = if (selectedId == null) "incorrect" else if (isCorrect) "correct" else "incorrect"
                )
            }
        }
    }

    private fun slugify(name: String): String {
        val slug = name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return slug.ifBlank { "student" }
    }
}
