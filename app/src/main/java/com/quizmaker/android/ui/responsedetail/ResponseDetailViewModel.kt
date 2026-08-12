package com.quizmaker.android.ui.responsedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.ResponseDetailData
import com.quizmaker.android.repository.ResponseDetailRepository
import com.quizmaker.android.util.PdfBranding
import com.quizmaker.android.util.PdfBrandingProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResponseDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val data: ResponseDetailData? = null
)

@HiltViewModel
class ResponseDetailViewModel @Inject constructor(
    private val repository: ResponseDetailRepository,
    private val pdfBrandingProvider: PdfBrandingProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val responseId: String = checkNotNull(savedStateHandle["responseId"])

    private val _uiState = MutableStateFlow(ResponseDetailUiState())
    val uiState: StateFlow<ResponseDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.getResponseDetail(responseId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, data = result.data)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    suspend fun getPdfBranding(): PdfBranding = pdfBrandingProvider.get()
}
