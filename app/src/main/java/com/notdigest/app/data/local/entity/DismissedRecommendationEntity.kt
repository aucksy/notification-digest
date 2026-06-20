package com.notdigest.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records a dismissed suggestion. We keep [countAtDismiss] so a suggestion can quietly reappear
 * only if the app's volume grows materially beyond when the user last said "not now".
 */
@Entity(tableName = "dismissed_recommendations")
data class DismissedRecommendationEntity(
    @PrimaryKey val packageName: String,
    val dismissedAt: Long,
    val countAtDismiss: Int,
)
