package com.quizmaker.android.ui.revision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.alert.AlertBus
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.NewQuestionDraft
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.data.model.RevisionItem
import com.quizmaker.android.data.model.RevisionStatus
import com.quizmaker.android.data.model.toDraft
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.QuestionRepository
import com.quizmaker.android.repository.RevisionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RevisionUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val items: List<RevisionItem> = emptyList(),
    val searchQuery: String = "",
    val tagFilter: String? = null,
    val quizFilter: String? = null,
    val isFilterSheetOpen: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val isDeleting: Boolean = false,
    val isMarkingDone: Boolean = false,
    // Editing a bookmarked question (tap a card outside selection mode) — reuses the same
    // shared sheet Question Bank uses, see ui/common/QuestionEditSheet.kt.
    val draft: NewQuestionDraft? = null,
    val isSavingDraft: Boolean = false
) {
    val filtered: List<RevisionItem>
        get() = items.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.question.text.contains(searchQuery, ignoreCase = true) ||
                item.question.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
            val matchesTag = tagFilter == null || tagFilter in item.question.tags
            val matchesQuiz = quizFilter == null || item.quizTitle == quizFilter
            matchesSearch && matchesTag && matchesQuiz
        }

    val availableTags: List<String> get() = items.flatMap { it.question.tags }.distinct().sorted()
    val availableQuizzes: List<String> get() = items.mapNotNull { it.quizTitle }.distinct().sorted()
}

/** Powers the "Revision" screen — bookmarked questions (see RevisionRepository), search/filter/bulk-manage them, and build a new quiz from a selection. */
@HiltViewModel
class RevisionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val revisionRepository: RevisionRepository,
    private val questionRepository: QuestionRepository
) : ViewModel() {

    companion object {
        const val MAX_SELECTABLE = 25
    }

    private val _uiState = MutableStateFlow(RevisionUiState())
    val uiState: StateFlow<RevisionUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = revisionRepository.getRevisionItems(userId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, items = result.data)
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

    fun applyFilter(tag: String?, quiz: String?) {
        _uiState.value = _uiState.value.copy(tagFilter = tag, quizFilter = quiz, isFilterSheetOpen = false)
    }

    fun toggleSelectionMode() {
        val enteringSelectionMode = !_uiState.value.isSelectionMode
        _uiState.value = _uiState.value.copy(
            isSelectionMode = enteringSelectionMode,
            selectedIds = if (enteringSelectionMode) _uiState.value.selectedIds else emptySet()
        )
    }

    fun exitSelectionMode() {
        _uiState.value = _uiState.value.copy(isSelectionMode = false, selectedIds = emptySet())
    }

    fun toggleSelection(id: String) {
        val current = _uiState.value.selectedIds
        if (id in current) {
            _uiState.value = _uiState.value.copy(selectedIds = current - id)
            return
        }
        if (current.size >= MAX_SELECTABLE) {
            _uiState.value = _uiState.value.copy(errorMessage = "You can select up to $MAX_SELECTABLE items.")
            return
        }
        _uiState.value = _uiState.value.copy(isSelectionMode = true, selectedIds = current + id)
    }

    /** Selects the first [MAX_SELECTABLE] of the current filter view, replacing any existing selection. */
    fun selectAll() {
        val ids = _uiState.value.filtered.take(MAX_SELECTABLE).map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(isSelectionMode = true, selectedIds = ids)
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            when (revisionRepository.deleteItems(ids)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        items = _uiState.value.items.filterNot { it.id in ids },
                        isSelectionMode = false,
                        selectedIds = emptySet()
                    )
                    AlertBus.success("Removed from revision")
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isDeleting = false)
            }
        }
    }

    fun markSelectedDone() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMarkingDone = true)
            when (revisionRepository.updateStatus(ids, RevisionStatus.DONE)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isMarkingDone = false,
                        items = _uiState.value.items.map { if (it.id in ids) it.copy(status = RevisionStatus.DONE) else it },
                        isSelectionMode = false,
                        selectedIds = emptySet()
                    )
                    AlertBus.success("Marked as done", actionLabel = "UNDO") { revertToPending(ids) }
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isMarkingDone = false)
            }
        }
    }

    private fun revertToPending(ids: List<String>) {
        viewModelScope.launch {
            if (revisionRepository.updateStatus(ids, RevisionStatus.PENDING) is AppResult.Success) {
                _uiState.value = _uiState.value.copy(
                    items = _uiState.value.items.map { if (it.id in ids) it.copy(status = RevisionStatus.PENDING) else it }
                )
            }
        }
    }

    /** Selected items' question ids, ready for Screen.CreateQuiz.createRoute(...) — clears selection mode on the way out. */
    fun createRetest(): List<String> {
        val ids = _uiState.value.selectedIds
        val questionIds = _uiState.value.items.filter { it.id in ids }.map { it.question.id }
        exitSelectionMode()
        return questionIds
    }

    /** Tapping a card outside selection mode — opens the same edit sheet Question Bank uses. */
    fun startEditQuestion(question: Question) {
        _uiState.value = _uiState.value.copy(draft = question.toDraft())
    }

    fun updateDraft(transform: (NewQuestionDraft) -> NewQuestionDraft) {
        val current = _uiState.value.draft ?: return
        _uiState.value = _uiState.value.copy(draft = transform(current))
    }

    fun dismissDraft() {
        _uiState.value = _uiState.value.copy(draft = null)
    }

    /** Edit-only (Revision never creates new questions) — mirrors QuestionBankViewModel.saveDraft's update path. */
    fun saveDraft() {
        val draft = _uiState.value.draft ?: return
        val editingId = draft.editingQuestionId ?: return
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
            _uiState.value = _uiState.value.copy(isSavingDraft = true, errorMessage = null)
            val result = questionRepository.updateQuestion(
                questionId = editingId,
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
                isUngraded = draft.type == QuestionType.FREE_TEXT
            )
            when (result) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSavingDraft = false,
                        draft = null,
                        items = _uiState.value.items.map { if (it.question.id == result.data.id) it.copy(question = result.data) else it }
                    )
                    AlertBus.success("Question updated")
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSavingDraft = false, errorMessage = result.message)
            }
        }
    }
}
