package com.notdigest.app.data.repository

import com.notdigest.app.core.util.TimeProvider
import com.notdigest.app.data.local.dao.AppRuleDao
import com.notdigest.app.data.local.dao.NotificationDao
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.model.NotificationStats
import com.notdigest.app.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val appRuleDao: AppRuleDao,
    private val time: TimeProvider,
) : StatsRepository {

    private data class Partial(
        val pending: Int,
        val capturedToday: Int,
        val capturedWeek: Int,
        val deliveredToday: Int,
        val realtime: Int,
    )

    override fun observeStats(): Flow<NotificationStats> {
        val now = time.now()
        val zone = time.zone()
        val startOfToday = Instant.ofEpochMilli(now)
            .atZone(zone)
            .toLocalDate()
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        val weekAgo = now - SEVEN_DAYS_MS

        val partial = combine(
            notificationDao.observePendingCount(),
            notificationDao.observeCapturedSince(startOfToday),
            notificationDao.observeCapturedSince(weekAgo),
            notificationDao.observeDeliveredSince(startOfToday),
            appRuleDao.observeCountByMode(DigestMode.REALTIME.name),
        ) { pending, capturedToday, capturedWeek, deliveredToday, realtime ->
            Partial(pending, capturedToday, capturedWeek, deliveredToday, realtime)
        }

        return combine(partial, notificationDao.observeTotal()) { p, total ->
            NotificationStats(
                waitingCount = p.pending,
                batchedToday = p.capturedToday,
                avoidedLast7Days = p.capturedWeek,
                totalAvoided = total,
                deliveredToday = p.deliveredToday,
                realtimeAppCount = p.realtime,
            )
        }
    }

    private companion object {
        const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
    }
}
