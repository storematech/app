package com.quizmaker.android.ui.tools.poll

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Poll
import com.quizmaker.android.data.model.PollOptionTally
import com.quizmaker.android.data.model.PollVote
import com.quizmaker.android.data.model.tally
import com.quizmaker.android.repository.PollRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PollResultsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val poll: Poll? = null,
    val tallies: List<PollOptionTally> = emptyList(),
    val votes: List<PollVote> = emptyList()
) {
    val totalVotes: Int get() = votes.size
    /** Only worth listing individually when at least one voter left a name/email — a fully anonymous poll has nothing more to show than the tallies above. */
    val hasIdentifiedVoters: Boolean get() = votes.any { !it.voterName.isNullOrBlank() || !it.voterEmail.isNullOrBlank() }
}

/** Results for a single poll — option tallies plus (if any voter identified themselves) the raw vote list. */
@HiltViewModel
class PollResultsViewModel @Inject constructor(
    private val repository: PollRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val pollId: String = checkNotNull(savedStateHandle["pollId"])

    private val _uiState = MutableStateFlow(PollResultsUiState())
    val uiState: StateFlow<PollResultsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val pollResult = repository.getPoll(pollId)
            val votesResult = repository.getVotes(pollId)
            when {
                pollResult is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = pollResult.message)
                votesResult is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = votesResult.message)
                pollResult is AppResult.Success && votesResult is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        poll = pollResult.data,
                        tallies = pollResult.data.tally(votesResult.data),
                        votes = votesResult.data
                    )
                }
            }
        }
    }
}
