package com.quizmaker.android.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.prefs.ProfileSummaryCache
import com.quizmaker.android.data.model.SaleDay
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.SaleDayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

data class MoreUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val email: String = "",
    val isPremium: Boolean = false,
    val licenseExpiredDate: String? = null,
    /** Non-null while a `sale_day` window is currently live — drives the More screen's sale styling. */
    val activeSale: SaleDay? = null
)

/**
 * Caches the last loaded profile summary for the process's lifetime. MoreViewModel can be
 * re-created on tab switches within the same session; without this, every fresh instance briefly
 * renders MoreUiState's default isPremium=false before the async profile fetch resolves — which
 * for an actual premium account showed the gold "Get Premium" banner for a beat before it flipped
 * to the real "Premium Active" one (a visible ~2s "blink" whenever any More row was tapped and the
 * screen was revisited). Seeding from the cache renders the correct banner immediately.
 */
@Singleton
class MoreStateCache @Inject constructor() {
    var lastState: MoreUiState? = null
}

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val stateCache: MoreStateCache,
    private val profileSummaryCache: ProfileSummaryCache,
    private val saleDayRepository: SaleDayRepository
) : ViewModel() {

    private val userId = authRepository.currentUserId()

    // Two layers: stateCache survives ViewModel re-creation within the same process (instant);
    // profileSummaryCache (SharedPreferences) survives an app restart too — without it, every
    // fresh process/test run hit the ~1-2s network gap again, showing the wrong (or no) banner
    // until the real fetch below resolved.
    private val _uiState = MutableStateFlow(
        stateCache.lastState?.copy(isLoading = false)
            ?: userId?.let(profileSummaryCache::read)?.let { cached ->
                MoreUiState(
                    isLoading = false,
                    name = cached.name,
                    email = cached.email,
                    isPremium = cached.isPremium,
                    licenseExpiredDate = cached.licenseExpiredDate
                )
            }
            ?: MoreUiState()
    )
    val uiState: StateFlow<MoreUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val now = Clock.System.now()
            val activeSale = (saleDayRepository.getSaleDays() as? AppResult.Success)?.data?.firstOrNull { sale ->
                val start = sale.startedAt
                val end = sale.endAt
                start != null && end != null && now >= start && now <= end
            }

            when (val result = authRepository.getCurrentProfile()) {
                is AppResult.Success -> {
                    val newState = MoreUiState(
                        isLoading = false,
                        name = result.data.name,
                        email = result.data.email,
                        isPremium = result.data.isPremium,
                        licenseExpiredDate = result.data.licenseExpiredDate,
                        activeSale = activeSale
                    )
                    _uiState.value = newState
                    stateCache.lastState = newState
                    userId?.let {
                        profileSummaryCache.write(
                            it,
                            ProfileSummaryCache.Snapshot(newState.name, newState.email, newState.isPremium, newState.licenseExpiredDate)
                        )
                    }
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, activeSale = activeSale)
            }
        }
    }
}
