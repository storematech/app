package com.quizmaker.android.ui.tools.voting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.alert.AlertBus
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Candidate
import com.quizmaker.android.data.model.VotingCampaign
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.VotingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VotingListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val campaigns: List<VotingCampaign> = emptyList(),
    val voteCounts: Map<String, Int> = emptyMap(),
    val isEditSheetOpen: Boolean = false,
    val editingCampaign: VotingCampaign? = null,
    val isSaving: Boolean = false
)

/** Powers Tools → Voting — the management list, same shape as PollListViewModel. */
@HiltViewModel
class VotingListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val repository: VotingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VotingListUiState())
    val uiState: StateFlow<VotingListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.getCampaigns(userId)) {
                is AppResult.Success -> {
                    val campaigns = result.data
                    val counts = (repository.getVoteCounts(campaigns.map { it.id }) as? AppResult.Success)?.data.orEmpty()
                    _uiState.value = _uiState.value.copy(isLoading = false, campaigns = campaigns, voteCounts = counts)
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun openCreateSheet() {
        _uiState.value = _uiState.value.copy(isEditSheetOpen = true, editingCampaign = null)
    }

    fun openEditSheet(campaign: VotingCampaign) {
        _uiState.value = _uiState.value.copy(isEditSheetOpen = true, editingCampaign = campaign)
    }

    fun dismissEditSheet() {
        _uiState.value = _uiState.value.copy(isEditSheetOpen = false, editingCampaign = null)
    }

    fun saveCampaign(
        title: String,
        description: String,
        candidates: List<Candidate>,
        requireEmail: Boolean,
        opensAt: String?,
        closesAt: String?,
        showResults: Boolean,
        isActive: Boolean
    ) {
        val userId = authRepository.currentUserId() ?: return
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter a campaign title.")
            return
        }
        if (candidates.any { it.name.isBlank() } || candidates.size < 2) {
            _uiState.value = _uiState.value.copy(errorMessage = "Add at least 2 candidates.")
            return
        }
        val editingId = _uiState.value.editingCampaign?.id
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val result = if (editingId != null) {
                repository.updateCampaign(
                    campaignId = editingId,
                    title = title.trim(),
                    description = description.trim().ifBlank { null },
                    candidates = candidates,
                    requireEmail = requireEmail,
                    opensAt = opensAt,
                    closesAt = closesAt,
                    showResults = showResults,
                    isActive = isActive
                )
            } else {
                repository.createCampaign(
                    userId = userId,
                    title = title.trim(),
                    description = description.trim().ifBlank { null },
                    candidates = candidates,
                    requireEmail = requireEmail,
                    opensAt = opensAt,
                    closesAt = closesAt,
                    showResults = showResults,
                    isActive = isActive
                )
            }
            when (result) {
                is AppResult.Success -> {
                    val updatedCampaigns = if (editingId != null) {
                        _uiState.value.campaigns.map { if (it.id == editingId) result.data else it }
                    } else {
                        listOf(result.data) + _uiState.value.campaigns
                    }
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isEditSheetOpen = false,
                        editingCampaign = null,
                        campaigns = updatedCampaigns
                    )
                    AlertBus.success(if (editingId != null) "Campaign updated" else "Campaign created")
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
            }
        }
    }

    fun toggleActive(campaign: VotingCampaign) {
        val newActive = !campaign.isActive
        _uiState.value = _uiState.value.copy(
            campaigns = _uiState.value.campaigns.map { if (it.id == campaign.id) it.copy(isActive = newActive) else it }
        )
        viewModelScope.launch { repository.setActive(campaign.id, newActive) }
    }

    fun deleteCampaign(campaignId: String) {
        _uiState.value = _uiState.value.copy(campaigns = _uiState.value.campaigns.filterNot { it.id == campaignId })
        viewModelScope.launch {
            repository.deleteCampaign(campaignId)
            AlertBus.success("Campaign deleted")
        }
    }
}
