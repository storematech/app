package com.quizmaker.android.ui.leaderboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.LeaderboardData
import com.quizmaker.android.repository.LeaderboardRepository
import com.quizmaker.android.util.PdfBranding
import com.quizmaker.android.util.PdfBrandingProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val data: LeaderboardData? = null
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: LeaderboardRepository,
    private val pdfBrandingProvider: PdfBrandingProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: String = checkNotNull(savedStateHandle["quizId"])

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.getLeaderboard(quizId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, data = result.data)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    suspend fun getPdfBranding(): PdfBranding = pdfBrandingProvider.get()
}
