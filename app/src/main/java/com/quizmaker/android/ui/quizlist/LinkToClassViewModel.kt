package com.quizmaker.android.ui.quizlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.QuizClass
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.ClassRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LinkToClassUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val classes: List<QuizClass> = emptyList(),
    val linkedClassIds: Set<String> = emptySet(),
    val isCreateDialogOpen: Boolean = false,
    val isCreating: Boolean = false
)

/** Powers the "Link to Class" bottom sheet opened from a quiz's action sheet — a lightweight,
 *  screen-scoped ViewModel rather than growing QuizListViewModel with a separate concern. */
@HiltViewModel
class LinkToClassViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val classRepository: ClassRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LinkToClassUiState())
    val uiState: StateFlow<LinkToClassUiState> = _uiState.asStateFlow()

    private var currentQuizId: String? = null

    fun load(quizId: String) {
        currentQuizId = quizId
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val classesResult = classRepository.getClasses(userId)
            val linkedResult = classRepository.getClassIdsForQuiz(quizId)
            when {
                classesResult is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = classesResult.message)
                linkedResult is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = linkedResult.message)
                classesResult is AppResult.Success && linkedResult is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        classes = classesResult.data,
                        linkedClassIds = linkedResult.data.toSet()
                    )
                }
            }
        }
    }

    /** Tap toggles link/unlink for the quiz passed to [load]. */
    fun toggleClass(classId: String) {
        val quizId = currentQuizId ?: return
        val isLinked = classId in _uiState.value.linkedClassIds
        _uiState.value = _uiState.value.copy(
            linkedClassIds = if (isLinked) _uiState.value.linkedClassIds - classId else _uiState.value.linkedClassIds + classId
        )
        viewModelScope.launch {
            if (isLinked) classRepository.unlinkQuiz(classId, quizId) else classRepository.linkQuiz(classId, quizId)
        }
    }

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(isCreateDialogOpen = true)
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(isCreateDialogOpen = false)
    }

    /** Creates the class, then immediately links the current quiz to it. */
    fun createAndLinkClass(name: String) {
        val userId = authRepository.currentUserId() ?: return
        val quizId = currentQuizId ?: return
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter a class name.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, errorMessage = null)
            when (val result = classRepository.createClass(userId, name.trim(), null)) {
                is AppResult.Success -> {
                    classRepository.linkQuiz(result.data.id, quizId)
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        isCreateDialogOpen = false,
                        classes = listOf(result.data) + _uiState.value.classes,
                        linkedClassIds = _uiState.value.linkedClassIds + result.data.id
                    )
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isCreating = false, errorMessage = result.message)
            }
        }
    }
}
