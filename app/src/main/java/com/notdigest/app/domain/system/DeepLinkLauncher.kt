package com.notdigest.app.domain.system

import com.notdigest.app.domain.model.AppNotification

/** Result of attempting to restore the original notification destination. */
enum class LaunchResult {
    /** The original PendingIntent fired — identical to tapping the notification. */
    DEEP_LINKED,
    /** Couldn't restore the exact destination; opened the owning app instead. */
    OPENED_APP,
    /** Nothing launchable was available. */
    FAILED,
}

/**
 * Restores a notification's destination. Tries the preserved live PendingIntent first, then the
 * app's launch intent. See `PendingIntentStore` for the in-memory PendingIntent cache.
 */
interface DeepLinkLauncher {
    fun open(notification: AppNotification): LaunchResult
    fun fireAction(notification: AppNotification, actionIndex: Int): Boolean
}
