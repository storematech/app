package com.quizmaker.android.core.analytics

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Lets NavGraph.kt (a plain Composable, not itself Hilt-injectable) reach the singleton
 * [AnalyticsLogger] the same way it already reaches SessionViewModel — via hiltViewModel().
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    fun logScreenView(screenName: String) = analyticsLogger.logScreenView(screenName)
}
