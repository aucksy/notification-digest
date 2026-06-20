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
        if (markRead && !notification.isRead) {
            notificationRepository.markRead(listOf(notification.id))
        }
        return launcher.open(notification)
    }
}
