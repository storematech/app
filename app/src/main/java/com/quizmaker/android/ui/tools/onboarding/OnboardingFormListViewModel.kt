package com.quizmaker.android.ui.tools.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.alert.AlertBus
import com.quizmaker.android.core.analytics.AnalyticsLogger
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.OnboardingForm
import com.quizmaker.android.data.model.ToolField
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.OnboardingFormRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingFormListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val forms: List<OnboardingForm> = emptyList(),
    val submissionCounts: Map<String, Int> = emptyMap(),
    val isEditSheetOpen: Boolean = false,
    val editingForm: OnboardingForm? = null,
    val isSaving: Boolean = false
)

/** Powers Tools → Onboarding Form — the management list, same shape as ClassListViewModel/QuizListViewModel. */
@HiltViewModel
class OnboardingFormListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val repository: OnboardingFormRepository,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingFormListUiState())
    val uiState: StateFlow<OnboardingFormListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.getForms(userId)) {
                is AppResult.Success -> {
                    val forms = result.data
                    val counts = (repository.getSubmissionCounts(forms.map { it.id }) as? AppResult.Success)?.data.orEmpty()
                    _uiState.value = _uiState.value.copy(isLoading = false, forms = forms, submissionCounts = counts)
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun openCreateSheet() {
        _uiState.value = _uiState.value.copy(isEditSheetOpen = true, editingForm = null)
    }

    fun openEditSheet(form: OnboardingForm) {
        _uiState.value = _uiState.value.copy(isEditSheetOpen = true, editingForm = form)
    }

    fun dismissEditSheet() {
        _uiState.value = _uiState.value.copy(isEditSheetOpen = false, editingForm = null)
    }

    fun saveForm(title: String, description: String, welcomeMessage: String, fields: List<ToolField>, isActive: Boolean) {
        val userId = authRepository.currentUserId() ?: return
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter a form title.")
            return
        }
        if (fields.any { it.label.isBlank() }) {
            _uiState.value = _uiState.value.copy(errorMessage = "Every field needs a label.")
            return
        }
        val editingId = _uiState.value.editingForm?.id
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val result = if (editingId != null) {
                repository.updateForm(
                    formId = editingId,
                    title = title.trim(),
                    description = description.trim().ifBlank { null },
                    welcomeMessage = welcomeMessage.trim().ifBlank { null },
                    fields = fields,
                    isActive = isActive
                )
            } else {
                repository.createForm(
                    userId = userId,
                    title = title.trim(),
                    description = description.trim().ifBlank { null },
                    welcomeMessage = welcomeMessage.trim().ifBlank { null },
                    fields = fields,
                    isActive = isActive
                )
            }
            when (result) {
                is AppResult.Success -> {
                    if (editingId == null) analyticsLogger.logToolCreated("onboarding")
                    val updatedForms = if (editingId != null) {
                        _uiState.value.forms.map { if (it.id == editingId) result.data else it }
                    } else {
                        listOf(result.data) + _uiState.value.forms
                    }
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isEditSheetOpen = false,
                        editingForm = null,
                        forms = updatedForms
                    )
                    AlertBus.success(if (editingId != null) "Form updated" else "Form created")
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
            }
        }
    }

    fun toggleActive(form: OnboardingForm) {
        val newActive = !form.isActive
        _uiState.value = _uiState.value.copy(
            forms = _uiState.value.forms.map { if (it.id == form.id) it.copy(isActive = newActive) else it }
        )
        analyticsLogger.logToolActiveToggled("onboarding", newActive)
        viewModelScope.launch { repository.setActive(form.id, newActive) }
    }

    fun deleteForm(formId: String) {
        _uiState.value = _uiState.value.copy(forms = _uiState.value.forms.filterNot { it.id == formId })
        analyticsLogger.logToolDeleted("onboarding")
        viewModelScope.launch {
            repository.deleteForm(formId)
            AlertBus.success("Form deleted")
        }
    }
}
