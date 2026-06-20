package com.notdigest.app.notification

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.notdigest.app.R
import com.notdigest.app.core.Constants
import com.notdigest.app.domain.model.DigestWithItems
import com.notdigest.app.domain.system.DigestNotifier
import com.notdigest.app.ui.navigation.NavRoutes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DigestNotifierImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val channels: NotificationChannels,
) : DigestNotifier {

    private val manager = NotificationManagerCompat.from(context)
    private val accent = 0xFF6E56CF.toInt()

    @SuppressLint("MissingPermission")
    override fun postDigest(digest: DigestWithItems) {
        channels.ensureChannels()
        if (!manager.areNotificationsEnabled()) return

        val total = digest.digest.notificationCount
        val appCount = digest.digest.appCount
        val title = context.getString(R.string.digest_ready_title)
        val summary = context.resources.getQuantityString(
            R.plurals.digest_summary, total, total, appCount,
        )

        val style = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText(summary)

        digest.groups.take(MAX_LINES).forEach { group ->
            val preview = group.notifications.firstOrNull()?.preview.orEmpty()
            style.addLine("${group.appName} (${group.count})  $preview")
        }
        if (digest.groups.size > MAX_LINES) {
            style.addLine("+${digest.groups.size - MAX_LINES} more apps")
        }

        val open = NotificationDeepLinks.openRoute(
            context, NavRoutes.digestDetail(digest.digest.id),
        )

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_DIGEST)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(style)
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.digest_action_review), open)
            .setColor(accent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notifId = Constants.NOTIF_ID_DIGEST_BASE + (digest.digest.id % 100).toInt()
        manager.notify(notifId, notification)
    }

    @SuppressLint("MissingPermission")
    override fun showCollectingStatus(pendingCount: Int) {
        channels.ensureChannels()
        if (!manager.areNotificationsEnabled() || pendingCount <= 0) {
            clearCollectingStatus()
            return
        }
        val open = NotificationDeepLinks.openRoute(context, NavRoutes.INBOX)
        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                context.resources.getQuantityString(
                    R.plurals.status_collecting, pendingCount, pendingCount,
                ),
            )
            .setContentText(context.getString(R.string.status_collecting_sub))
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.status_action_deliver), NotificationDeepLinks.deliverNow(context))
            .setColor(accent)
            .setOngoing(true)
            .setSilent(true)
            // The count updates on every suppression; these keep it from re-alerting or jumping to
            // the top of the shade each time — it just quietly reflects the new number.
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        manager.notify(Constants.NOTIF_ID_STATUS, notification)
    }

    override fun clearCollectingStatus() {
        manager.cancel(Constants.NOTIF_ID_STATUS)
    }

    private companion object {
        const val MAX_LINES = 7
    }
}
