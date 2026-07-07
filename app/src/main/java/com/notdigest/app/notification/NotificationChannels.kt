package com.notdigest.app.notification

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

        // The keep-alive foreground-service notice. IMPORTANCE_MIN → silent, no status-bar icon,
        // collapsed under "Silent". The user can turn this channel off to hide it entirely (the FGS
        // keeps running regardless).
        val keepAlive = NotificationChannel(
            Constants.CHANNEL_KEEPALIVE,
            context.getString(R.string.channel_keepalive_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = context.getString(R.string.channel_keepalive_desc)
            setShowBadge(false)
            enableVibration(false)
        }

        manager.createNotificationChannels(listOf(digest, recommendation, status, keepAlive))
    }
}
