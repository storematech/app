package com.quizmaker.android.ui.classlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.analytics.AnalyticsLogger
import com.quizmaker.android.core.prefs.ClassesIntroPrefs
import com.quizmaker.android.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClassesIntroViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val classesIntroPrefs: ClassesIntroPrefs,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    /** Whether More → Classes should route through the intro interstitial before the Classes list itself. */
    suspend fun shouldShowIntro(): Boolean {
        val userId = authRepository.currentUserId() ?: return false
        return !classesIntroPrefs.hasShownIntro(userId)
    }

    /** Marks the interstitial as seen (so it never shows again for this account+device) regardless of which CTA was tapped. */
    fun markIntroShown(used: Boolean) {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch { classesIntroPrefs.markIntroShown(userId) }
        analyticsLogger.logClassesIntroResult(used)
    }
}
