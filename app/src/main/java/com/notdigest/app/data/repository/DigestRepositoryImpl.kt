package com.notdigest.app.data.repository

import androidx.room.withTransaction
import com.notdigest.app.data.local.NotDigestDatabase
import com.notdigest.app.data.local.dao.DigestDao
import com.notdigest.app.data.local.dao.NotificationDao
import com.notdigest.app.data.local.entity.DigestEntity
import com.notdigest.app.data.local.entity.NotificationEntity
import com.notdigest.app.data.local.mapper.toDomain
import com.notdigest.app.domain.model.Digest
import com.notdigest.app.domain.model.DigestType
import com.notdigest.app.domain.model.DigestWithItems
import com.notdigest.app.domain.repository.DigestRepository
import com.notdigest.app.domain.usecase.GroupNotificationsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DigestRepositoryImpl @Inject constructor(
    private val db: NotDigestDatabase,
    private val digestDao: DigestDao,
    private val notificationDao: NotificationDao,
    private val groupNotifications: GroupNotificationsUseCase,
) : DigestRepository {

    override fun observeDigests(): Flow<List<Digest>> =
        digestDao.observeAll().map { it.map(DigestEntity::toDomain) }

    override fun searchDigests(query: String): Flow<List<Digest>> {
        val q = query.trim()
        val source = if (q.isEmpty()) digestDao.observeAll() else digestDao.search(q)
        return source.map { it.map(DigestEntity::toDomain) }
    }

    override suspend fun getDigestWithItems(id: Long): DigestWithItems? {
        val digest = digestDao.getById(id)?.toDomain() ?: return null
        val items = notificationDao.byDigest(id).map(NotificationEntity::toDomain)
        return DigestWithItems(digest, groupNotifications(items))
    }

    override suspend fun createDigest(
        type: DigestType,
        createdAt: Long,
        notificationCount: Int,
        appCount: Int,
    ): Long = digestDao.insert(
        DigestEntity(
            createdAt = createdAt,
            type = type.name,
            notificationCount = notificationCount,
            appCount = appCount,
        ),
    )

    override suspend fun deleteDigest(id: Long) {
        db.withTransaction {
            notificationDao.deleteByDigest(id)
            digestDao.delete(id)
        }
    }

    override suspend fun deleteOlderThan(olderThan: Long): Int =
        digestDao.deleteOlderThan(olderThan)
}
