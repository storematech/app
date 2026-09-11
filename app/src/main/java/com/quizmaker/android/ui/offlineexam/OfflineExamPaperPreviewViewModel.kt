package com.quizmaker.android.ui.offlineexam

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.Profile
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.ProfileRepository
import com.quizmaker.android.repository.QuizRepository
import com.quizmaker.android.util.MasterPaperMode
import com.quizmaker.android.util.MasterPaperPdfExporter
import com.quizmaker.android.util.PdfBranding
import com.quizmaker.android.util.PdfBrandingProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Width, in pixels, the live preview bitmap is rendered at — generous enough to look sharp on any
 *  phone screen once Compose scales it down to fit the preview box, without re-rendering at the
 *  exact on-screen pixel size every time the box is measured. */
private const val PREVIEW_WIDTH_PX = 900

data class OfflineExamPaperPreviewUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,

    val quizTitle: String = "",
    val questions: List<Question> = emptyList(),

    // Editable letterhead fields — seeded from the account's saved PDF branding (same fields the
    // Profile screen manages), so editing here is really just a shortcut into that same data,
    // pre-populated and previewed live against the paper actually being downloaded.
    val businessName: String = "",
    val address: String = "",
    val subtitle: String = "",
    val logoUrl: String? = null,
    val isUploadingLogo: Boolean = false,

    val previewBitmap: Bitmap? = null,
    val isRenderingPreview: Boolean = false,

    val isSaving: Boolean = false
)

/**
 * Backs the Offline Exam paper preview — shown when the user taps "Preview Paper" instead of the
 * old direct-download flow (see CreateOfflineExamScreen). Lets them tweak the account's PDF
 * letterhead (business name/address/subtitle/logo) with a live, re-rendered preview of the actual
 * paper, then "Save & Download" persists that letterhead (via ProfileRepository — the same fields
 * the Profile screen edits) and shares the PDF, exactly as the old direct-download button did.
 *
 * The preview is never hand-drawn separately from the real export: [MasterPaperPdfExporter]'s
 * `buildPdfFile` is the single source of truth, and [MasterPaperPdfExporter.renderPreviewBitmap]
 * just rasterizes page 1 of that same PDF, so the preview can't drift out of sync with Download.
 */
