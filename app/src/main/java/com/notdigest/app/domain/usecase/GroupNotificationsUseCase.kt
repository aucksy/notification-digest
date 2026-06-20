package com.notdigest.app.domain.usecase

import com.notdigest.app.domain.model.AppGroup
import com.notdigest.app.domain.model.AppNotification
import javax.inject.Inject

/**
 * Groups a flat notification list by app, sorting items newest-first within each group and
 * groups by their most recent notification. Used by the inbox, digests, and history.
 */
class GroupNotificationsUseCase @Inject constructor() {

    operator fun invoke(notifications: List<AppNotification>): List<AppGroup> =
        notifications
            .groupBy { it.packageName }
            .map { (pkg, items) ->
                val sorted = items.sortedByDescending { it.postedAt }
                AppGroup(
                    packageName = pkg,
                    appName = sorted.firstOrNull()?.appName ?: pkg,
                    notifications = sorted,
                )
            }
            .sortedWith(compareByDescending<AppGroup> { it.latestAt }.thenBy { it.appName.lowercase() })
}
