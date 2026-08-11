package com.quizmaker.android.ui.questionbank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.alert.AlertBus
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.NewQuestionDraft
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionDifficulty
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.data.model.toDraft
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.QuestionRepository
import com.quizmaker.android.util.TrialStatus
import com.quizmaker.android.util.trialStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestionBankUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val questions: List<Question> = emptyList(),
    val searchQuery: String = "",
    val tagFilter: String? = null,
    val difficultyFilter: QuestionDifficulty? = null,
    val isFilterSheetOpen: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedQuestionIds: Set<String> = emptySet(),
    val draft: NewQuestionDraft? = null,
    val isSaving: Boolean = false,
    val isCreationBlocked: Boolean = false,
    val showTrialPaywall: Boolean = false,
    // Non-null while that question's delete request is in flight — lets the row show a "Deleting…" state.
    val deletingQuestionId: String? = null
) {
    val filtered: List<Question>
        get() = questions.filter { question ->
            val matchesSearch = searchQuery.isBlank() ||
                question.text.contains(searchQuery, ignoreCase = true) ||
                question.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
            val matchesTag = tagFilter == null || tagFilter in question.tags
            val matchesDifficulty = difficultyFilter == null || question.difficulty == difficultyFilter
            matchesSearch && matchesTag && matchesDifficulty
        }

    val availableTags: List<String> get() = questions.flatMap { it.tags }.distinct().sorted()
}

@HiltViewModel
class QuestionBankViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val questionRepository: QuestionRepository
) : ViewModel() {

    companion object {
        const val MAX_SELECTABLE_QUESTIONS = 100
    }

    private val _uiState = MutableStateFlow(QuestionBankUiState())
    val uiState: StateFlow<QuestionBankUiState> = _uiState.asStateFlow()

    init {
        loadQuestions()
        loadTrialGate()
    }

    /** Independent of the question list load — a failed/slow trial check shouldn't block the list from showing. */
    private fun loadTrialGate() {
        viewModelScope.launch {
            val profile = (authRepository.getCurrentProfile() as? AppResult.Success)?.data ?: return@launch
            _uiState.value = _uiState.value.copy(isCreationBlocked = profile.trialStatus() is TrialStatus.Expired)
        }
    }

    /** Gate for both the manual "Add Question" button and the "AI" banner — same rule for both. */
    fun onAddQuestionClick() {
        if (_uiState.value.isCreationBlocked) {
            _uiState.value = _uiState.value.copy(showTrialPaywall = true)
        } else {
            startNewQuestion()
        }
    }

    fun onOpenAiClick(onAllowed: () -> Unit) {
        if (_uiState.value.isCreationBlocked) {
            _uiState.value = _uiState.value.copy(showTrialPaywall = true)
        } else {
            onAllowed()
        }
    }

    fun dismissTrialPaywall() {
        _uiState.value = _uiState.value.copy(showTrialPaywall = false)
    }

    fun loadQuestions() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = questionRepository.getQuestionsForUser(userId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, questions = result.data)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun openFilterSheet() {
        _uiState.value = _uiState.value.copy(isFilterSheetOpen = true)
    }

    fun closeFilterSheet() {
        _uiState.value = _uiState.value.copy(isFilterSheetOpen = false)
    }

    fun applyFilter(tag: String?, difficulty: QuestionDifficulty?) {
        _uiState.value = _uiState.value.copy(tagFilter = tag, difficultyFilter = difficulty, isFilterSheetOpen = false)
    }

    fun toggleSelectionMode() {
        val enteringSelectionMode = !_uiState.value.isSelectionMode
        _uiState.value = _uiState.value.copy(
            isSelectionMode = enteringSelectionMode,
            selectedQuestionIds = if (enteringSelectionMode) _uiState.value.selectedQuestionIds else emptySet()
        )
    }

    fun exitSelectionMode() {
        _uiState.value = _uiState.value.copy(isSelectionMode = false, selectedQuestionIds = emptySet())
    }

    fun toggleQuestionSelection(questionId: String) {
        val current = _uiState.value.selectedQuestionIds
        if (questionId in current) {
            _uiState.value = _uiState.value.copy(selectedQuestionIds = current - questionId)
            return
        }
        if (current.size >= MAX_SELECTABLE_QUESTIONS) {
            _uiState.value = _uiState.value.copy(errorMessage = "You can select up to $MAX_SELECTABLE_QUESTIONS questions.")
            return
        }
        _uiState.value = _uiState.value.copy(selectedQuestionIds = current + questionId)
    }

    fun deleteQuestion(questionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deletingQuestionId = questionId)
            when (val result = questionRepository.deleteQuestion(questionId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    deletingQuestionId = null,
                    questions = _uiState.value.questions.filterNot { it.id == questionId }
                )
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(deletingQuestionId = null, errorMessage = result.message)
            }
        }
    }

    fun startNewQuestion() {
        _uiState.value = _uiState.value.copy(draft = NewQuestionDraft())
    }

    fun startEditQuestion(question: Question) {
        _uiState.value = _uiState.value.copy(draft = question.toDraft())
    }

    fun dismissDraft() {
        _uiState.value = _uiState.value.copy(draft = null)
    }

    fun updateDraft(transform: (NewQuestionDraft) -> NewQuestionDraft) {
        val current = _uiState.value.draft ?: return
        _uiState.value = _uiState.value.copy(draft = transform(current))
    }

    fun saveDraft(keepEditing: Boolean) {
        val draft = _uiState.value.draft ?: return
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

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val editingId = draft.editingQuestionId
            val result = if (editingId != null) {
                questionRepository.updateQuestion(
                    questionId = editingId,
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
            } else {
                questionRepository.createQuestion(
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
            }
            when (result) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        questions = if (editingId != null) {
                            _uiState.value.questions.map { if (it.id == result.data.id) result.data else it }
                        } else {
                            listOf(result.data) + _uiState.value.questions
                        },
                        draft = if (keepEditing) NewQuestionDraft() else null
                    )
                    AlertBus.success(if (editingId != null) "Question updated" else "Question added")
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
            }
        }
    }
}
