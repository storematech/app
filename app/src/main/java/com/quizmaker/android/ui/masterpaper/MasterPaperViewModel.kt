package com.quizmaker.android.ui.masterpaper

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.OmrLayout
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.OmrRepository
import com.quizmaker.android.repository.QuizRepository
import com.quizmaker.android.util.PdfBranding
import com.quizmaker.android.util.PdfBrandingProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MasterPaperUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val quizId: String = "",
    val quizTitle: String = "",
    val questions: List<Question> = emptyList(),
    val isSavingOmrLayout: Boolean = false,
    // Deliberately separate from [errorMessage] -- that field gates the whole screen into an error
    // view (see MasterPaperScreen's top-level `when`); a failed OMR-sheet save shouldn't blow away
    // an otherwise successfully loaded question list, just surface inline near its own button.
    val omrErrorMessage: String? = null
)

@HiltViewModel
class MasterPaperViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val pdfBrandingProvider: PdfBrandingProvider,
    private val omrRepository: OmrRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: String = checkNotNull(savedStateHandle["quizId"])

    private val _uiState = MutableStateFlow(MasterPaperUiState(quizId = quizId))
    val uiState: StateFlow<MasterPaperUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val quizResult = quizRepository.getQuizById(quizId)
            val questionsResult = quizRepository.getQuestionsForQuiz(quizId)
            when {
                quizResult is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoading = false, errorMessage = quizResult.message)
                questionsResult is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoading = false, errorMessage = questionsResult.message)
                quizResult is AppResult.Success && questionsResult is AppResult.Success -> _uiState.value =
                    _uiState.value.copy(isLoading = false, quizTitle = quizResult.data.title, questions = questionsResult.data)
            }
        }
    }

    suspend fun getPdfBranding(): PdfBranding = pdfBrandingProvider.get()

    /** Persists the [layout] built while rendering the OMR sheet PDF, same convention as
     *  OmrScanViewModel.onSheetExported -- see OmrRepository.saveLayout. Current-user-id pattern
     *  mirrors CertificateDesignerViewModel (authRepository.currentUserId()). */
    fun saveOmrLayout(layout: OmrLayout) {
        val userId = authRepository.currentUserId() ?: run {
            _uiState.value = _uiState.value.copy(omrErrorMessage = "You need to be signed in to generate an answer sheet.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingOmrLayout = true, omrErrorMessage = null)
            when (val result = omrRepository.saveLayout(quizId, userId, layout)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isSavingOmrLayout = false)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSavingOmrLayout = false, omrErrorMessage = result.message)
            }
        }
    }
}
