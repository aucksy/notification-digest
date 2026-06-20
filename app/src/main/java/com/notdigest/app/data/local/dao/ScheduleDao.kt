package com.notdigest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.notdigest.app.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedules ORDER BY sortOrder ASC, minuteOfDay ASC")
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules ORDER BY sortOrder ASC, minuteOfDay ASC")
    suspend fun snapshot(): List<ScheduleEntity>

    @Upsert
    suspend fun upsert(entity: ScheduleEntity): Long

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE schedules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE schedules SET sortOrder = :order WHERE id = :id")
    suspend fun setOrder(id: Long, order: Int)

    @Query("SELECT COUNT(*) FROM schedules")
    suspend fun count(): Int
}