@HiltViewModel
class OfflineExamPaperPreviewViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val authRepository: AuthRepository,
    private val quizRepository: QuizRepository,
    private val profileRepository: ProfileRepository,
    private val pdfBrandingProvider: PdfBrandingProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: String = savedStateHandle.get<String>("quizId").orEmpty()

    private val _uiState = MutableStateFlow(OfflineExamPaperPreviewUiState())
    val uiState: StateFlow<OfflineExamPaperPreviewUiState> = _uiState.asStateFlow()

    // Unedited fields (name/phone/country/website/registrationNumber/letterheadPhone/
    // letterheadEmail/gstNumber) needed to round-trip ProfileRepository.updateProfile's
    // all-fields-at-once shape without this screen's businessName/address/subtitle edits
    // clobbering anything it doesn't show.
    private var baseProfile: Profile? = null

    // Accent color + template + the currently-loaded logo bitmap, reused as-is on every preview
    // re-render — only businessName/address/tagline/logo are ever overridden by this screen.
    private var baseBranding: PdfBranding = PdfBranding.NONE

    private var previewJob: Job? = null

    init {
        loadAll()
    }

    private fun loadAll() {
        if (quizId.isBlank()) {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Missing exam.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val quizResult = quizRepository.getQuizById(quizId)
            val quiz = (quizResult as? AppResult.Success)?.data
            if (quiz == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = (quizResult as? AppResult.Error)?.message ?: "Couldn't load this exam."
                )
                return@launch
            }
            val questionsResult = quizRepository.getQuestionsForQuiz(quizId)
            val questions = (questionsResult as? AppResult.Success)?.data
            if (questions == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = (questionsResult as? AppResult.Error)?.message ?: "Couldn't load this exam's questions."
                )
                return@launch
            }
            val profile = (authRepository.getCurrentProfile(notifyOnError = false) as? AppResult.Success)?.data
            baseProfile = profile
            baseBranding = pdfBrandingProvider.get()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                quizTitle = quiz.title,
                questions = questions,
                businessName = baseBranding.businessName.orEmpty(),
                address = baseBranding.address.orEmpty(),
                subtitle = baseBranding.tagline.orEmpty(),
                logoUrl = profile?.businessLogo
            )
            renderPreview()
        }
    }

    fun onBusinessNameChange(value: String) {
        _uiState.value = _uiState.value.copy(businessName = value)
        scheduleRefresh()
    }

    fun onAddressChange(value: String) {
        _uiState.value = _uiState.value.copy(address = value)
        scheduleRefresh()
    }

    fun onSubtitleChange(value: String) {
        _uiState.value = _uiState.value.copy(subtitle = value)
        scheduleRefresh()
    }

    /** [contentType] e.g. "image/jpeg" — read from the picked image's Uri via ContentResolver at
     *  the call site. Uploads immediately (same as CertificateDesignerScreen/ProfileScreen's own
     *  logo pickers) rather than waiting for Save, since it's already an explicit, deliberate
     *  action rather than a keystroke that needs debouncing. */
    fun uploadLogo(bytes: ByteArray, contentType: String) {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingLogo = true, errorMessage = null)
            when (val result = profileRepository.uploadLogo(userId, bytes, contentType)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isUploadingLogo = false, logoUrl = result.data)
                    renderPreview()
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isUploadingLogo = false, errorMessage = result.message)
            }
        }
    }

    fun removeLogo() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingLogo = true, errorMessage = null)
            when (val result = profileRepository.removeLogo(userId)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isUploadingLogo = false, logoUrl = null)
                    renderPreview()
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isUploadingLogo = false, errorMessage = result.message)
            }
        }
    }

    /** Debounced re-render so a live preview doesn't re-generate a whole PDF on every keystroke —
     *  only once typing pauses for a moment. */
    private fun scheduleRefresh() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(400)
            renderPreview()
        }
    }

    private suspend fun renderPreview() {
        val state = _uiState.value
        if (state.quizTitle.isBlank() && state.questions.isEmpty()) return
        _uiState.value = _uiState.value.copy(isRenderingPreview = true)
        val logoBitmap = state.logoUrl?.let { loadBitmap(it) }
        val branding = baseBranding.copy(
            logo = logoBitmap,
            businessName = state.businessName.trim().ifBlank { null },
            address = state.address.trim().ifBlank { null },
            tagline = state.subtitle.trim().ifBlank { null }
        )
        val bitmap = withContext(Dispatchers.IO) {
            MasterPaperPdfExporter.renderPreviewBitmap(
                appContext, state.quizTitle, state.questions, MasterPaperMode.OFFLINE, branding, PREVIEW_WIDTH_PX
            )
        }
        // A failed render (rare — see renderPreviewBitmap's own KDoc) just keeps showing the last
        // good preview rather than blanking it out.
        _uiState.value = _uiState.value.copy(isRenderingPreview = false, previewBitmap = bitmap ?: _uiState.value.previewBitmap)
    }

    private suspend fun loadBitmap(url: String): Bitmap? = runCatching {
        val request = ImageRequest.Builder(appContext).data(url).allowHardware(false).build()
        (appContext.imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
    }.getOrNull()

    /** Persists the letterhead edits (same `profiles` fields Profile screen edits — see
     *  ProfileRepository.updateProfile) and, once saved, shares the exact same PDF the old
     *  direct-download button produced. [context] is the calling Activity context, needed to
     *  launch the share chooser — the account-wide render above only ever needs the app context. */
    fun saveAndDownload(context: Context) {
        val userId = authRepository.currentUserId() ?: return
        val profile = baseProfile ?: return
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val state = _uiState.value
            val saveResult = profileRepository.updateProfile(
                userId = userId,
                name = profile.name,
                businessName = state.businessName.trim(),
                phoneNumber = profile.phoneNumber,
                country = profile.country,
                address = state.address.trim(),
                website = profile.website.orEmpty(),
                registrationNumber = profile.registrationNumber.orEmpty(),
                letterheadPhone = profile.letterheadPhone.orEmpty(),
                letterheadEmail = profile.letterheadEmail.orEmpty(),
                tagline = state.subtitle.trim(),
                gstNumber = profile.gstNumber.orEmpty()
            )
            if (saveResult is AppResult.Error) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = saveResult.message)
                return@launch
            }
            // Re-fetch rather than hand-building a PdfBranding from local state — guarantees the
            // downloaded PDF reflects exactly what was just saved (accent color/template included).
            val freshBranding = withContext(Dispatchers.IO) { pdfBrandingProvider.get() }
            val intent = withContext(Dispatchers.IO) {
                MasterPaperPdfExporter.export(context, state.quizTitle, state.questions, MasterPaperMode.OFFLINE, freshBranding)
            }
            _uiState.value = _uiState.value.copy(isSaving = false)
            context.startActivity(Intent.createChooser(intent, "Export Offline Exam"))
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
