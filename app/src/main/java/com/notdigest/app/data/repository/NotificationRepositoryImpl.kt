package com.notdigest.app.data.repository

import com.notdigest.app.data.local.dao.NotificationDao
import com.notdigest.app.data.local.entity.NotificationEntity
import com.notdigest.app.data.local.mapper.toDomain
import com.notdigest.app.data.local.mapper.toEntity
import com.notdigest.app.domain.model.AppNotification
import com.notdigest.app.domain.repository.NotificationRepository
import com.notdigest.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val dao: NotificationDao,
    private val preferencesRepository: PreferencesRepository,
) : NotificationRepository {

    override fun observePending(): Flow<List<AppNotification>> =
        dao.observePending().map { it.map(NotificationEntity::toDomain) }

    override fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    override fun searchPending(query: String): Flow<List<AppNotification>> =
        dao.searchPending(query.trim()).map { it.map(NotificationEntity::toDomain) }

    override fun observeDelivered(): Flow<List<AppNotification>> =
        dao.observeDelivered().map { it.map(NotificationEntity::toDomain) }

    override fun searchDelivered(query: String): Flow<List<AppNotification>> =
        dao.searchDelivered(query.trim()).map { it.map(NotificationEntity::toDomain) }

    override suspend fun markAllDeliveredRead() = dao.markAllDeliveredRead()

    override fun observeByDigest(digestId: Long): Flow<List<AppNotification>> =
        dao.observeByDigest(digestId).map { it.map(NotificationEntity::toDomain) }

    override suspend fun pendingSnapshot(): List<AppNotification> =
        dao.pendingSnapshot().map(NotificationEntity::toDomain)

    override suspend fun getById(id: Long): AppNotification? = dao.getById(id)?.toDomain()

    override suspend fun insert(notification: AppNotification): Long = dao.insert(notification.toEntity())

    override suspend fun upsertPending(notification: AppNotification): Long {
        var isReplacement = false
        // 1. Same system key = the app updated the exact same notification in place.
        notification.sbnKey?.let { key ->
            dao.pendingIdByKey(key)?.let { existingId -> dao.delete(listOf(existingId)); isReplacement = true }
        }
        // 2. Identical content under a new key = a duplicate the app re-fired (Phone, Zepto, etc.).
        dao.pendingIdByContent(notification.packageName, notification.title, notification.text)
            ?.let { dupId -> dao.delete(listOf(dupId)); isReplacement = true }
        val id = dao.insert(notification.toEntity())
        // Each genuinely-new suppressed notification = one interruption avoided (lifetime, monotonic).
        if (!isReplacement) preferencesRepository.addLifetimeAvoided(1)
        return id
    }

    override suspend fun markRead(ids: List<Long>) = dao.markRead(ids)

    override suspend fun markAllPendingRead() = dao.markAllPendingRead()

    override suspend fun delete(ids: List<Long>) = dao.delete(ids)

    override suspend fun deletePendingAll() = dao.deletePendingAll()

    override suspend fun assignToDigest(ids: List<Long>, digestId: Long, deliveredAt: Long) =
        dao.assignToDigest(ids, digestId, deliveredAt)

    override suspend fun purgeOlderThan(olderThan: Long): Int = dao.purgeOlderThan(olderThan)

    override suspend fun countDeliveredSince(since: Long): Int = dao.countDeliveredSince(since)

    override suspend fun countCapturedSince(since: Long): Int = dao.countCapturedSince(since)

    override suspend fun totalCaptured(): Long = dao.totalCaptured()

    override fun observePerAppCountsSince(since: Long): Flow<Map<String, Int>> =
        dao.observePerAppCountsSince(since).map { rows -> rows.associate { it.packageName to it.cnt } }
}
