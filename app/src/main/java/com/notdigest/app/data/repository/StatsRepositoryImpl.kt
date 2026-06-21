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
import kotlinx.coroutines.flow.map
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

    override fun observeStats(): Flow<NotificationStats> {
        val zone = time.zone()
        // Recompute the day boundary at each local midnight so "today" counters roll over even while
        // the app (and this ViewModel) stay alive across midnight.
        return localDayFlow(zone).flatMapLatest { today ->
            val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val weekAgo = time.now() - SEVEN_DAYS_MS

            // Snapshot the launchable apps here — once per midnight tick AND once per (re)subscription,
            // i.e. every time Home is re-entered. PackageManager doesn't push install/uninstall events
            // into a Room flow, so without this the Real-Time count would only react to rule edits and
            // could disagree with the Apps screen after an uninstall. The Apps screen likewise refreshes
            // its launchable set on navigation, so both stay consistent.
            val launchable = runCatching { installedApps.launchablePackageNames() }.getOrNull()
            val realtimeCount = appRuleDao.observePackagesByMode(DigestMode.REALTIME.name).map { pkgs ->
                if (launchable == null) pkgs.size else pkgs.count { it in launchable }
            }

            val partial = combine(
                notificationDao.observePendingCount(),
                notificationDao.observeCapturedSince(startOfToday),
                notificationDao.observeCapturedSince(weekAgo),
                notificationDao.observeDeliveredSince(startOfToday),
                realtimeCount,
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
