package com.quizmaker.android.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.ProfileRepository
import com.quizmaker.android.util.Country
import com.quizmaker.android.util.CountryCodes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectPhoneUiState(
    val selectedCountry: Country = CountryCodes.findByIso("US")!!,
    val phoneNumber: String = "",
    val isCountryPickerOpen: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class CollectPhoneViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectPhoneUiState(selectedCountry = CountryCodes.detectDefault(context)))
    val uiState: StateFlow<CollectPhoneUiState> = _uiState.asStateFlow()

    fun openCountryPicker() {
        _uiState.value = _uiState.value.copy(isCountryPickerOpen = true)
    }

    fun closeCountryPicker() {
        _uiState.value = _uiState.value.copy(isCountryPickerOpen = false)
    }

    fun onCountrySelected(country: Country) {
        _uiState.value = _uiState.value.copy(selectedCountry = country, isCountryPickerOpen = false, errorMessage = null)
    }

    fun onPhoneNumberChange(value: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = value.filter { it.isDigit() }.take(14), errorMessage = null)
    }

    fun save() {
        val state = _uiState.value
        if (state.phoneNumber.isBlank()) return
        val userId = authRepository.currentUserId() ?: return
        val fullNumber = "${state.selectedCountry.dialCode} ${state.phoneNumber}"
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            when (val result = profileRepository.updatePhoneNumber(userId, fullNumber, state.selectedCountry.countryName)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
            }
        }
    }
}
