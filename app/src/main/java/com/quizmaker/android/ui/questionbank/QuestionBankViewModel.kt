package com.quizmaker.android.ui.questionbank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionDifficulty
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewQuestionDraft(
    val text: String = "",
    val type: QuestionType = QuestionType.SINGLE_CHOICE,
    val options: List<String> = listOf("", "", "", ""),
    val correctOptionIndex: Int = 0,
    val correctOptionIndices: Set<Int> = setOf(0),
    val freeTextAnswer: String = "",
    val points: Int = 1,
    val difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
    // Carried through from the source question when editing (no UI for these yet), so saving
    // an edit doesn't silently wipe them. Null id means "creating a new question".
    val editingQuestionId: String? = null,
    val tags: List<String> = emptyList(),
    val explanation: String? = null
)

data class QuestionBankUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val questions: List<Question> = emptyList(),
    val searchQuery: String = "",
    val selectedQuestion: Question? = null,
    val draft: NewQuestionDraft? = null,
    val isSaving: Boolean = false
) {
    val filtered: List<Question>
        get() = if (searchQuery.isBlank()) questions else questions.filter {
            it.text.contains(searchQuery, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
        }
}

@HiltViewModel
class QuestionBankViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionBankUiState())
    val uiState: StateFlow<QuestionBankUiState> = _uiState.asStateFlow()

    init {
        loadQuestions()
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

    fun selectQuestion(question: Question?) {
        _uiState.value = _uiState.value.copy(selectedQuestion = question)
    }

    fun deleteQuestion(questionId: String) {
        viewModelScope.launch {
            questionRepository.deleteQuestion(questionId)
            _uiState.value = _uiState.value.copy(
                selectedQuestion = null,
                questions = _uiState.value.questions.filterNot { it.id == questionId }
            )
        }
    }

    fun startNewQuestion() {
        _uiState.value = _uiState.value.copy(draft = NewQuestionDraft())
    }

    fun startEditQuestion(question: Question) {
        val isChoice = question.type == QuestionType.SINGLE_CHOICE || question.type == QuestionType.MULTI_CHOICE
        val optionTexts = if (isChoice && question.options.isNotEmpty()) {
            question.options.map { it.text }
        } else {
            listOf("", "", "", "")
        }
        val correctIndex = question.options.indexOfFirst { it.isCorrect }.takeIf { it >= 0 } ?: 0
        val correctIndices = question.options.withIndex()
            .filter { (_, option) -> option.isCorrect }
            .map { (index, _) -> index }
            .toSet()
            .ifEmpty { setOf(0) }

        _uiState.value = _uiState.value.copy(
            draft = NewQuestionDraft(
                text = question.text,
                type = question.type,
                options = optionTexts,
                correctOptionIndex = correctIndex,
                correctOptionIndices = correctIndices,
                freeTextAnswer = question.correctAnswer.orEmpty(),
                points = question.points,
                difficulty = question.difficulty ?: QuestionDifficulty.MEDIUM,
                editingQuestionId = question.id,
                tags = question.tags,
                explanation = question.explanation
            )
        )
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
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    questions = if (editingId != null) {
                        _uiState.value.questions.map { if (it.id == result.data.id) result.data else it }
                    } else {
                        listOf(result.data) + _uiState.value.questions
                    },
                    draft = if (keepEditing) NewQuestionDraft() else null
                )
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
            }
        }
    }
}
