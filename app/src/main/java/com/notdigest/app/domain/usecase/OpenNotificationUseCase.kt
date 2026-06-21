package com.notdigest.app.domain.usecase

import com.notdigest.app.domain.model.AppNotification
import com.notdigest.app.domain.repository.NotificationRepository
import com.notdigest.app.domain.system.DeepLinkLauncher
import com.notdigest.app.domain.system.LaunchResult
import javax.inject.Inject

/**
 * Opens a notification's original destination (deep link) and, by default, marks it read.
 * Opening never deletes — retention is entirely the user's choice.
 */
class OpenNotificationUseCase @Inject constructor(
    private val launcher: DeepLinkLauncher,
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(notification: AppNotification, markRead: Boolean = true): LaunchResult {
        // Launch FIRST — starting the activity as close to the user's tap as possible avoids the
        // Android 14/15 background-launch block that a preceding suspending DB write can trigger.
        val result = launcher.open(notification)
        if (markRead && !notification.isRead) {
            notificationRepository.markRead(listOf(notification.id))
        }
        return result
    }
}
