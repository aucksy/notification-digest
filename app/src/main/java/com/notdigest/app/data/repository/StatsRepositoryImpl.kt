package com.notdigest.app.data.repository

import com.notdigest.app.core.util.TimeProvider
import com.notdigest.app.core.util.localDayFlow
import com.notdigest.app.data.local.dao.AppRuleDao
import com.notdigest.app.data.local.dao.NotificationDao
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.model.NotificationStats
import com.notdigest.app.domain.repository.InstalledAppsRepository
import com.notdigest.app.domain.repository.PreferencesRepository
import com.notdigest.app.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val appRuleDao: AppRuleDao,
    private val installedApps: InstalledAppsRepository,
    private val preferencesRepository: PreferencesRepository,
    private val time: TimeProvider,
) : StatsRepository {

    private data class Partial(
        val pending: Int,
        val capturedToday: Int,
        val capturedWeek: Int,
        val deliveredToday: Int,
        val realtime: Int,
    )

    // Count only Real-Time apps the user can actually see on the Apps screen (installed + launchable).
    // A restored backup, or an app later uninstalled, leaves a Real-Time rule for an app that isn't in
    // the list — counting raw rules made the Home tile disagree with the Apps screen.
    private val realtimeAppCount: Flow<Int> =
        appRuleDao.observePackagesByMode(DigestMode.REALTIME.name).mapLatest { pkgs ->
            val launchable = runCatching { installedApps.launchablePackageNames() }.getOrNull()
            if (launchable == null) pkgs.size else pkgs.count { it in launchable }
        }

    override fun observeStats(): Flow<NotificationStats> {
        val zone = time.zone()
        // Recompute the day boundary at each local midnight so "today" counters roll over even while
        // the app (and this ViewModel) stay alive across midnight.
        return localDayFlow(zone).flatMapLatest { today ->
            val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val weekAgo = time.now() - SEVEN_DAYS_MS

            val partial = combine(
                notificationDao.observePendingCount(),
                notificationDao.observeCapturedSince(startOfToday),
                notificationDao.observeCapturedSince(weekAgo),
                notificationDao.observeDeliveredSince(startOfToday),
                realtimeAppCount,
            ) { pending, capturedToday, capturedWeek, deliveredToday, realtime ->
                Partial(pending, capturedToday, capturedWeek, deliveredToday, realtime)
            }

            combine(
                partial,
                notificationDao.observeTotal(),
                preferencesRepository.lifetimeAvoided,
            ) { p, total, lifetime ->
                NotificationStats(
                    waitingCount = p.pending,
                    batchedToday = p.capturedToday,
                    avoidedLast7Days = p.capturedWeek,
                    totalAvoided = total,
                    deliveredToday = p.deliveredToday,
                    realtimeAppCount = p.realtime,
                    lifetimeAvoided = lifetime,
                )
            }
        }
    }

    private companion object {
        const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
    }
}
