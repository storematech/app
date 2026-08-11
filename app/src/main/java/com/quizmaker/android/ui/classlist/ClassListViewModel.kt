package com.quizmaker.android.ui.classlist

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

data class ClassListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val classes: List<QuizClass> = emptyList(),
    val isCreateDialogOpen: Boolean = false,
    val isCreating: Boolean = false
)

/** Powers the Class List screen — More → Classes. Create Class lives here, same shape as Quiz List's Create Quiz button. */
@HiltViewModel
class ClassListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val classRepository: ClassRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClassListUiState())
    val uiState: StateFlow<ClassListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = classRepository.getClasses(userId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, classes = result.data)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(isCreateDialogOpen = true)
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(isCreateDialogOpen = false)
    }

    fun createClass(name: String, description: String) {
        val userId = authRepository.currentUserId() ?: return
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter a class name.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, errorMessage = null)
            when (val result = classRepository.createClass(userId, name.trim(), description.trim().ifBlank { null })) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    isCreateDialogOpen = false,
                    classes = listOf(result.data) + _uiState.value.classes
                )
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isCreating = false, errorMessage = result.message)
            }
        }
    }
}
