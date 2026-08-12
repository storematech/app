package com.quizmaker.android.ui.learners

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.alert.AlertBus
import com.quizmaker.android.core.analytics.AnalyticsLogger
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Group
import com.quizmaker.android.data.model.Learner
import com.quizmaker.android.data.model.LearnerQuizAttempt
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.LearnersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

const val LEARNERS_PER_PAGE = 10

data class LearnersUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val learners: List<Learner> = emptyList(),
    val groups: List<Group> = emptyList(),
    val searchQuery: String = "",
    val groupFilter: String = "all", // "all" | "no-group" | a group id
    val currentPage: Int = 1,

    // Learner create/edit form — null [editingLearner] with the form open means "create".
    val isLearnerFormOpen: Boolean = false,
    val editingLearner: Learner? = null,
    val isSavingLearner: Boolean = false,

    val isCreateGroupDialogOpen: Boolean = false,
    val isSavingGroup: Boolean = false,

    val learnerToDelete: Learner? = null,
    val isDeletingLearner: Boolean = false,

    val profileLearner: Learner? = null,
    val isStudentProfileOpen: Boolean = false,
    val profileAttempts: List<LearnerQuizAttempt> = emptyList(),
    val isLoadingProfileAttempts: Boolean = false
) {
    val filteredLearners: List<Learner>
        get() = learners.filter { learner ->
            val matchesSearch = searchQuery.isBlank() ||
                learner.name.contains(searchQuery, ignoreCase = true) ||
                learner.studentId.contains(searchQuery, ignoreCase = true) ||
                (learner.email?.contains(searchQuery, ignoreCase = true) == true)
            val matchesGroup = when (groupFilter) {
                "all" -> true
                "no-group" -> learner.groupId == null
                else -> learner.groupId == groupFilter
            }
            matchesSearch && matchesGroup
        }

    val totalPages: Int
        get() = ((filteredLearners.size + LEARNERS_PER_PAGE - 1) / LEARNERS_PER_PAGE).coerceAtLeast(1)

    val paginatedLearners: List<Learner>
        get() {
            val start = (currentPage - 1) * LEARNERS_PER_PAGE
            return filteredLearners.drop(start).take(LEARNERS_PER_PAGE)
        }
}

