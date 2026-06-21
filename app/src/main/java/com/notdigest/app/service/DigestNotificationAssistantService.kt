package com.notdigest.app.service

import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.service.notification.Adjustment
import android.service.notification.NotificationAssistantService
import android.service.notification.StatusBarNotification
import com.notdigest.app.domain.model.DigestMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Optional companion to [DigestNotificationListenerService] that the user can enable by making this
 * app the system **Notification Assistant**.
 *
 * A plain notification listener only sees a notification *after* it's posted — by then the alert
 * sound has already played, which is why a Digest app can still make a brief noise before it's tucked
 * away. The assistant role is the only hook that runs *before* posting ([onNotificationEnqueued]).
 * Here we demote Digest-mode apps to [NotificationManager.IMPORTANCE_LOW] so they arrive silently;
 * the listener then captures and removes them. Real-Time / critical apps and non-batchable cases
 * (calls, media, ongoing) are never touched, so they alert normally.
 *
 * Only one app can hold the assistant role, so enabling this replaces the system default assistant.
 * It does nothing unless the user opts in via Settings.
 */
@AndroidEntryPoint
class DigestNotificationAssistantService : NotificationAssistantService() {

    @Inject lateinit var modeCache: ModeCache

    override fun onNotificationEnqueued(sbn: StatusBarNotification): Adjustment? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        if (sbn.packageName == packageName) return null
        if (modeCache.cachedMode(sbn.packageName) != DigestMode.DIGEST) return null
        if (isNeverSilence(sbn)) return null

        val signals = Bundle().apply {
            putInt(Adjustment.KEY_IMPORTANCE, NotificationManager.IMPORTANCE_LOW)
        }
        return runCatching {
            Adjustment(sbn.packageName, sbn.key, signals, SILENCE_REASON, sbn.user)
        }.getOrNull()
    }

    /** Capture/suppression is the listener's job; the assistant only silences on enqueue. */
    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap?) = Unit

    override fun onNotificationSnoozedUntilContext(sbn: StatusBarNotification, snoozeCriterionId: String) = Unit

    /** Mirror the listener's "never batch" cases so we don't mute a call, media or ongoing notice. */
    private fun isNeverSilence(sbn: StatusBarNotification): Boolean {
        if (!sbn.isClearable) return true
        val flags = sbn.notification.flags
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return true
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return true
        return when (sbn.notification.category) {
            Notification.CATEGORY_CALL,
            Notification.CATEGORY_TRANSPORT,
            Notification.CATEGORY_NAVIGATION,
            Notification.CATEGORY_SERVICE,
            -> true
            else -> false
        }
    }

    private companion object {
        const val SILENCE_REASON = "Digest app — delivered silently"
    }
}
