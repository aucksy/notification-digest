package com.notdigest.app.domain.usecase

import com.notdigest.app.core.util.TimeProvider
import com.notdigest.app.domain.model.Digest
import com.notdigest.app.domain.model.DigestType
import com.notdigest.app.domain.model.DigestWithItems
import com.notdigest.app.domain.repository.DigestRepository
import com.notdigest.app.domain.repository.NotificationRepository
import com.notdigest.app.domain.system.DigestNotifier
import javax.inject.Inject

sealed interface DeliverResult {
    /** Nothing was pending; no digest was created. */
    data object Empty : DeliverResult
    data class Delivered(val digestId: Long, val notificationCount: Int, val appCount: Int) : DeliverResult
}

/**
 * The heart of "Deliver Now" and every scheduled delivery.
 *
 * Snapshots the pending queue, persists a digest, links the notifications to it (marking them
 * delivered — never deleting them), and posts the grouped digest notification. The queue is thus
 * "cleared" from the inbox's perspective while everything stays available in History.
 */
class DeliverDigestUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val digestRepository: DigestRepository,
    private val groupNotifications: GroupNotificationsUseCase,
    private val notifier: DigestNotifier,
    private val time: TimeProvider,
) {
    /**
     * @param postNotification when false (in-app "See All Notifications Now"), the digest is created
     * and revealed in the inbox WITHOUT posting a system notification — the user is already looking
     * at the app, so re-notifying would be noise.
     */
    suspend operator fun invoke(type: DigestType, postNotification: Boolean = true): DeliverResult {
        val pending = notificationRepository.pendingSnapshot()
        if (pending.isEmpty()) return DeliverResult.Empty

        val now = time.now()
        val groups = groupNotifications(pending)
        // Create the digest and link its notifications atomically — otherwise the digest briefly exists
        // with no linked rows, and the retention worker's deleteEmptyDigests() (running concurrently on
        // another thread) could delete it, orphaning the notifications it's about to claim.
        val digestId = digestRepository.createDigestWithAssignment(
            type = type,
            createdAt = now,
            notificationCount = pending.size,
            appCount = groups.size,
            notificationIds = pending.map { it.id },
        )

        if (postNotification) {
            notifier.postDigest(
                DigestWithItems(
                    digest = Digest(digestId, now, type, pending.size, groups.size),
                    groups = groups,
                ),
            )
        }
        notifier.clearCollectingStatus()

        return DeliverResult.Delivered(digestId, pending.size, groups.size)
    }
}
