package com.quizmaker.android.ui.classweaklearners

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.WeakLearner
import com.quizmaker.android.repository.ClassRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClassWeakLearnersUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val learners: List<WeakLearner> = emptyList()
)

/** Lowest-score-first ranking across every quiz in a class — a "who needs help" glance, capped at 50 rows. */
@HiltViewModel
class ClassWeakLearnersViewModel @Inject constructor(
    private val classRepository: ClassRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val classId: String = checkNotNull(savedStateHandle["classId"])

    private val _uiState = MutableStateFlow(ClassWeakLearnersUiState())
    val uiState: StateFlow<ClassWeakLearnersUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = classRepository.getWeakLearners(classId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, learners = result.data)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }
}
