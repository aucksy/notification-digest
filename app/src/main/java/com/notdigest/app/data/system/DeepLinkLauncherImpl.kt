package com.notdigest.app.data.system

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
    private val activityHolder: CurrentActivityHolder,
) : DeepLinkLauncher {

    /**
     * Restores a notification's destination, best-effort:
     *  1. fire the original content [PendingIntent] (exact chat / email / thread), else
     *  2. launch the owning app, else
     *  3. open the app's info page (apps with no launchable screen, e.g. Phone/Telecom), else
     *  4. report failure so the UI can show a graceful message.
     *
     * Activities are started from the foreground Activity when available — under Android 14/15 launch
     * rules, an app-context startActivity can be silently blocked even while the app is visible.
     */
    override fun open(notification: AppNotification): LaunchResult {
        val launcher = activityHolder.current() ?: context

        pendingIntents.contentIntent(notification.sbnKey)?.let { intent ->
            if (sendQuietly(intent)) return LaunchResult.DEEP_LINKED
        }

        val launch = context.packageManager.getLaunchIntentForPackage(notification.packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { launcher.startActivity(launch) }.isSuccess) return LaunchResult.OPENED_APP
        }

        // Some notifications come from packages with no launchable activity — e.g. the Phone /
        // Telecom "call blocked" notice. Land the user on that app's system page so the tap still
        // does something useful rather than silently failing.
        val settings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", notification.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (runCatching { launcher.startActivity(settings) }.isSuccess) return LaunchResult.OPENED_SETTINGS

        return LaunchResult.FAILED
    }

    override fun fireAction(notification: AppNotification, actionIndex: Int): Boolean {
        val intent = pendingIntents.actionIntent(notification.sbnKey, actionIndex) ?: return false
        return sendQuietly(intent)
    }

    private fun sendQuietly(intent: PendingIntent): Boolean =
        runCatching { intent.send() }.isSuccess
}
