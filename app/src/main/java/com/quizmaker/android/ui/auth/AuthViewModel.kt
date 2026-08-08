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

/** Where the email-first auth flow currently is. */
enum class AuthStep {
    EMAIL,
    SIGN_IN,
    SIGN_UP
}

data class AuthUiState(
    val step: AuthStep = AuthStep.EMAIL,
    val email: String = "",
    val isCheckingEmail: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Set after a resetPassword() call succeeds, so the screen can show a confirmation. */
    val resetEmailSent: Boolean = false,
    /**
     * Set after signUp() succeeds. If the Supabase project requires email confirmation,
     * the session won't become Authenticated yet, so the screen needs its own
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

    /** Step 1 of the email-first flow: look up whether this email already has an account. */
    fun continueWithEmail(email: String) {
        val trimmed = email.trim()
        if (trimmed.isBlank() || !trimmed.contains("@")) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter a valid email address.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingEmail = true, errorMessage = null)
            when (val result = authRepository.checkEmailExists(trimmed)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    isCheckingEmail = false,
                    email = trimmed,
                    step = if (result.data) AuthStep.SIGN_IN else AuthStep.SIGN_UP
                )
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isCheckingEmail = false, errorMessage = result.message)
            }
        }
    }

    /** "Not you?" — back to the email step, keeping whatever was typed so it's easy to fix a typo. */
    fun changeEmail() {
        _uiState.value = _uiState.value.copy(step = AuthStep.EMAIL, errorMessage = null)
    }

    fun signIn(password: String) {
        val email = _uiState.value.email
        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter your password.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.signIn(email, password)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false)
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun signUp(name: String, password: String) {
        val email = _uiState.value.email
        if (name.isBlank() || password.length < 6) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Enter your name and a password of at least 6 characters."
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.signUp(email, password, name.trim())) {
                is AppResult.Success -> _uiState.value =
                    // requiresConfirmation == false means the session is already established —
                    // NavGraph's own sessionStatus listener takes over navigation from here, so
                    // showing "check your inbox" here would just be a wrong message that flashes
                    // for the moment before that navigation fires.
                    _uiState.value.copy(isLoading = false, signUpSucceeded = result.data)
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
