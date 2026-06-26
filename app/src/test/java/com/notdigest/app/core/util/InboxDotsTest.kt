package com.notdigest.app.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Locks the view-based unread-dot rule, especially the cross-digest persistence the user asked for:
 * notifications from a digest the user never opened keep their dot through later digests, and dots
 * clear only after the Inbox is actually opened.
 */
class InboxDotsTest {

    // Timeline (epoch millis, simplified): the user last opened the Inbox at T_SEEN.
    private val tSeen = 1_000L

    @Test
    fun `delivered after last visit is unread`() {
        assertThat(isNotificationUnread(deliveredAt = 1_500L, postedAt = 1_400L, seenThreshold = tSeen)).isTrue()
    }

    @Test
    fun `delivered before last visit is read`() {
        assertThat(isNotificationUnread(deliveredAt = 800L, postedAt = 700L, seenThreshold = tSeen)).isFalse()
    }

    @Test
    fun `never visited inbox (threshold 0) shows everything as unread`() {
        assertThat(isNotificationUnread(deliveredAt = 1L, postedAt = 1L, seenThreshold = 0L)).isTrue()
    }

    @Test
    fun `untapped digest stays unread across a later digest until the inbox is opened`() {
        // Digest 1 delivered at 2_000 while last inbox visit was 1_000 — unread.
        val digest1 = 2_000L
        assertThat(isNotificationUnread(digest1, digest1, seenThreshold = 1_000L)).isTrue()
        // The user never opens the Inbox, so the threshold does NOT advance. Digest 2 arrives at 3_000.
        val digest2 = 3_000L
        // Both digests remain unread against the still-old threshold.
        assertThat(isNotificationUnread(digest1, digest1, seenThreshold = 1_000L)).isTrue()
        assertThat(isNotificationUnread(digest2, digest2, seenThreshold = 1_000L)).isTrue()
        // Only after the user opens the Inbox (threshold advances to, say, 3_500) do both clear.
        assertThat(isNotificationUnread(digest1, digest1, seenThreshold = 3_500L)).isFalse()
        assertThat(isNotificationUnread(digest2, digest2, seenThreshold = 3_500L)).isFalse()
    }

    @Test
    fun `falls back to postedAt when there is no delivery stamp`() {
        assertThat(isNotificationUnread(deliveredAt = null, postedAt = 1_500L, seenThreshold = tSeen)).isTrue()
        assertThat(isNotificationUnread(deliveredAt = null, postedAt = 500L, seenThreshold = tSeen)).isFalse()
    }
}
