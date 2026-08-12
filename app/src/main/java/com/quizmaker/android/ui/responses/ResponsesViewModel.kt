package com.quizmaker.android.ui.responses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.data.model.QuizResponse
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.QuizRepository
import com.quizmaker.android.util.PdfBranding
import com.quizmaker.android.util.PdfBrandingProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

enum class ResponseTimeFrame(val label: String, val days: Int?) {
    LAST_7_DAYS("Last 7 Days", 7),
    LAST_30_DAYS("Last 30 Days", 30),
    LAST_3_MONTHS("Last 3 Months", 90),
    ALL_TIME("All Time", null)
}

enum class ResponseStatusFilter(val label: String) {
    ALL("All Statuses"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    IN_PROGRESS("In Progress")
}

data class ResponsesUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val quizzes: List<Quiz> = emptyList(),
    val selectedQuizId: String = "all",
    val searchEmail: String = "",
    val statusFilter: ResponseStatusFilter = ResponseStatusFilter.ALL,
    val timeFrame: ResponseTimeFrame = ResponseTimeFrame.LAST_7_DAYS,
    val filteredResponses: List<QuizResponse> = emptyList(),
    val quizTitleById: Map<String, String> = emptyMap()
)

@HiltViewModel
class ResponsesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val quizRepository: QuizRepository,
    private val pdfBrandingProvider: PdfBrandingProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResponsesUiState())
    val uiState: StateFlow<ResponsesUiState> = _uiState.asStateFlow()

    private var allResponses: List<QuizResponse> = emptyList()

    init {
        refresh()
    }

    fun refresh() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val quizzesResult = quizRepository.getQuizzesForUser(userId)
            val responsesResult = quizRepository.getResponsesForUser(userId)

            if (quizzesResult is AppResult.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = quizzesResult.message)
                return@launch
            }
            if (responsesResult is AppResult.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = responsesResult.message)
                return@launch
            }

            val quizzes = (quizzesResult as AppResult.Success).data
            allResponses = (responsesResult as AppResult.Success).data

            _uiState.value = _uiState.value.copy(
                quizzes = quizzes,
                quizTitleById = quizzes.associate { it.id to it.title }
            )
            applyFilters()
        }
    }

    fun onQuizSelected(quizId: String) {
        _uiState.value = _uiState.value.copy(selectedQuizId = quizId)
        applyFilters()
    }

    fun onSearchEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(searchEmail = value)
        applyFilters()
    }

    fun onStatusSelected(status: ResponseStatusFilter) {
        _uiState.value = _uiState.value.copy(statusFilter = status)
        applyFilters()
    }

    fun onTimeFrameSelected(timeFrame: ResponseTimeFrame) {
        _uiState.value = _uiState.value.copy(timeFrame = timeFrame)
        applyFilters()
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            selectedQuizId = "all",
            searchEmail = "",
            statusFilter = ResponseStatusFilter.ALL,
            timeFrame = ResponseTimeFrame.LAST_7_DAYS
        )
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val cutoff: Instant? = state.timeFrame.days?.let { Clock.System.now() - it.days }

        val filtered = allResponses.filter { response ->
            val matchesQuiz = state.selectedQuizId == "all" || response.quizId == state.selectedQuizId
            val matchesEmail = state.searchEmail.isBlank() ||
                response.userEmail.contains(state.searchEmail, ignoreCase = true) ||
                response.userName?.contains(state.searchEmail, ignoreCase = true) == true
            val matchesStatus = when (state.statusFilter) {
                ResponseStatusFilter.ALL -> true
                ResponseStatusFilter.COMPLETED -> response.completed
                ResponseStatusFilter.CANCELLED -> response.cancelled
                ResponseStatusFilter.IN_PROGRESS -> !response.completed && !response.cancelled
            }
            val matchesTime = cutoff == null || (response.startedAt != null && response.startedAt >= cutoff)
            matchesQuiz && matchesEmail && matchesStatus && matchesTime
        }.sortedByDescending { it.startedAt }

        _uiState.value = state.copy(isLoading = false, errorMessage = null, filteredResponses = filtered)
    }

    suspend fun getPdfBranding(): PdfBranding = pdfBrandingProvider.get()
}
