package com.quizmaker.android.ui.tools.voting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.CandidateTally
import com.quizmaker.android.data.model.VotingBallot
import com.quizmaker.android.data.model.VotingCampaign
import com.quizmaker.android.data.model.tally
import com.quizmaker.android.repository.VotingRepository
import com.quizmaker.android.util.PdfBranding
import com.quizmaker.android.util.PdfBrandingProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VotingResultsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val campaign: VotingCampaign? = null,
    val tallies: List<CandidateTally> = emptyList(),
    val ballots: List<VotingBallot> = emptyList()
) {
    val totalBallots: Int get() = ballots.size
    /** Only worth listing individually when at least one voter left a name/email — a fully anonymous campaign has nothing more to show than the tallies above. */
    val hasIdentifiedVoters: Boolean get() = ballots.any { !it.voterName.isNullOrBlank() || !it.voterEmail.isNullOrBlank() }
}

/** Results for a single voting campaign — candidate tallies plus (if any voter identified themselves) the raw ballot list. */
@HiltViewModel
class VotingResultsViewModel @Inject constructor(
    private val repository: VotingRepository,
    private val pdfBrandingProvider: PdfBrandingProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val campaignId: String = checkNotNull(savedStateHandle["campaignId"])

    private val _uiState = MutableStateFlow(VotingResultsUiState())
    val uiState: StateFlow<VotingResultsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val campaignResult = repository.getCampaign(campaignId)
            val ballotsResult = repository.getBallots(campaignId)
            when {
                campaignResult is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = campaignResult.message)
                ballotsResult is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = ballotsResult.message)
                campaignResult is AppResult.Success && ballotsResult is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        campaign = campaignResult.data,
                        tallies = campaignResult.data.tally(ballotsResult.data),
                        ballots = ballotsResult.data
                    )
                }
            }
        }
    }

    suspend fun getPdfBranding(): PdfBranding = pdfBrandingProvider.get()
}
