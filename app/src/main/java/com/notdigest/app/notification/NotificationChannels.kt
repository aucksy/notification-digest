package com.notdigest.app.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.notdigest.app.R
import com.notdigest.app.core.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Creates the app's notification channels. minSdk 26 guarantees channels are available. */
@Singleton
class NotificationChannels @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun ensureChannels() {
        val digest = NotificationChannel(
            Constants.CHANNEL_DIGEST,
            context.getString(R.string.channel_digest_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_digest_desc)
            enableVibration(true)
        }

        val recommendation = NotificationChannel(
            Constants.CHANNEL_RECOMMENDATION,
            context.getString(R.string.channel_recommendation_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_recommendation_desc)
            setShowBadge(false)
        }

        val status = NotificationChannel(
            Constants.CHANNEL_STATUS,
            context.getString(R.string.channel_status_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = context.getString(R.string.channel_status_desc)
            setShowBadge(false)
            enableVibration(false)
        }

        // The keep-alive foreground-service notice, made as invisible as Android allows while the app
        // can post notifications: IMPORTANCE_MIN (silent, no status-bar icon) + VISIBILITY_SECRET
        // (never on the lockscreen). All that remains is a collapsed line in the pulled-down shade's
        // "Silent" section — so it never prompts the user to check for a digest.
        val keepAlive = NotificationChannel(
            Constants.CHANNEL_KEEPALIVE,
            context.getString(R.string.channel_keepalive_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = context.getString(R.string.channel_keepalive_desc)
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }

        // Drop the pre-v1.1.28 keep-alive channel (its lockscreen visibility can't be changed in place).
        manager.deleteNotificationChannel(Constants.CHANNEL_KEEPALIVE_OLD)

        manager.createNotificationChannels(listOf(digest, recommendation, status, keepAlive))
    }
}
