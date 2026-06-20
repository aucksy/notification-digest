package com.notdigest.app.domain.model

/**
 * A single captured notification, independent of any Android framework type.
 *
 * Deep-link preservation note: Android does not allow a [android.app.PendingIntent] to be
 * persisted across process death. We therefore store [sbnKey] (the StatusBarNotification key)
 * and re-fire the live PendingIntent through the running listener when possible, falling back
 * to launching the owning app by [packageName]. See `PendingIntentStore`.
 */
data class AppNotification(
    val id: Long = 0L,
    val sbnKey: String? = null,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val subText: String? = null,
    val category: String? = null,
    val postedAt: Long,
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val digestId: Long? = null,
    val hasDeepLink: Boolean = false,
    val actions: List<NotificationActionItem> = emptyList(),
) {
    /** A short, human-friendly one-line preview used in lists and digests. */
    val preview: String
        get() = when {
            title.isNotBlank() && text.isNotBlank() -> "$title — $text"
            title.isNotBlank() -> title
            else -> text
        }
}

/** A tappable action that was attached to the original notification. */
data class NotificationActionItem(
    val index: Int,
    val title: String,
)
