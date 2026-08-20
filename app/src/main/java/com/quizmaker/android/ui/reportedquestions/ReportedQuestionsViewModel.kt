package com.quizmaker.android.ui.reportedquestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.ReportedQuestion
import com.quizmaker.android.data.model.ReportedQuestionStatus
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.ReportedQuestionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReportedQuestionStatusFilter(val label: String) {
    PENDING("Pending"),
    RESOLVED("Resolved"),
    ALL("All Statuses")
}

data class ReportedQuestionsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val statusFilter: ReportedQuestionStatusFilter = ReportedQuestionStatusFilter.PENDING,
    val filteredReports: List<ReportedQuestion> = emptyList(),
    /** Report ids with a status update in flight — disables that card's button, doesn't block the rest of the list. */
    val updatingIds: Set<String> = emptySet()
)

@HiltViewModel
class ReportedQuestionsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val reportedQuestionsRepository: ReportedQuestionsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportedQuestionsUiState())
    val uiState: StateFlow<ReportedQuestionsUiState> = _uiState.asStateFlow()

    private var allReports: List<ReportedQuestion> = emptyList()

    init {
        refresh()
    }

    fun refresh() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = reportedQuestionsRepository.getReportedQuestions(userId)) {
                is AppResult.Success -> {
                    allReports = result.data
                    applyFilter()
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun onStatusFilterSelected(filter: ReportedQuestionStatusFilter) {
        _uiState.value = _uiState.value.copy(statusFilter = filter)
        applyFilter()
    }

    /** Flips a report between pending/resolved. Optimistic on success (the row is already known,
     *  no need to re-fetch); reverts to the pre-tap list and surfaces the error otherwise. */
    fun toggleStatus(report: ReportedQuestion) {
        val newStatus = if (report.status == ReportedQuestionStatus.PENDING) {
            ReportedQuestionStatus.RESOLVED
        } else {
            ReportedQuestionStatus.PENDING
        }
        _uiState.value = _uiState.value.copy(updatingIds = _uiState.value.updatingIds + report.id)
        viewModelScope.launch {
            when (val result = reportedQuestionsRepository.updateStatus(report.id, newStatus)) {
                is AppResult.Success -> {
                    allReports = allReports.map { if (it.id == report.id) it.copy(status = newStatus) else it }
                    _uiState.value = _uiState.value.copy(updatingIds = _uiState.value.updatingIds - report.id)
                    applyFilter()
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(
                    updatingIds = _uiState.value.updatingIds - report.id,
                    errorMessage = result.message
                )
            }
        }
    }

    private fun applyFilter() {
        val state = _uiState.value
        val filtered = allReports.filter { report ->
            when (state.statusFilter) {
                ReportedQuestionStatusFilter.ALL -> true
                ReportedQuestionStatusFilter.PENDING -> report.status == ReportedQuestionStatus.PENDING
                ReportedQuestionStatusFilter.RESOLVED -> report.status == ReportedQuestionStatus.RESOLVED
            }
        }.sortedByDescending { it.reportDate }
        _uiState.value = state.copy(isLoading = false, errorMessage = null, filteredReports = filtered)
    }
}
