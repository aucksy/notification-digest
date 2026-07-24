package com.notdigest.app.ui.inbox

import com.google.common.truth.Truth.assertThat
import com.notdigest.app.domain.model.AppNotification
import org.junit.Test

/**
 * Covers [selectedApps], the pure helper behind "Make Real-Time" in the selection top bar (the action
 * that replaced swipe-right). It maps the selected notification ids to the distinct apps they belong to.
 */
class InboxSelectionTest {

    private fun notif(id: Long, pkg: String, app: String) = AppNotification(
        id = id,
        packageName = pkg,
        appName = app,
        title = "T$id",
        text = "Body $id",
        postedAt = id,
    )

    private val loaded = listOf(
        notif(1, "com.whatsapp", "WhatsApp"),
        notif(2, "com.instagram", "Instagram"),
        notif(3, "com.whatsapp", "WhatsApp"),
        notif(4, "com.slack", "Slack"),
    )

    @Test
    fun `collapses multiple notifications from the same app to one entry`() {
        val apps = selectedApps(loaded, setOf(1L, 3L))
        assertThat(apps).containsExactly("com.whatsapp" to "WhatsApp")
    }

    @Test
    fun `returns each distinct app once, in first-seen order`() {
        val apps = selectedApps(loaded, setOf(1L, 2L, 3L, 4L))
        assertThat(apps).containsExactly(
            "com.whatsapp" to "WhatsApp",
            "com.instagram" to "Instagram",
            "com.slack" to "Slack",
        ).inOrder()
    }

    @Test
    fun `ignores ids that are not currently loaded`() {
        val apps = selectedApps(loaded, setOf(2L, 999L))
        assertThat(apps).containsExactly("com.instagram" to "Instagram")
    }

    @Test
    fun `empty selection yields no apps`() {
        assertThat(selectedApps(loaded, emptySet())).isEmpty()
    }
}
