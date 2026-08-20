package com.quizmaker.android.core.messaging

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.quizmaker.android.MainActivity
import com.quizmaker.android.R
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.ProfileRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

const val NOTIFICATION_CHANNEL_ID = "quiz_submissions"

/** Lifecycle/engagement pushes from scheduled-push (signup nudges, trial reminders) — kept
 *  separate from [NOTIFICATION_CHANNEL_ID] so they don't show up mislabeled under a channel
 *  named/described as being about quiz submissions in the user's Android notification settings. */
const val LIFECYCLE_NOTIFICATION_CHANNEL_ID = "lifecycle_tips"

/** Read by NavGraph on cold start / onNewIntent to deep-link straight to the response that was submitted. */
const val EXTRA_RESPONSE_ID = "response_id"

/**
 * Messages are sent data-only (no "notification" payload key) by both notify-quiz-submission and
 * scheduled-push specifically so onMessageReceived always fires here — foreground, background, or
 * app killed — and we build the notification ourselves with a tap target, rather than relying on
 * the OS's default handling (which skips onMessageReceived for background/killed "notification"
 * messages).
 */
@AndroidEntryPoint
class QuizFcmService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var profileRepository: ProfileRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userId = authRepository.currentUserId() ?: return
        scope.launch { profileRepository.updateFcmToken(userId, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // The user may never have granted POST_NOTIFICATIONS (asked once from Dashboard); silently
        // skip rather than crash — a Service can't prompt for permission itself.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val data = message.data
        // scheduled-push sets type="lifecycle"; notify-quiz-submission sends no type at all, which
        // is what routes its messages to the original channel/default title unchanged.
        val isLifecycle = data["type"] == "lifecycle"
        val channelId = if (isLifecycle) LIFECYCLE_NOTIFICATION_CHANNEL_ID else NOTIFICATION_CHANNEL_ID
        val title = data["title"]?.ifBlank { null } ?: if (isLifecycle) "YUNO LMS" else "New quiz submission"
        val body = data["body"].orEmpty()
        val responseId = data["response_id"]

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (responseId != null) putExtra(EXTRA_RESPONSE_ID, responseId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            responseId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_splash_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(this)
            .notify(responseId?.hashCode() ?: System.currentTimeMillis().toInt(), notification)
    }
}
