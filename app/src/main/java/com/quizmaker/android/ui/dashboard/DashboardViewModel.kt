package com.quizmaker.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.data.model.QuizResponse
import com.quizmaker.android.data.model.SaleDay
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.ProfileRepository
import com.quizmaker.android.repository.QuestionRepository
import com.quizmaker.android.repository.QuizRepository
import com.quizmaker.android.repository.SaleDayRepository
import com.quizmaker.android.util.TrialStatus
import com.quizmaker.android.util.trialStatus
import com.google.firebase.messaging.FirebaseMessaging
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
    val quizTitleById: Map<String, String> = emptyMap(),
    val activeSale: SaleDay? = null,
    val trialStatus: TrialStatus = TrialStatus.Premium
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val quizRepository: QuizRepository,
    private val questionRepository: QuestionRepository,
    private val saleDayRepository: SaleDayRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var allQuizzes: List<Quiz> = emptyList()
    private var allCompletedResponses: List<QuizResponse> = emptyList()
    private var creatorName: String = ""
    private var totalQuestions: Int = 0
    private var activeSale: SaleDay? = null
    private var trialStatus: TrialStatus = TrialStatus.Premium

    init {
        refresh()
        uploadFcmToken()
    }

    /**
     * Fire-and-forget, once per ViewModel lifetime (roughly "once per Dashboard visit/session" —
     * matches hiltViewModel()'s NavBackStackEntry scoping). Also re-run from QuizFcmService's own
     * onNewToken() whenever FCM rotates the token, so this call is a belt-and-braces top-up, not
     * the only path.
     */
    private fun uploadFcmToken() {
        val userId = authRepository.currentUserId() ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            viewModelScope.launch { profileRepository.updateFcmToken(userId, token) }
        }
    }

    fun refresh() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val profileResult = authRepository.getCurrentProfile()
            val quizzesResult = quizRepository.getQuizzesForUser(userId)
            val responsesResult = quizRepository.getResponsesForUser(userId)
            val questionsResult = questionRepository.getQuestionsForUser(userId)
            val saleDaysResult = saleDayRepository.getSaleDays()

            if (quizzesResult is AppResult.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = quizzesResult.message)
                return@launch
            }

            val questions = (questionsResult as? AppResult.Success)?.data.orEmpty()

            allQuizzes = (quizzesResult as AppResult.Success).data
            // A responses-fetch failure (e.g. a slow "all my quiz ids, then all their responses"
            // query timing out) shouldn't blank out quizzes/questions data that already loaded fine —
            // fall back to no responses and surface the error inline instead of hard-failing the page.
            allCompletedResponses = (responsesResult as? AppResult.Success)?.data
                ?.filter { it.completed && !it.cancelled }
                .orEmpty()
            totalQuestions = questions.size
            creatorName = when (profileResult) {
                is AppResult.Success -> profileResult.data.name
                is AppResult.Error -> ""
            }
            trialStatus = (profileResult as? AppResult.Success)?.data?.trialStatus() ?: TrialStatus.Premium

            val now = Clock.System.now()
            activeSale = (saleDaysResult as? AppResult.Success)?.data?.firstOrNull { sale ->
                val start = sale.startedAt
                val end = sale.endAt
                start != null && end != null && now >= start && now <= end
            }

            val partialError = (responsesResult as? AppResult.Error)?.message
            applyRange(_uiState.value.selectedRange, partialErrorMessage = partialError)
        }
    }

    fun onRangeSelected(range: DashboardDateRange) {
        applyRange(range)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyRange(_uiState.value.selectedRange)
    }

    private fun applyRange(range: DashboardDateRange, partialErrorMessage: String? = null) {
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
            errorMessage = partialErrorMessage,
            creatorName = creatorName,
            selectedRange = range,
            totalQuizzes = allQuizzes.size,
            totalQuestions = totalQuestions,
            totalResponses = completed.size,
            averageScorePercent = averageScore,
            recentSubmissions = submissions,
            quizzes = allQuizzes,
            quizTitleById = allQuizzes.associate { it.id to it.title },
            activeSale = activeSale,
            trialStatus = trialStatus
        )
    }
}
