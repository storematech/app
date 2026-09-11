package com.quizmaker.android.ui.businesscard

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.SettingsRepository
import com.quizmaker.android.util.BusinessCardImageExporter
import com.quizmaker.android.util.PdfBrandingProvider
import com.quizmaker.android.util.ReportDesign
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BusinessCardUiState(
    val isLoading: Boolean = true,
    val personName: String = "",
    val businessName: String = "",
    val tagline: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val website: String? = null,
    val logoBitmap: Bitmap? = null,
    val accentColor: Int = ReportDesign.DEFAULT_ACCENT_COLOR,
    val template: BusinessCardTemplate = BusinessCardTemplate.CLASSIC,
    /** True once every optional field (tagline/phone/email/address/website) is blank — the screen
     *  shows a hint to fill in more details on the Profile screen when this is true. */
    val hasIncompleteDetails: Boolean = false
) {
    val renderData: BusinessCardRenderData
        get() = BusinessCardRenderData(
            personName = personName,
            businessName = businessName,
            tagline = tagline,
            phone = phone,
            email = email,
            address = address,
            website = website
        )
}

@HiltViewModel
class BusinessCardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val pdfBrandingProvider: PdfBrandingProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessCardUiState())
    val uiState: StateFlow<BusinessCardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val branding = pdfBrandingProvider.get()
            val profileResult = authRepository.getCurrentProfile(notifyOnError = false)
            val profile = (profileResult as? AppResult.Success)?.data

            val phone = branding.letterheadPhone ?: profile?.phoneNumber?.takeIf { it.isNotBlank() }
            val email = branding.letterheadEmail ?: profile?.email?.takeIf { it.isNotBlank() }
            val savedTemplate = profile?.id?.let {
                (settingsRepository.getBusinessCardTemplate(it) as? AppResult.Success)?.data
            }
            val template = runCatching { BusinessCardTemplate.valueOf(savedTemplate.orEmpty().uppercase()) }
                .getOrDefault(BusinessCardTemplate.CLASSIC)

            val businessName = branding.businessName ?: profile?.businessName?.takeIf { it.isNotBlank() }.orEmpty()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                personName = profile?.name.orEmpty(),
                businessName = businessName,
                tagline = branding.tagline,
                phone = phone,
                email = email,
                address = branding.address,
                website = branding.website,
                logoBitmap = branding.logo,
                accentColor = branding.accentColor,
                template = template,
                hasIncompleteDetails = listOf(branding.tagline, phone, email, branding.address, branding.website).all { it.isNullOrBlank() }
            )
        }
    }

    /** Optimistic, same convention as ReportDesignViewModel.onTemplateSelected: the pick updates
     *  immediately — the preview and any export from this point on already use it — and is saved
     *  to the background `user_settings` store without blocking or reverting the UI on failure. */
    fun onTemplateSelected(template: BusinessCardTemplate) {
        _uiState.value = _uiState.value.copy(template = template)
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            settingsRepository.saveBusinessCardTemplate(userId, template.name.lowercase())
        }
    }

    fun buildShareIntent(context: Context): Intent {
        val state = _uiState.value
        return BusinessCardImageExporter.export(
            context = context,
            template = state.template,
            data = state.renderData,
            logoBitmap = state.logoBitmap,
            accentColor = state.accentColor
        )
    }
}
