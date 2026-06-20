package com.notdigest.app.domain.model

/** How a digest batch was produced. */
enum class DigestType { SCHEDULED, MANUAL }

/** A delivered batch of notifications. */
data class Digest(
    val id: Long = 0L,
    val createdAt: Long,
    val type: DigestType,
    val notificationCount: Int,
    val appCount: Int,
)

/** Notifications belonging to a single app within a digest or the inbox. */
data class AppGroup(
    val packageName: String,
    val appName: String,
    val notifications: List<AppNotification>,
) {
    val count: Int get() = notifications.size
    val latestAt: Long get() = notifications.maxOfOrNull { it.postedAt } ?: 0L
    val unreadCount: Int get() = notifications.count { !it.isRead }
}

/** A digest together with its notifications, grouped by app. */
data class DigestWithItems(
    val digest: Digest,
    val groups: List<AppGroup>,
) {
    val notifications: List<AppNotification> get() = groups.flatMap { it.notifications }
}
