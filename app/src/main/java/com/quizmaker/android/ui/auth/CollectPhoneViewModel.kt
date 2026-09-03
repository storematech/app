package com.quizmaker.android.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.analytics.AnalyticsLogger
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
) {
    /** India requires exactly 10 digits (the real mobile number length there); every other
     *  country just needs a plausible minimum of 5 digits, since dial plans vary too widely to
     *  validate more strictly without a per-country length table. */
    val minPhoneDigits: Int get() = if (selectedCountry.iso == "IN") 10 else 5

    val isPhoneValid: Boolean get() = if (selectedCountry.iso == "IN") {
        phoneNumber.length == 10
    } else {
        phoneNumber.length >= 5
    }
}

@HiltViewModel
class CollectPhoneViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val analyticsLogger: AnalyticsLogger
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
        val state = _uiState.value
        // Re-clamp an already-typed number if switching to India makes it too long (e.g. typed 12
        // digits under a country with no strict cap, then picked India from the list).
        val maxDigits = if (country.iso == "IN") 10 else 14
        _uiState.value = state.copy(
            selectedCountry = country,
            phoneNumber = state.phoneNumber.take(maxDigits),
            isCountryPickerOpen = false,
            errorMessage = null
        )
    }

    fun onPhoneNumberChange(value: String) {
        val state = _uiState.value
        val maxDigits = if (state.selectedCountry.iso == "IN") 10 else 14
        _uiState.value = state.copy(phoneNumber = value.filter { it.isDigit() }.take(maxDigits), errorMessage = null)
    }

    fun save() {
        val state = _uiState.value
        if (!state.isPhoneValid) return
        val userId = authRepository.currentUserId() ?: return
        val fullNumber = "${state.selectedCountry.dialCode} ${state.phoneNumber}"
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            when (val result = profileRepository.updatePhoneNumber(userId, fullNumber, state.selectedCountry.countryName)) {
                is AppResult.Success -> {
                    analyticsLogger.logPhoneCollected()
                    _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
            }
        }
    }
}
