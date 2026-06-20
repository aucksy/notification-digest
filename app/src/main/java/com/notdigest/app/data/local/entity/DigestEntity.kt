package com.notdigest.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "digests", indices = [Index("createdAt")])
data class DigestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val createdAt: Long,
    val type: String,
    val notificationCount: Int,
    val appCount: Int,
)
