package com.notdigest.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.notdigest.app.core.util.TimeProvider
import com.notdigest.app.domain.model.AppNotification
import com.notdigest.app.domain.model.DigestType
import com.notdigest.app.domain.repository.DigestRepository
import com.notdigest.app.domain.repository.NotificationRepository
import com.notdigest.app.domain.system.DigestNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeliverDigestUseCaseTest {

    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val digestRepository = mockk<DigestRepository>(relaxed = true)
    private val notifier = mockk<DigestNotifier>(relaxed = true)
    private val time = mockk<TimeProvider>(relaxed = true).apply { every { now() } returns 1_000L }

    private val useCase = DeliverDigestUseCase(
        notificationRepository = notificationRepository,
        digestRepository = digestRepository,
        groupNotifications = GroupNotificationsUseCase(),
        notifier = notifier,
        time = time,
    )

    private fun notif(id: Long, pkg: String) = AppNotification(
        id = id, packageName = pkg, appName = pkg, title = "t$id", text = "b$id", postedAt = id,
    )

    @Test
    fun `delivers, links notifications to the new digest and posts it`() = runTest {
        coEvery { notificationRepository.pendingSnapshot() } returns listOf(notif(1, "com.a"), notif(2, "com.a"), notif(3, "com.b"))
        coEvery { digestRepository.createDigest(any(), any(), any(), any()) } returns 42L

        val result = useCase(DigestType.MANUAL)

        assertThat(result).isInstanceOf(DeliverResult.Delivered::class.java)
        result as DeliverResult.Delivered
        assertThat(result.digestId).isEqualTo(42L)
        assertThat(result.notificationCount).isEqualTo(3)
        assertThat(result.appCount).isEqualTo(2)

        coVerify { notificationRepository.assignToDigest(listOf(1L, 2L, 3L), 42L, 1_000L) }
        verify { notifier.postDigest(any()) }
    }

    @Test
    fun `does nothing when there is nothing pending`() = runTest {
        coEvery { notificationRepository.pendingSnapshot() } returns emptyList()

        val result = useCase(DigestType.SCHEDULED)

        assertThat(result).isEqualTo(DeliverResult.Empty)
        coVerify(exactly = 0) { digestRepository.createDigest(any(), any(), any(), any()) }
        verify(exactly = 0) { notifier.postDigest(any()) }
    }
}
