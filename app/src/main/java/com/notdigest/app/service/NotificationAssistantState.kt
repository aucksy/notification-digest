package com.notdigest.app.service

import android.content.Context
import android.provider.Settings

/**
 * Best-effort check of whether this app currently holds the system **Notification Assistant** role.
 *
 * Android exposes no public API for this, so we read the (long-standing AOSP) secure setting that
 * stores the active assistant's flattened ComponentName. It can be unreadable on some OEM builds, in
 * which case we simply report "off" — enabling still works, we just can't reflect the status.
 */
object NotificationAssistantState {

    private const val SECURE_KEY = "enabled_notification_assistant"

    fun isGranted(context: Context): Boolean = runCatching {
        val current = Settings.Secure.getString(context.contentResolver, SECURE_KEY)
        !current.isNullOrBlank() && current.contains(context.packageName)
    }.getOrDefault(false)
}
