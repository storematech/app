package com.quizmaker.android.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.analytics.AnalyticsLogger
import com.quizmaker.android.core.prefs.ToolsIntroPrefs
import com.quizmaker.android.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ToolsIntroViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val toolsIntroPrefs: ToolsIntroPrefs,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    /** Whether More → Tools should route through the intro interstitial before the Tools list itself. */
    suspend fun shouldShowIntro(): Boolean {
        val userId = authRepository.currentUserId() ?: return false
        return !toolsIntroPrefs.hasShownIntro(userId)
    }

    /** Marks the interstitial as seen (so it never shows again for this account+device) regardless of which CTA was tapped. */
    fun markIntroShown(used: Boolean) {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch { toolsIntroPrefs.markIntroShown(userId) }
        analyticsLogger.logToolsIntroResult(used)
    }
}
