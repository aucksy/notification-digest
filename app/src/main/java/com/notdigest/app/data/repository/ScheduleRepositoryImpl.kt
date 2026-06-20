package com.notdigest.app.data.repository

import androidx.room.withTransaction
import com.notdigest.app.core.Constants
import com.notdigest.app.data.local.NotDigestDatabase
import com.notdigest.app.data.local.dao.ScheduleDao
import com.notdigest.app.data.local.entity.ScheduleEntity
import com.notdigest.app.data.local.mapper.toDomain
import com.notdigest.app.data.local.mapper.toEntity
import com.notdigest.app.domain.model.Schedule
import com.notdigest.app.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val dao: ScheduleDao,
    private val db: NotDigestDatabase,
) : ScheduleRepository {

    override fun observeSchedules(): Flow<List<Schedule>> =
        dao.observeAll().map { it.map(ScheduleEntity::toDomain) }

    override suspend fun snapshot(): List<Schedule> = dao.snapshot().map(ScheduleEntity::toDomain)

    override suspend fun upsert(schedule: Schedule): Long = dao.upsert(schedule.toEntity())

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    override suspend fun reorder(orderedIds: List<Long>) {
        db.withTransaction {
            orderedIds.forEachIndexed { index, id -> dao.setOrder(id, index) }
        }
    }

    override suspend fun ensureSeeded() {
        if (dao.count() > 0) return
        Constants.SchedulePresets.BALANCED.forEachIndexed { index, minute ->
            dao.upsert(
                ScheduleEntity(
                    label = "Digest",
                    minuteOfDay = minute,
                    enabled = true,
                    sortOrder = index,
                ),
            )
        }
    }
}
