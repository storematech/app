package com.quizmaker.android.ui.manualmarking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.MarkingItem
import com.quizmaker.android.repository.ManualMarkingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManualMarkingUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val items: List<MarkingItem> = emptyList(),
    /** Non-null only while that specific item's mark is being saved — used to show a per-card
     *  spinner without blocking marking a different item at the same time. */
    val savingItemId: String? = null
) {
    val pendingCount: Int get() = items.count { it.isPending }
}

@HiltViewModel
class ManualMarkingViewModel @Inject constructor(
    private val manualMarkingRepository: ManualMarkingRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: String = checkNotNull(savedStateHandle["quizId"])

    private val _uiState = MutableStateFlow(ManualMarkingUiState())
    val uiState: StateFlow<ManualMarkingUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = manualMarkingRepository.getMarkingItems(quizId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, items = result.data)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun submitMarking(item: MarkingItem, pointsEarned: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(savingItemId = item.answerDetailId, errorMessage = null)
            val result = manualMarkingRepository.submitMarking(
                quizId = quizId,
                responseId = item.responseId,
                answerDetailId = item.answerDetailId,
                pointsEarned = pointsEarned,
                questionMaxPoints = item.maxPoints
            )
            _uiState.value = when (result) {
                is AppResult.Success -> _uiState.value.copy(
                    savingItemId = null,
                    items = _uiState.value.items.map { existing ->
                        if (existing.answerDetailId == item.answerDetailId) {
                            existing.copy(pointsEarned = pointsEarned, isPending = false)
                        } else {
                            existing
                        }
                    }
                )
                is AppResult.Error -> _uiState.value.copy(savingItemId = null, errorMessage = result.message)
            }
        }
    }
}
