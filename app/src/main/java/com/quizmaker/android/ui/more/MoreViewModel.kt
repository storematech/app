package com.quizmaker.android.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MoreUiState(
    val name: String = "",
    val email: String = ""
)

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoreUiState())
    val uiState: StateFlow<MoreUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = authRepository.getCurrentProfile()) {
                is AppResult.Success -> _uiState.value = MoreUiState(name = result.data.name, email = result.data.email)
                is AppResult.Error -> Unit
            }
        }
    }
}
