package com.notdigest.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A content-free record that a Real-Time app posted a notification — **package + time only**, never
 * any title/text. Real-Time apps are otherwise not stored at all; this minimal metadata lets us
 * estimate how noisy *un-batched* apps are, so we can suggest moving the noisy ones into Digest.
 * Purged on the user's retention window like everything else.
 */
@Entity(tableName = "realtime_events")
data class RealtimeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val postedAt: Long,
)
