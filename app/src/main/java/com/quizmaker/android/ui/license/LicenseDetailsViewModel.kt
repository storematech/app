package com.quizmaker.android.ui.license

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

data class LicenseDetailsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val userType: String? = null,
    val licenseExpiredDate: String? = null
)

@HiltViewModel
class LicenseDetailsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LicenseDetailsUiState())
    val uiState: StateFlow<LicenseDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.getCurrentProfile()) {
                is AppResult.Success -> _uiState.value = LicenseDetailsUiState(
                    isLoading = false,
                    userType = result.data.userType,
                    licenseExpiredDate = result.data.licenseExpiredDate
                )
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }
}
