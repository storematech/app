package com.quizmaker.android.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val email: String = "",
    val name: String = "",
    val businessName: String = "",
    val phoneNumber: String = "",
    val country: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isChangingPassword: Boolean = false,
    val passwordChangeSuccess: Boolean = false,
    val passwordError: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.getCurrentProfile()) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    email = result.data.email,
                    name = result.data.name,
                    businessName = result.data.businessName,
                    phoneNumber = result.data.phoneNumber,
                    country = result.data.country
                )
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun onNameChange(value: String) { _uiState.value = _uiState.value.copy(name = value, saveSuccess = false) }
    fun onBusinessNameChange(value: String) { _uiState.value = _uiState.value.copy(businessName = value, saveSuccess = false) }
    fun onPhoneNumberChange(value: String) { _uiState.value = _uiState.value.copy(phoneNumber = value, saveSuccess = false) }
    fun onCountryChange(value: String) { _uiState.value = _uiState.value.copy(country = value, saveSuccess = false) }
    fun onNewPasswordChange(value: String) { _uiState.value = _uiState.value.copy(newPassword = value, passwordChangeSuccess = false) }
    fun onConfirmPasswordChange(value: String) { _uiState.value = _uiState.value.copy(confirmPassword = value, passwordChangeSuccess = false) }

    fun saveProfile() {
        val userId = authRepository.currentUserId() ?: return
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, saveSuccess = false)
            val result = profileRepository.updateProfile(
                userId = userId,
                name = state.name.trim(),
                businessName = state.businessName.trim(),
                phoneNumber = state.phoneNumber.trim(),
                country = state.country.trim()
            )
            when (result) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
            }
        }
    }

    fun changePassword() {
        val state = _uiState.value
        if (state.newPassword.length < 6) {
            _uiState.value = _uiState.value.copy(passwordError = "Password must be at least 6 characters.")
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.value = _uiState.value.copy(passwordError = "Passwords don't match.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChangingPassword = true, passwordError = null)
            when (val result = authRepository.changePassword(state.newPassword)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    isChangingPassword = false,
                    passwordChangeSuccess = true,
                    newPassword = "",
                    confirmPassword = ""
                )
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isChangingPassword = false, passwordError = result.message)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
