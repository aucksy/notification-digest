package com.notdigest.app.domain.repository

import com.notdigest.app.domain.model.Schedule
import kotlinx.coroutines.flow.Flow

/** User-defined digest delivery times. */
interface ScheduleRepository {

    fun observeSchedules(): Flow<List<Schedule>>

    suspend fun snapshot(): List<Schedule>

    suspend fun upsert(schedule: Schedule): Long

    suspend fun delete(id: Long)

    suspend fun setEnabled(id: Long, enabled: Boolean)

    /** Persist a new ordering (ids in display order). */
    suspend fun reorder(orderedIds: List<Long>)

    /** Create the recommended default schedule on first run if none exist. */
    suspend fun ensureSeeded()
}
