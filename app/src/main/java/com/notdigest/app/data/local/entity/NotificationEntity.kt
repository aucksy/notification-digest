package com.notdigest.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.notdigest.app.domain.model.NotificationActionItem

/**
 * Persisted notification. Indexed on the columns the inbox/stats queries filter & sort by so the
 * app stays fast with 10,000+ rows.
 *
 * [deliveredAt] is set when the notification is folded into a digest; it powers accurate
 * "delivered today" stats independent of the original [postedAt].
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index("isDelivered"),
        Index("postedAt"),
        Index("packageName"),
        Index("digestId"),
        Index("deliveredAt"),
    ],
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sbnKey: String?,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val subText: String?,
    val category: String?,
    val postedAt: Long,
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val digestId: Long? = null,
    val deliveredAt: Long? = null,
    val hasDeepLink: Boolean = false,
    val actions: List<NotificationActionItem> = emptyList(),
)
