package com.quizmaker.android.ui.appintro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.analytics.AnalyticsLogger
import com.quizmaker.android.core.prefs.AppIntroPrefs
import com.quizmaker.android.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppIntroViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appIntroPrefs: AppIntroPrefs,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    /** Marks the carousel as seen (so it never shows again for this account+device) regardless of
     *  whether the user swiped through every slide or tapped "Skip" partway through. */
    fun markIntroShown(skipped: Boolean, reachedSlide: Int, totalSlides: Int) {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch { appIntroPrefs.markIntroShown(userId) }
        analyticsLogger.logAppIntroResult(skipped, reachedSlide, totalSlides)
    }
}
