package com.quizmaker.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.quizmaker.android.core.messaging.NOTIFICATION_CHANNEL_ID
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuizMakerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupPostHog()
    }

    // Required on API 26+ before any notification can be posted; QuizFcmService posts to this channel.
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Quiz submissions",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notified when someone submits one of your quizzes"
        }
        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    /**
     * No-ops (rather than crashing) when POSTHOG_API_KEY isn't set in local.properties — same
     * "gracefully unconfigured" treatment as Razorpay's key. Screen views are sent manually from
     * NavGraph.kt (alongside the same Firebase screen_view calls) instead of relying on PostHog's
     * own Activity-based auto-tracking, which would only ever fire once for this single-Activity
     * Compose app rather than per screen.
     */
    private fun setupPostHog() {
        if (BuildConfig.POSTHOG_API_KEY.isBlank()) return
        val config = PostHogAndroidConfig(
            apiKey = BuildConfig.POSTHOG_API_KEY,
            host = BuildConfig.POSTHOG_HOST
        ).apply {
            captureScreenViews = false
            debug = BuildConfig.DEBUG
        }
        PostHogAndroid.setup(this, config)
    }
}
