package com.quizmaker.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.data.model.QuizResponse
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.QuestionRepository
import com.quizmaker.android.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

enum class DashboardDateRange(val label: String, val days: Int?) {
    LAST_7_DAYS("Last 7 Days", 7),
    LAST_30_DAYS("Last 30 Days", 30),
    LAST_90_DAYS("Last 90 Days", 90),
    ALL_TIME("All Time", null)
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val creatorName: String = "",
    val selectedRange: DashboardDateRange = DashboardDateRange.LAST_7_DAYS,
    val totalQuizzes: Int = 0,
    val totalQuestions: Int = 0,
    val totalResponses: Int = 0,
    val averageScorePercent: Int = 0,
    val searchQuery: String = "",
    val recentSubmissions: List<QuizResponse> = emptyList(),
    val quizzes: List<Quiz> = emptyList(),
    val quizTitleById: Map<String, String> = emptyMap()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val quizRepository: QuizRepository,
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var allQuizzes: List<Quiz> = emptyList()
    private var allCompletedResponses: List<QuizResponse> = emptyList()
    private var creatorName: String = ""
    private var totalQuestions: Int = 0

    init {
        refresh()
    }

    fun refresh() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val profileResult = authRepository.getCurrentProfile()
            val quizzesResult = quizRepository.getQuizzesForUser(userId)
            val responsesResult = quizRepository.getResponsesForUser(userId)
            val questionsResult = questionRepository.getQuestionsForUser(userId)

            if (quizzesResult is AppResult.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = quizzesResult.message)
                return@launch
            }
            if (responsesResult is AppResult.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = responsesResult.message)
                return@launch
            }

            val questions = (questionsResult as? AppResult.Success)?.data.orEmpty()

            allQuizzes = (quizzesResult as AppResult.Success).data
            allCompletedResponses = (responsesResult as AppResult.Success).data.filter { it.completed && !it.cancelled }
            totalQuestions = questions.size
            creatorName = when (profileResult) {
                is AppResult.Success -> profileResult.data.name
                is AppResult.Error -> ""
            }

            applyRange(_uiState.value.selectedRange)
        }
    }

    fun onRangeSelected(range: DashboardDateRange) {
        applyRange(range)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyRange(_uiState.value.selectedRange)
    }

    private fun applyRange(range: DashboardDateRange) {
        val cutoff: Instant? = range.days?.let { Clock.System.now() - it.days }
        val completed = if (cutoff == null) {
            allCompletedResponses
        } else {
            allCompletedResponses.filter { r -> r.completedAt != null && r.completedAt >= cutoff }
        }

        // quiz_responses.score is already a 0-100 percentage (matching the web app's convention),
        // so this is a plain average — no maxPoints conversion needed.
        val averageScore = if (completed.isNotEmpty()) completed.map { it.score }.average().roundToInt() else 0

        val query = _uiState.value.searchQuery.trim()
        val submissions = if (query.isBlank()) {
            completed.sortedByDescending { it.completedAt }.take(8)
        } else {
            completed
                .filter {
                    it.userEmail.contains(query, ignoreCase = true) ||
                        it.userName?.contains(query, ignoreCase = true) == true
                }
                .sortedByDescending { it.completedAt }
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = null,
            creatorName = creatorName,
            selectedRange = range,
            totalQuizzes = allQuizzes.size,
            totalQuestions = totalQuestions,
            totalResponses = completed.size,
            averageScorePercent = averageScore,
            recentSubmissions = submissions,
            quizzes = allQuizzes,
            quizTitleById = allQuizzes.associate { it.id to it.title }
        )
    }
}
