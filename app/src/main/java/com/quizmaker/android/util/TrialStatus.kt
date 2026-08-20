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
    /** Trial window manually granted by an admin (`profiles.user_type = 'trial_extend'`, reusing
     *  `license_expired_date` as the extension's end date) — full feature access continues, same
     *  as [Active], right up to that date. Kept as its own case (rather than folded into [Active])
     *  so the UI can show a distinct "extended" message instead of the normal trial banner. */
    data class Extended(val daysLeft: Int) : TrialStatus()
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

    extendedTrialEndAt()?.let { end ->
        return if (now < end) {
            TrialStatus.Extended(daysLeft = (end - now).inWholeDays.toInt().coerceAtLeast(0))
        } else {
            TrialStatus.Expired
        }
    }

    val start = createdAt ?: return TrialStatus.Expired
    val elapsedDays = (now - start).inWholeDays
    return if (elapsedDays < TRIAL_DAYS) {
        TrialStatus.Active(daysLeft = (TRIAL_DAYS - elapsedDays).toInt())
    } else {
        TrialStatus.Expired
    }
}

/** Parses `license_expired_date` as the extension's end-of-day cutoff, but only when an admin has
 *  actually opted this account into an extended trial (`user_type = 'trial_extend'`) — for every
 *  other user_type this column means something else (paid-license expiry) or is unset, so it must
 *  never be read as a trial extension by accident. Defaults an unadorned `date` column (no `T`) to
 *  end-of-day UTC so the granted date itself still counts as a full day of access. */
private fun Profile.extendedTrialEndAt(): Instant? {
    if (userType?.lowercase() != "trial_extend") return null
    val raw = licenseExpiredDate?.takeIf { it.isNotBlank() } ?: return null
    val normalized = if (raw.contains("T")) raw else "${raw}T23:59:59Z"
    return runCatching { Instant.parse(normalized) }.getOrNull()
}
