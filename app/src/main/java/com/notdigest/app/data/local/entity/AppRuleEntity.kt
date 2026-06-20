package com.notdigest.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "app_rules", indices = [Index("mode"), Index("updatedAt")])
data class AppRuleEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val mode: String,
    val isSystemApp: Boolean = false,
    val updatedAt: Long = 0L,
)
