package com.quizmaker.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService
import com.quizmaker.android.core.messaging.NOTIFICATION_CHANNEL_ID
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuizMakerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
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
}
