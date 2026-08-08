package com.quizmaker.android.util

import com.quizmaker.android.data.model.Profile
import kotlin.time.Clock
import kotlin.time.Instant

private const val TRIAL_DAYS = 7

/** Where a non-premium account stands in its 7-day trial — the single source of truth used by
 *  every screen that shows trial messaging or gates an action. */
sealed class TrialStatus {
    /** Paid plan — no trial concept applies. */
    data object Premium : TrialStatus()
    data class Active(val daysLeft: Int) : TrialStatus()
    data object Expired : TrialStatus()
}

/**
 * 7 days from account creation (`profiles.created_at`). No separate trial-start column exists —
 * see the trial plan doc for why (avoids a Supabase migration; means pre-existing free accounts
 * older than 7 days land straight on Expired the moment this ships, only fresh signups get Active).
 * A missing/unparseable createdAt fails closed to Expired rather than granting an indefinite trial.
 */
fun Profile.trialStatus(now: Instant = Clock.System.now()): TrialStatus {
    if (isPremium) return TrialStatus.Premium
    val start = createdAt ?: return TrialStatus.Expired
    val elapsedDays = (now - start).inWholeDays
    return if (elapsedDays < TRIAL_DAYS) {
        TrialStatus.Active(daysLeft = (TRIAL_DAYS - elapsedDays).toInt())
    } else {
        TrialStatus.Expired
    }
}
