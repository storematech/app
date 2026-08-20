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
    /**
     * Ties all subsequent events to this account across sessions/devices. Call with null on sign-out.
     * [email] is attached as a PostHog person property only — Firebase Analytics' terms disallow
     * logging PII (like email) as a user ID/property, so it's deliberately left out of that call.
     */
    fun setUserId(userId: String?, email: String? = null) {
        firebaseAnalytics.setUserId(userId)
        if (userId != null) {
            PostHog.identify(
                distinctId = userId,
                userProperties = email?.let { mapOf("email" to it) }
            )
        } else {
            PostHog.reset()
        }
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

    fun logClassCreated() {
        firebaseAnalytics.logEvent("class_created", null)
        PostHog.capture(event = "class_created")
    }

    fun logLearnerCreated() {
        firebaseAnalytics.logEvent("learner_created", null)
        PostHog.capture(event = "learner_created")
    }

    fun logLearnerDeleted() {
        firebaseAnalytics.logEvent("learner_deleted", null)
        PostHog.capture(event = "learner_deleted")
    }

    fun logGroupCreated() {
        firebaseAnalytics.logEvent("group_created", null)
        PostHog.capture(event = "group_created")
    }

    fun logQuizDeleted() {
        firebaseAnalytics.logEvent("quiz_deleted", null)
        PostHog.capture(event = "quiz_deleted")
    }

    fun logQuizDuplicated() {
        firebaseAnalytics.logEvent("quiz_duplicated", null)
        PostHog.capture(event = "quiz_duplicated")
    }

    /** A quiz being closed stops it accepting new responses — a meaningful lifecycle change, distinct from deletion. */
    fun logQuizClosed() {
        firebaseAnalytics.logEvent("quiz_closed", null)
        PostHog.capture(event = "quiz_closed")
    }

    /** Question Bank deletion — a reusable bank question, not a per-quiz question edit (those aren't tracked individually). */
    fun logQuestionDeleted() {
        firebaseAnalytics.logEvent("question_deleted", null)
        PostHog.capture(event = "question_deleted")
    }

    /** [source] is "prompt" | "pdf" | "images" — which AI Quiz entry point was used. */
    fun logAiQuizGenerated(source: String, questionCount: Int) {
        firebaseAnalytics.logEvent(
            "ai_quiz_generated",
            Bundle().apply {
                putString("source", source)
                putInt("question_count", questionCount)
            }
        )
        PostHog.capture(
            event = "ai_quiz_generated",
            properties = mapOf("source" to source, "question_count" to questionCount)
        )
    }

    fun logQuestionsImported(count: Int) {
        firebaseAnalytics.logEvent(
            "questions_imported",
            Bundle().apply { putInt("count", count) }
        )
        PostHog.capture(event = "questions_imported", properties = mapOf("count" to count))
    }

    /** [toolType] is "poll" | "rsvp" | "voting" | "feedback" | "onboarding" — shared across all five Tools list ViewModels rather than five near-identical methods. */
    fun logToolCreated(toolType: String) {
        firebaseAnalytics.logEvent(
            "tool_created",
            Bundle().apply { putString("tool_type", toolType) }
        )
        PostHog.capture(event = "tool_created", properties = mapOf("tool_type" to toolType))
    }

    fun logToolDeleted(toolType: String) {
        firebaseAnalytics.logEvent(
            "tool_deleted",
            Bundle().apply { putString("tool_type", toolType) }
        )
        PostHog.capture(event = "tool_deleted", properties = mapOf("tool_type" to toolType))
    }

    fun logToolActiveToggled(toolType: String, isActive: Boolean) {
        firebaseAnalytics.logEvent(
            "tool_active_toggled",
            Bundle().apply {
                putString("tool_type", toolType)
                putBoolean("is_active", isActive)
            }
        )
        PostHog.capture(
            event = "tool_active_toggled",
            properties = mapOf("tool_type" to toolType, "is_active" to isActive)
        )
    }

    /** Outcome of the one-time "what Tools can do" interstitial shown on first opening More → Tools — see ToolsIntroScreen. */
    fun logToolsIntroResult(used: Boolean) {
        firebaseAnalytics.logEvent(
            "tools_intro_result",
            Bundle().apply { putString("result", if (used) "used" else "skipped") }
        )
        PostHog.capture(
            event = "tools_intro_result",
            properties = mapOf("result" to if (used) "used" else "skipped")
        )
    }

    /** Outcome of the one-time "what Learners can do" interstitial shown on first opening More → Learners — see LearnersIntroScreen. */
    fun logLearnersIntroResult(used: Boolean) {
        firebaseAnalytics.logEvent(
            "learners_intro_result",
            Bundle().apply { putString("result", if (used) "used" else "skipped") }
        )
        PostHog.capture(
            event = "learners_intro_result",
            properties = mapOf("result" to if (used) "used" else "skipped")
        )
    }

    /** Outcome of the one-time "what Classes can do" interstitial shown on first opening More → Classes — see ClassesIntroScreen. */
    fun logClassesIntroResult(used: Boolean) {
        firebaseAnalytics.logEvent(
            "classes_intro_result",
            Bundle().apply { putString("result", if (used) "used" else "skipped") }
        )
        PostHog.capture(
            event = "classes_intro_result",
            properties = mapOf("result" to if (used) "used" else "skipped")
        )
    }

    /** [channel] is "link" | "qr_code" | "pdf_flyer" — the three ways to distribute a quiz from the QuizCreated screen. */
    fun logQuizShared(channel: String) {
        firebaseAnalytics.logEvent(
            FirebaseAnalytics.Event.SHARE,
            Bundle().apply { putString("channel", channel) }
        )
        PostHog.capture(event = "share", properties = mapOf("channel" to channel))
    }
}
