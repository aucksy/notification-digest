package com.notdigest.app.core.util

/**
 * View-based "unread dot" rule for the Inbox.
 *
 * A delivered notification shows a dot until the user has actually **opened the Inbox** after it was
 * delivered. [seenThreshold] is the epoch-millis of the user's last Inbox visit (0 if they've never
 * opened it). The comparison uses [deliveredAt] — the moment a notification entered a digest — falling
 * back to [postedAt] only as a safety net for any row without a delivery stamp.
 *
 * The dot depends **only** on when the user last viewed the Inbox — never on a digest being delivered.
 * So if a user skips a digest (doesn't open the app), every notification in it keeps its dot through
 * the next digest and beyond, until the Inbox is finally opened. Dots clear on the visit *after* the
 * one in which they were seen (the threshold advances when the user leaves the Inbox).
 */
fun isNotificationUnread(deliveredAt: Long?, postedAt: Long, seenThreshold: Long): Boolean =
    (deliveredAt ?: postedAt) > seenThreshold
