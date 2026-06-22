package com.notdigest.app.data.system

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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
            if (sendQuietly(intent, launcher)) return LaunchResult.DEEP_LINKED
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
        return sendQuietly(intent, activityHolder.current() ?: context)
    }

    /**
     * Re-fire a captured PendingIntent. On Android 14+ the sender must explicitly grant its
     * background-activity-launch privilege to the target, or the activity is silently dropped while
     * `send()` still reports success — which made recent (in-cache) deep links "do nothing" while
     * older ones fell through to a plain app launch. Passing the foreground Activity as the send
     * context and opting in via ActivityOptions makes the deep link actually land.
     */
    private fun sendQuietly(intent: PendingIntent, launchContext: Context): Boolean = runCatching {
        val options = if (Build.VERSION.SDK_INT >= 34) {
            ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                .toBundle()
        } else {
            null
        }
        intent.send(launchContext, 0, null, null, null, null, options)
    }.isSuccess
}
