package com.notdigest.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.notdigest.app.domain.model.AppNotification
import org.junit.Test

class GroupNotificationsUseCaseTest {

    private val useCase = GroupNotificationsUseCase()

    private fun notif(pkg: String, app: String, postedAt: Long, id: Long) = AppNotification(
        id = id,
        packageName = pkg,
        appName = app,
        title = "T$id",
        text = "Body $id",
        postedAt = postedAt,
    )

    @Test
    fun `groups by package and orders items newest first`() {
        val groups = useCase(
            listOf(
                notif("com.a", "Alpha", postedAt = 100, id = 1),
                notif("com.a", "Alpha", postedAt = 300, id = 2),
                notif("com.b", "Beta", postedAt = 200, id = 3),
            ),
        )
        assertThat(groups).hasSize(2)
        // Group with the most recent notification comes first (com.a @300).
        assertThat(groups.first().packageName).isEqualTo("com.a")
        assertThat(groups.first().notifications.map { it.id }).containsExactly(2L, 1L).inOrder()
        assertThat(groups.first().count).isEqualTo(2)
    }

    @Test
    fun `empty input yields empty output`() {
        assertThat(useCase(emptyList())).isEmpty()
    }

    @Test
    fun `latestAt reflects the newest notification in the group`() {
        val groups = useCase(
            listOf(
                notif("com.a", "Alpha", postedAt = 100, id = 1),
                notif("com.a", "Alpha", postedAt = 500, id = 2),
            ),
        )
        assertThat(groups.single().latestAt).isEqualTo(500)
    }
}
