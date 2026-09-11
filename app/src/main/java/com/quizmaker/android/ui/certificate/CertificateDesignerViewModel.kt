package com.quizmaker.android.ui.certificate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.remote.dto.CertificateDesignDto
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.CertificateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CertificateDesignerUiState(
    val isLoading: Boolean = true,
    val title: String = "Certificate of Achievement",
    val bodyText: String = "This certificate is proudly presented for outstanding performance and dedication.",
    val companyName: String = "",
    val signerName: String = "",
    val signerRole: String = "",
    val issueDateLabel: String = "Date of Issue",
    val logoUrl: String? = null,
    val signatureUrl: String? = null,
    val template: String = "traditional",
    /** Local-only — never persisted. Lets the designer preview a real name without a saved design row ever carrying one. */
    val previewRecipientName: String = "Daniel Gallego",
    val isSaving: Boolean = false,
    val isUploadingLogo: Boolean = false,
    val isUploadingSignature: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CertificateDesignerViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val certificateRepository: CertificateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CertificateDesignerUiState())
    val uiState: StateFlow<CertificateDesignerUiState> = _uiState.asStateFlow()

    init {
        val userId = authRepository.currentUserId()
        if (userId == null) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        } else {
            viewModelScope.launch {
                when (val result = certificateRepository.getDesign(userId)) {
                    is AppResult.Success -> {
                        val design = result.data
                        _uiState.value = if (design != null) {
                            _uiState.value.copy(
                                isLoading = false,
                                title = design.title,
                                bodyText = design.bodyText,
                                companyName = design.companyName,
                                signerName = design.signerName,
                                signerRole = design.signerRole,
                                issueDateLabel = design.issueDateLabel,
                                logoUrl = design.logoUrl,
                                signatureUrl = design.signatureUrl,
                                template = design.template
                            )
                        } else {
                            _uiState.value.copy(isLoading = false)
                        }
                    }
                    is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(title = value, saveSuccess = false)
    }

    fun onBodyTextChange(value: String) {
        _uiState.value = _uiState.value.copy(bodyText = value, saveSuccess = false)
    }

    fun onCompanyNameChange(value: String) {
        _uiState.value = _uiState.value.copy(companyName = value, saveSuccess = false)
    }

    fun onSignerNameChange(value: String) {
        _uiState.value = _uiState.value.copy(signerName = value, saveSuccess = false)
    }

    fun onSignerRoleChange(value: String) {
        _uiState.value = _uiState.value.copy(signerRole = value, saveSuccess = false)
    }

    fun onIssueDateLabelChange(value: String) {
        _uiState.value = _uiState.value.copy(issueDateLabel = value, saveSuccess = false)
    }

    /** Local only — deliberately never written to CertificateRepository. */
    fun onPreviewRecipientNameChange(value: String) {
        _uiState.value = _uiState.value.copy(previewRecipientName = value)
    }

    /** Optimistic, same pattern as ReportDesignViewModel.onTemplateSelected() — the pick applies to
     *  the preview immediately and saves in the background; a save failure surfaces an error but
     *  doesn't revert the local pick. */
    fun onTemplateSelected(template: String) {
        _uiState.value = _uiState.value.copy(template = template)
        persist()
    }

    fun uploadLogo(bytes: ByteArray, contentType: String, extension: String) {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingLogo = true, errorMessage = null)
            when (val result = certificateRepository.uploadLogo(userId, bytes, contentType, extension)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isUploadingLogo = false, logoUrl = result.data)
                    persist()
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isUploadingLogo = false, errorMessage = result.message)
            }
        }
    }

    fun uploadSignature(bytes: ByteArray, contentType: String, extension: String) {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingSignature = true, errorMessage = null)
            when (val result = certificateRepository.uploadSignature(userId, bytes, contentType, extension)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isUploadingSignature = false, signatureUrl = result.data)
                    persist()
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isUploadingSignature = false, errorMessage = result.message)
            }
        }
    }

    fun removeLogo() {
        _uiState.value = _uiState.value.copy(logoUrl = null)
        persist()
    }

    fun removeSignature() {
        _uiState.value = _uiState.value.copy(signatureUrl = null)
        persist()
    }

    /** Explicit save for the text fields, mirrors ReportDesignViewModel.saveBranding(). */
    fun saveDesign() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, saveSuccess = false)
            when (val result = certificateRepository.upsertDesign(userId, currentDto(userId))) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun persist() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            (certificateRepository.upsertDesign(userId, currentDto(userId)) as? AppResult.Error)?.let {
                _uiState.value = _uiState.value.copy(errorMessage = it.message)
            }
        }
    }

    private fun currentDto(userId: String): CertificateDesignDto {
        val state = _uiState.value
        return CertificateDesignDto(
            userId = userId,
            title = state.title.trim(),
            bodyText = state.bodyText.trim(),
            companyName = state.companyName.trim(),
            signerName = state.signerName.trim(),
            signerRole = state.signerRole.trim(),
            logoUrl = state.logoUrl,
            signatureUrl = state.signatureUrl,
            issueDateLabel = state.issueDateLabel.trim().ifBlank { "Date of Issue" },
            template = state.template
        )
    }
}
