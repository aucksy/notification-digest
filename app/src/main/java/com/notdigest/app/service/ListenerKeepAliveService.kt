package com.notdigest.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import androidx.core.content.ContextCompat
import com.notdigest.app.MainActivity
import com.notdigest.app.R
import com.notdigest.app.core.Constants

/**
 * The "keep app always running" mode. This service does no work of its own — its only purpose is to be
 * a foreground service so the OS keeps our process alive, which in turn keeps the system-bound
 * [DigestNotificationListenerService] connected. Aggressive OEM battery management otherwise unbinds the
 * listener in the background, so notifications posted while it's dead slip through until the app is
 * opened. Pinning the process is the definitive fix short of asking the user to fight their OEM settings.
 *
 * The notice is on an IMPORTANCE_MIN channel: silent, no status-bar icon, collapsed under "Silent",
 * and hide-able by the user (turning off the channel doesn't stop the service). It appears only because
 * the app holds POST_NOTIFICATIONS for delivering digests; an app without it would show nothing.
 *
 * Started from a foreground context (the app UI / a boot broadcast) and kept alive by START_STICKY, so
 * the system re-creates it after a low-memory kill. Modelled on Pause's UsageAccessMonitorService.
 */
class ListenerKeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If we can't go foreground (e.g. the START landed while backgrounded without an exemption on
        // Android 12+, or a notification-permission edge), stand down instead of crashing — the system
        // requires startForeground() within 5s of startForegroundService(), so a throw here would
        // otherwise take down the app. A later foreground pass (AppViewModel / BootReceiver) retries.
        if (runCatching { promoteToForeground() }.isFailure) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Belt-and-suspenders: nudge the listener to bind in case it was unbound before we came up.
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(this, DigestNotificationListenerService::class.java),
            )
        }
        return START_STICKY
    }

    private fun promoteToForeground() {
        // Defensive: the channel is normally created at app start, but ensure it here too so a
        // START_STICKY re-create can never call startForeground on a missing channel. Idempotent.
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(Constants.CHANNEL_KEEPALIVE) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    Constants.CHANNEL_KEEPALIVE,
                    getString(R.string.channel_keepalive_name),
                    NotificationManager.IMPORTANCE_MIN,
                ).apply {
                    description = getString(R.string.channel_keepalive_desc)
                    setShowBadge(false)
                    enableVibration(false)
                },
            )
        }

        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = Notification.Builder(this, Constants.CHANNEL_KEEPALIVE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.keepalive_title))
            .setContentText(getString(R.string.keepalive_text))
            .setOngoing(true)
            .setContentIntent(tapIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(Constants.NOTIF_ID_KEEPALIVE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Constants.NOTIF_ID_KEEPALIVE, notification)
        }
    }

    companion object {
        /**
         * Start or stop the keep-alive service to match [shouldRun] (enabled by the user AND notification
         * access granted). Safe to call from any foreground context; the start is wrapped so the rare
         * background-start restriction (Android 12+) can't crash the caller — START_STICKY + the next
         * foreground pass will bring it up.
         */
        fun sync(context: Context, shouldRun: Boolean) {
            val intent = Intent(context, ListenerKeepAliveService::class.java)
            if (shouldRun) {
                runCatching { ContextCompat.startForegroundService(context, intent) }
            } else {
                runCatching { context.stopService(intent) }
            }
        }
    }
}
