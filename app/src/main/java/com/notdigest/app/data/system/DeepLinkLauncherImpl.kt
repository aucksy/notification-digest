package com.notdigest.app.data.system

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.notdigest.app.domain.model.AppNotification
import com.notdigest.app.domain.system.DeepLinkLauncher
import com.notdigest.app.domain.system.LaunchResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepLinkLauncherImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingIntents: PendingIntentStore,
) : DeepLinkLauncher {

    /**
     * Restores a notification's destination, best-effort:
     *  1. fire the original content [PendingIntent] (exact chat / email / thread), else
     *  2. launch the owning app, else
     *  3. report failure so the UI can show a graceful message.
     */
    override fun open(notification: AppNotification): LaunchResult {
        pendingIntents.contentIntent(notification.sbnKey)?.let { intent ->
            if (sendQuietly(intent)) return LaunchResult.DEEP_LINKED
        }

        val launch = context.packageManager.getLaunchIntentForPackage(notification.packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(launch) }.isSuccess) return LaunchResult.OPENED_APP
        }
        return LaunchResult.FAILED
    }

    override fun fireAction(notification: AppNotification, actionIndex: Int): Boolean {
        val intent = pendingIntents.actionIntent(notification.sbnKey, actionIndex) ?: return false
        return sendQuietly(intent)
    }

    private fun sendQuietly(intent: PendingIntent): Boolean =
        runCatching { intent.send() }.isSuccess
}
