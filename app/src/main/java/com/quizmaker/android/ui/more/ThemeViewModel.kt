package com.quizmaker.android.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.prefs.AppThemeMode
import com.quizmaker.android.core.prefs.ThemePrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePrefs: ThemePrefs
) : ViewModel() {

    val themeMode: StateFlow<AppThemeMode> = themePrefs.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppThemeMode.SYSTEM)

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { themePrefs.setThemeMode(mode) }
    }
}
