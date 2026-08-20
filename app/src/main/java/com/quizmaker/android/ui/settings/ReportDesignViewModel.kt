package com.quizmaker.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.ProfileRepository
import com.quizmaker.android.repository.SettingsRepository
import com.quizmaker.android.util.ReportDesign
import com.quizmaker.android.util.ReportTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportDesignUiState(
    val isLoading: Boolean = true,
    val template: ReportTemplate = ReportTemplate.MODERN,
    val colorHex: String = ReportDesign.DEFAULT_COLOR_HEX,
    val businessName: String = "",
    val address: String = "",
    val businessLogoUrl: String? = null,
    val isUploadingLogo: Boolean = false,
    val isSavingBranding: Boolean = false,
    val brandingSaveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ReportDesignViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportDesignUiState())
    val uiState: StateFlow<ReportDesignUiState> = _uiState.asStateFlow()

    // ProfileRepository.updateProfile() is a full-record update — these three round-trip
    // unchanged whenever business name/address are saved from this screen, which only edits the
    // letterhead-relevant fields.
    private var profileName: String = ""
    private var profilePhoneNumber: String = ""
    private var profileCountry: String = ""

    init {
        val userId = authRepository.currentUserId()
        if (userId == null) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        } else {
            viewModelScope.launch {
                val designResult = settingsRepository.getReportDesign(userId)
                val profileResult = authRepository.getCurrentProfile()
                val profile = (profileResult as? AppResult.Success)?.data
                if (profile != null) {
                    profileName = profile.name
                    profilePhoneNumber = profile.phoneNumber
                    profileCountry = profile.country
                }
                _uiState.value = when (designResult) {
                    is AppResult.Success -> _uiState.value.copy(
                        isLoading = false,
                        template = designResult.data.template,
                        colorHex = designResult.data.colorHex,
                        businessName = profile?.businessName.orEmpty(),
                        address = profile?.address.orEmpty(),
                        businessLogoUrl = profile?.businessLogo
                    )
                    is AppResult.Error -> _uiState.value.copy(isLoading = false, errorMessage = designResult.message)
                }
            }
        }
    }

    /**
     * Optimistic, same pattern as ReportedQuestionsViewModel.toggleStatus(): the local pick
     * updates immediately — every export from this point on already reads it via
     * PdfBrandingProvider — and is saved in the background. A save failure surfaces an error but
     * deliberately doesn't revert the pick; the export path is already using the new value this
     * session either way, so silently reverting the UI while exports keep the new look would be
     * more confusing than just failing to persist it for next session.
     */
    fun onTemplateSelected(template: ReportTemplate) {
        _uiState.value = _uiState.value.copy(template = template)
        save()
    }

    fun onColorSelected(colorHex: String) {
        _uiState.value = _uiState.value.copy(colorHex = colorHex)
        save()
    }

    fun onBusinessNameChange(value: String) {
        _uiState.value = _uiState.value.copy(businessName = value, brandingSaveSuccess = false)
    }

    fun onAddressChange(value: String) {
        _uiState.value = _uiState.value.copy(address = value, brandingSaveSuccess = false)
    }

    fun saveBranding() {
        val userId = authRepository.currentUserId() ?: return
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingBranding = true, errorMessage = null, brandingSaveSuccess = false)
            val result = profileRepository.updateProfile(
                userId = userId,
                name = profileName,
                businessName = state.businessName.trim(),
                phoneNumber = profilePhoneNumber,
                country = profileCountry,
                address = state.address.trim()
            )
            when (result) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isSavingBranding = false, brandingSaveSuccess = true)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSavingBranding = false, errorMessage = result.message)
            }
        }
    }

    /** [contentType] e.g. "image/jpeg" — read from the picked image's Uri via ContentResolver at the call site. */
    fun uploadLogo(bytes: ByteArray, contentType: String) {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingLogo = true, errorMessage = null)
            when (val result = profileRepository.uploadLogo(userId, bytes, contentType)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isUploadingLogo = false, businessLogoUrl = result.data)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isUploadingLogo = false, errorMessage = result.message)
            }
        }
    }

    fun removeLogo() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingLogo = true, errorMessage = null)
            when (val result = profileRepository.removeLogo(userId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isUploadingLogo = false, businessLogoUrl = null)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isUploadingLogo = false, errorMessage = result.message)
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun save() {
        val userId = authRepository.currentUserId() ?: return
        val design = ReportDesign(_uiState.value.template, _uiState.value.colorHex)
        viewModelScope.launch {
            (settingsRepository.saveReportDesign(userId, design) as? AppResult.Error)?.let {
                _uiState.value = _uiState.value.copy(errorMessage = it.message)
            }
        }
    }
}
