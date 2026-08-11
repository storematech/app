package com.quizmaker.android.ui.classdashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.ClassAiSummary
import com.quizmaker.android.data.model.ClassQuizPerformance
import com.quizmaker.android.data.model.ClassSummary
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.data.model.QuizAiSummary
import com.quizmaker.android.data.model.QuizClass
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.ClassRepository
import com.quizmaker.android.repository.QuizAiSummaryRepository
import com.quizmaker.android.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import javax.inject.Inject

enum class ClassDateRangePreset(val label: String, val days: Int?) {
    LAST_7_DAYS("Last 7 Days", 7),
    LAST_30_DAYS("Last 30 Days", 30),
    ALL_TIME("All Time", null)
}

data class ClassDashboardUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val quizClass: QuizClass? = null,
    val selectedRange: ClassDateRangePreset = ClassDateRangePreset.LAST_7_DAYS,
    val summary: ClassSummary? = null,
    val quizPerformance: List<ClassQuizPerformance> = emptyList(),
    // Class AI Summary — always all-time, see ClassAiSummary's KDoc.
    val classAiSummary: ClassAiSummary? = null,
    val isLoadingClassAiSummary: Boolean = true,
    // Per-quiz AI Summary — identical lazy pattern to QuizListViewModel, reusing the exact same
    // cached quiz_ai_summaries data (see QuizAiSummaryRepository), not recomputed here.
    val aiSummaries: Map<String, QuizAiSummary> = emptyMap(),
    val expandedAiSummaryQuizIds: Set<String> = emptySet(),
    val loadingAiSummaryQuizIds: Set<String> = emptySet(),
    val isLinkSheetOpen: Boolean = false,
    val linkableQuizzes: List<Quiz> = emptyList(),
    val isLoadingLinkableQuizzes: Boolean = false,
    val isLinking: Boolean = false
)

@HiltViewModel
class ClassDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val classRepository: ClassRepository,
    private val quizRepository: QuizRepository,
    private val quizAiSummaryRepository: QuizAiSummaryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val classId: String = checkNotNull(savedStateHandle["classId"])

    private val _uiState = MutableStateFlow(ClassDashboardUiState())
    val uiState: StateFlow<ClassDashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
        loadClassAiSummary()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = classRepository.getClassById(classId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(quizClass = result.data)
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                    return@launch
                }
            }
            loadDashboardData()
        }
    }

    fun onRangeChange(preset: ClassDateRangePreset) {
        _uiState.value = _uiState.value.copy(selectedRange = preset)
        viewModelScope.launch { loadDashboardData() }
    }

    private suspend fun loadDashboardData() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        val since = sinceIso(_uiState.value.selectedRange)
        val summaryResult = classRepository.getClassSummary(classId, since)
        val performanceResult = classRepository.getQuizPerformance(classId, since)
        when {
            summaryResult is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = summaryResult.message)
            performanceResult is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = performanceResult.message)
            summaryResult is AppResult.Success && performanceResult is AppResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    summary = summaryResult.data,
                    quizPerformance = performanceResult.data
                )
            }
        }
    }

    private fun sinceIso(preset: ClassDateRangePreset): String? {
        val days = preset.days ?: return null
        return (Clock.System.now() - days.days).toString()
    }

    private fun loadClassAiSummary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingClassAiSummary = true)
            val result = classRepository.getClassAiSummary(classId)
            _uiState.value = _uiState.value.copy(
                isLoadingClassAiSummary = false,
                classAiSummary = (result as? AppResult.Success)?.data
            )
        }
    }

    /** Same shape as QuizListViewModel.onToggleAiSummary — fetches from the shared quiz_ai_summaries cache. */
    fun onToggleQuizAiSummary(quizId: String) {
        val expanded = _uiState.value.expandedAiSummaryQuizIds
        val nowExpanding = quizId !in expanded
        _uiState.value = _uiState.value.copy(
            expandedAiSummaryQuizIds = if (nowExpanding) expanded + quizId else expanded - quizId
        )
        if (nowExpanding && quizId !in _uiState.value.aiSummaries) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(loadingAiSummaryQuizIds = _uiState.value.loadingAiSummaryQuizIds + quizId)
                val result = quizAiSummaryRepository.getSummary(quizId)
                val summaries = if (result is AppResult.Success) _uiState.value.aiSummaries + (quizId to result.data) else _uiState.value.aiSummaries
                _uiState.value = _uiState.value.copy(
                    aiSummaries = summaries,
                    loadingAiSummaryQuizIds = _uiState.value.loadingAiSummaryQuizIds - quizId
                )
            }
        }
    }

    fun openLinkSheet() {
        _uiState.value = _uiState.value.copy(isLinkSheetOpen = true)
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingLinkableQuizzes = true)
            val allQuizzes = (quizRepository.getQuizzesForUser(userId) as? AppResult.Success)?.data.orEmpty()
            val linkedIds = (classRepository.getLinkedQuizIds(classId) as? AppResult.Success)?.data.orEmpty().toSet()
            _uiState.value = _uiState.value.copy(
                isLoadingLinkableQuizzes = false,
                linkableQuizzes = allQuizzes.filter { it.id !in linkedIds }
            )
        }
    }

    fun closeLinkSheet() {
        _uiState.value = _uiState.value.copy(isLinkSheetOpen = false)
    }

    fun linkQuiz(quizId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLinking = true)
            when (classRepository.linkQuiz(classId, quizId)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLinking = false,
                        isLinkSheetOpen = false,
                        linkableQuizzes = _uiState.value.linkableQuizzes.filterNot { it.id == quizId }
                    )
                    loadDashboardData()
                    loadClassAiSummary()
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLinking = false)
            }
        }
    }
}
