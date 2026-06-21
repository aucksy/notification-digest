package com.notdigest.app.data.repository

import com.notdigest.app.data.local.dao.RealtimeEventDao
import com.notdigest.app.data.local.entity.RealtimeEventEntity
import com.notdigest.app.domain.repository.RealtimeStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeStatsRepositoryImpl @Inject constructor(
    private val dao: RealtimeEventDao,
) : RealtimeStatsRepository {

    override suspend fun record(packageName: String, appName: String, postedAt: Long) =
        dao.insert(RealtimeEventEntity(packageName = packageName, appName = appName, postedAt = postedAt))

    override fun observePerAppCountsSince(since: Long): Flow<Map<String, Int>> =
        dao.observePerAppCountsSince(since).map { rows -> rows.associate { it.packageName to it.cnt } }

    override suspend fun countForPackageSince(packageName: String, since: Long): Int =
        dao.countForPackageSince(packageName, since)

    override suspend fun purgeOlderThan(olderThan: Long): Int = dao.purgeOlderThan(olderThan)
}
