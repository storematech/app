package com.quizmaker.android.ui.learners

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.analytics.AnalyticsLogger
import com.quizmaker.android.core.prefs.LearnersIntroPrefs
import com.quizmaker.android.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LearnersIntroViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val learnersIntroPrefs: LearnersIntroPrefs,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    /** Whether More → Learners should route through the intro interstitial before the Learners list itself. */
    suspend fun shouldShowIntro(): Boolean {
        val userId = authRepository.currentUserId() ?: return false
        return !learnersIntroPrefs.hasShownIntro(userId)
    }

    /** Marks the interstitial as seen (so it never shows again for this account+device) regardless of which CTA was tapped. */
    fun markIntroShown(used: Boolean) {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch { learnersIntroPrefs.markIntroShown(userId) }
        analyticsLogger.logLearnersIntroResult(used)
    }
}