/** Powers the Learners screen — More → Learners. Mirrors the web app's Learners.tsx/learnersService.ts, minus Mini LMS course assignment and the learner-login-portal flow. */
@HiltViewModel
class LearnersViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val learnersRepository: LearnersRepository,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnersUiState())
    val uiState: StateFlow<LearnersUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val learnersResult = learnersRepository.getLearners(userId)
            val groupsResult = learnersRepository.getGroups(userId)
            val learners = (learnersResult as? AppResult.Success)?.data
            val groups = (groupsResult as? AppResult.Success)?.data
            val error = (learnersResult as? AppResult.Error)?.message ?: (groupsResult as? AppResult.Error)?.message
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                learners = learners ?: _uiState.value.learners,
                groups = groups ?: _uiState.value.groups,
                errorMessage = error
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, currentPage = 1)
    }

    fun onGroupFilterChange(filter: String) {
        _uiState.value = _uiState.value.copy(groupFilter = filter, currentPage = 1)
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(searchQuery = "", groupFilter = "all", currentPage = 1)
    }

    fun setPage(page: Int) {
        if (page in 1.._uiState.value.totalPages) {
            _uiState.value = _uiState.value.copy(currentPage = page)
        }
    }

    fun openCreateLearnerDialog() {
        _uiState.value = _uiState.value.copy(isLearnerFormOpen = true, editingLearner = null)
    }

    fun openEditLearnerDialog(learner: Learner) {
        _uiState.value = _uiState.value.copy(isLearnerFormOpen = true, editingLearner = learner)
    }

    fun dismissLearnerForm() {
        _uiState.value = _uiState.value.copy(isLearnerFormOpen = false, editingLearner = null, isSavingLearner = false)
    }

    fun saveLearner(
        name: String,
        email: String,
        groupId: String?,
        parentName: String,
        parentContact: String,
        notes: String
    ) {
        val userId = authRepository.currentUserId() ?: return
        if (name.isBlank() || !email.contains("@")) {
            viewModelScope.launch {
                // AlertBus rather than uiState.errorMessage — the form is a bottom sheet, and the
                // ErrorBanner that renders uiState.errorMessage lives on the screen underneath it.
                AlertBus.error(if (name.isBlank()) "Name is required." else "Enter a valid email.")
            }
            return
        }

        val editing = _uiState.value.editingLearner
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingLearner = true, errorMessage = null)
            val result = if (editing != null) {
                learnersRepository.updateLearner(
                    learnerId = editing.id,
                    name = name.trim(),
                    email = email.trim(),
                    groupId = groupId,
                    parentName = parentName.trim().ifBlank { null },
                    parentContact = parentContact.trim().ifBlank { null },
                    notes = notes.trim().ifBlank { null }
                )
            } else {
                learnersRepository.createLearner(
                    userId = userId,
                    name = name.trim(),
                    email = email.trim(),
                    groupId = groupId,
                    parentName = parentName.trim().ifBlank { null },
                    parentContact = parentContact.trim().ifBlank { null },
                    notes = notes.trim().ifBlank { null }
                )
            }
            when (result) {
                is AppResult.Success -> {
                    if (editing == null) analyticsLogger.logLearnerCreated()
                    _uiState.value = _uiState.value.copy(isSavingLearner = false, isLearnerFormOpen = false, editingLearner = null)
                    AlertBus.success(if (editing != null) "Learner updated" else "Learner created")
                    refresh()
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSavingLearner = false, errorMessage = result.message)
            }
        }
    }

    fun openCreateGroupDialog() {
        _uiState.value = _uiState.value.copy(isCreateGroupDialogOpen = true)
    }

    fun dismissCreateGroupDialog() {
        _uiState.value = _uiState.value.copy(isCreateGroupDialogOpen = false, isSavingGroup = false)
    }

    fun createGroup(name: String, description: String) {
        val userId = authRepository.currentUserId() ?: return
        if (name.isBlank()) {
            viewModelScope.launch { AlertBus.error("Enter a group name.") }
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingGroup = true, errorMessage = null)
            when (val result = learnersRepository.createGroup(userId, name.trim(), description.trim().ifBlank { null })) {
                is AppResult.Success -> {
                    analyticsLogger.logGroupCreated()
                    _uiState.value = _uiState.value.copy(
                        isSavingGroup = false,
                        isCreateGroupDialogOpen = false,
                        groups = listOf(result.data) + _uiState.value.groups
                    )
                    AlertBus.success("\"${result.data.name}\" created")
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSavingGroup = false, errorMessage = result.message)
            }
        }
    }

    /** Quick "+ Create Group" inline in the learner form — same call, just returns the new id for auto-select instead of closing a dialog. */
    suspend fun quickCreateGroup(name: String): String? {
        val userId = authRepository.currentUserId() ?: return null
        if (name.isBlank()) return null
        return when (val result = learnersRepository.createGroup(userId, name.trim(), null)) {
            is AppResult.Success -> {
                analyticsLogger.logGroupCreated()
                _uiState.value = _uiState.value.copy(groups = listOf(result.data) + _uiState.value.groups)
                result.data.id
            }
            is AppResult.Error -> null
        }
    }

    fun requestDeleteLearner(learner: Learner) {
        _uiState.value = _uiState.value.copy(learnerToDelete = learner)
    }

    fun cancelDeleteLearner() {
        _uiState.value = _uiState.value.copy(learnerToDelete = null)
    }

    fun confirmDeleteLearner() {
        val learner = _uiState.value.learnerToDelete ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingLearner = true)
            when (learnersRepository.deleteLearner(learner.id)) {
                is AppResult.Success -> {
                    analyticsLogger.logLearnerDeleted()
                    _uiState.value = _uiState.value.copy(
                        isDeletingLearner = false,
                        learnerToDelete = null,
                        learners = _uiState.value.learners.filterNot { it.id == learner.id }
                    )
                    AlertBus.success("Learner deleted")
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isDeletingLearner = false, learnerToDelete = null)
            }
        }
    }

    fun openStudentProfile(learner: Learner) {
        _uiState.value = _uiState.value.copy(
            profileLearner = learner,
            isStudentProfileOpen = true,
            profileAttempts = emptyList(),
            isLoadingProfileAttempts = true
        )
        viewModelScope.launch {
            when (val result = learnersRepository.getQuizAttempts(learner.id)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isLoadingProfileAttempts = false, profileAttempts = result.data)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoadingProfileAttempts = false)
            }
        }
    }

    fun dismissStudentProfile() {
        _uiState.value = _uiState.value.copy(isStudentProfileOpen = false, profileLearner = null, profileAttempts = emptyList())
    }
}
