package com.quizmaker.android.ui.auth

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

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Set after a resetPassword() call succeeds, so the screen can show a confirmation. */
    val resetEmailSent: Boolean = false,
    /**
     * Set after signUp() succeeds. If the Supabase project requires email confirmation,
     * the session won't become Authenticated yet, so the Signup screen needs its own
     * "check your email" message rather than relying solely on auto-navigation.
     */
    val signUpSucceeded: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter your email and password.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.signIn(email.trim(), password)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false)
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun signUp(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.length < 6) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Enter your name, email, and a password of at least 6 characters."
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.signUp(email.trim(), password, name.trim())) {
                is AppResult.Success -> _uiState.value =
                    _uiState.value.copy(isLoading = false, signUpSucceeded = true)
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter your email.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.resetPassword(email.trim())) {
                is AppResult.Success -> _uiState.value =
                    _uiState.value.copy(isLoading = false, resetEmailSent = true)
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
