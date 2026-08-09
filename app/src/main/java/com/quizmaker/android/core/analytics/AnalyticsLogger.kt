package com.quizmaker.android.core.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.posthog.PostHog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin, typed wrapper that fans every event out to both FirebaseAnalytics and PostHog, so call
 * sites never touch either SDK's raw event-name strings/property maps directly — this is the one
 * place to see every event the app sends, and the one place to fix a typo'd event/param name
 * instead of hunting through every ViewModel that logs something. Event names are kept identical
 * across both platforms wherever possible (using Firebase's own standard event/param constants,
 * which happen to already be the plain lowercase strings PostHog convention also expects — e.g.
 * "sign_up", "login", "purchase", "screen_view") so the two dashboards describe the same funnel
 * rather than two different vocabularies. PostHog silently no-ops if it was never initialized
 * (POSTHOG_API_KEY unset in local.properties — see QuizMakerApp.setupPostHog()).
 */
@Singleton
class AnalyticsLogger @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) {
    /** Ties all subsequent events to this account across sessions/devices. Call with null on sign-out. */
    fun setUserId(userId: String?) {
        firebaseAnalytics.setUserId(userId)
        if (userId != null) PostHog.identify(distinctId = userId) else PostHog.reset()
    }

    fun setPremiumStatus(isPremium: Boolean) {
        firebaseAnalytics.setUserProperty("is_premium", isPremium.toString())
        PostHog.capture(
            event = "premium_status_updated",
            userProperties = mapOf("is_premium" to isPremium)
        )
    }

    /** NavGraph.kt calls this on every route change — one spot covers every screen instead of each screen self-reporting. */
    fun logScreenView(screenName: String) {
        firebaseAnalytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply { putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName) }
        )
        PostHog.screen(screenTitle = screenName)
    }

    fun logSignUp() {
        firebaseAnalytics.logEvent(
            FirebaseAnalytics.Event.SIGN_UP,
            Bundle().apply { putString(FirebaseAnalytics.Param.METHOD, "email") }
        )
        PostHog.capture(event = "sign_up", properties = mapOf("method" to "email"))
    }

    fun logLogin(method: String = "email") {
        firebaseAnalytics.logEvent(
            FirebaseAnalytics.Event.LOGIN,
            Bundle().apply { putString(FirebaseAnalytics.Param.METHOD, method) }
        )
        PostHog.capture(event = "login", properties = mapOf("method" to method))
    }

    fun logPhoneCollected() {
        firebaseAnalytics.logEvent("phone_number_collected", null)
        PostHog.capture(event = "phone_number_collected")
    }

    fun logTrialStarted() {
        firebaseAnalytics.logEvent("trial_started", null)
        PostHog.capture(event = "trial_started")
    }

    fun logNotificationPermissionResult(granted: Boolean) {
        firebaseAnalytics.logEvent(
            "notification_permission_result",
            Bundle().apply { putString("result", if (granted) "granted" else "denied") }
        )
        PostHog.capture(
            event = "notification_permission_result",
            properties = mapOf("result" to if (granted) "granted" else "denied")
        )
    }

    fun logNotificationPermissionSkipped() {
        firebaseAnalytics.logEvent("notification_permission_skipped", null)
        PostHog.capture(event = "notification_permission_skipped")
    }

    fun logQuizCreated(quizId: String, questionCount: Int, source: String) {
        firebaseAnalytics.logEvent(
            "quiz_created",
            Bundle().apply {
                putString("quiz_id", quizId)
                putInt("question_count", questionCount)
                putString("source", source)
            }
        )
        PostHog.capture(
            event = "quiz_created",
            properties = mapOf("quiz_id" to quizId, "question_count" to questionCount, "source" to source)
        )
    }

    /** Fired when a learner submits — the event that makes the notification-permission ask meaningful. */
    fun logQuizSubmitted(quizId: String, scorePercent: Int) {
        firebaseAnalytics.logEvent(
            "quiz_submitted",
            Bundle().apply {
                putString("quiz_id", quizId)
                putInt("score_percent", scorePercent)
            }
        )
        PostHog.capture(
            event = "quiz_submitted",
            properties = mapOf("quiz_id" to quizId, "score_percent" to scorePercent)
        )
    }

    fun logPlansViewed(saleActive: Boolean) {
        firebaseAnalytics.logEvent(
            "plans_viewed",
            Bundle().apply { putBoolean("sale_active", saleActive) }
        )
        PostHog.capture(event = "plans_viewed", properties = mapOf("sale_active" to saleActive))
    }

    fun logSalePriceActivated(discountPercent: Int) {
        firebaseAnalytics.logEvent(
            "sale_price_activated",
            Bundle().apply { putInt("discount_percent", discountPercent) }
        )
        PostHog.capture(event = "sale_price_activated", properties = mapOf("discount_percent" to discountPercent))
    }

    /** [amountMajor] is the display amount actually charged (already discounted, if a sale was active). */
    fun logPurchase(amountMajor: Int, currency: String, planLabel: String) {
        firebaseAnalytics.logEvent(
            FirebaseAnalytics.Event.PURCHASE,
            Bundle().apply {
                putDouble(FirebaseAnalytics.Param.VALUE, amountMajor.toDouble())
                putString(FirebaseAnalytics.Param.CURRENCY, currency)
                putString(FirebaseAnalytics.Param.ITEM_NAME, planLabel)
            }
        )
        PostHog.capture(
            event = "purchase",
            properties = mapOf("value" to amountMajor, "currency" to currency, "item_name" to planLabel)
        )
    }
}
