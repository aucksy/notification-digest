package com.notdigest.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String,
    val minuteOfDay: Int,
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
)
