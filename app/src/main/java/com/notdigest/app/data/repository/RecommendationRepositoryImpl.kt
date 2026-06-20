package com.notdigest.app.data.repository

import com.notdigest.app.core.util.TimeProvider
import com.notdigest.app.data.local.dao.AppRuleDao
import com.notdigest.app.data.local.dao.DismissedRecommendationDao
import com.notdigest.app.data.local.dao.NotificationDao
import com.notdigest.app.data.local.entity.DismissedRecommendationEntity
import com.notdigest.app.domain.model.AppRecommendation
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.repository.RecommendationRepository
import com.notdigest.app.domain.usecase.GenerateRecommendationsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val appRuleDao: AppRuleDao,
    private val dismissedDao: DismissedRecommendationDao,
    private val generate: GenerateRecommendationsUseCase,
    private val time: TimeProvider,
) : RecommendationRepository {

    override fun observeRecommendations(): Flow<List<AppRecommendation>> {
        val weekAgo = time.now() - SEVEN_DAYS_MS
        return combine(
            notificationDao.observePerAppCountsSince(weekAgo),
            appRuleDao.observeAll(),
            dismissedDao.observeAll(),
        ) { counts, rules, dismissed ->
            val ruleByPkg = rules.associateBy { it.packageName }
            val volumes = counts.map { pc ->
                val rule = ruleByPkg[pc.packageName]
                GenerateRecommendationsUseCase.AppVolume(
                    packageName = pc.packageName,
                    appName = rule?.appName ?: pc.packageName,
                    mode = rule?.let { runCatching { DigestMode.valueOf(it.mode) }.getOrNull() }
                        ?: DigestMode.DIGEST,
                    weeklyCount = pc.cnt,
                )
            }
            // A dismissed suggestion stays hidden until the app's volume grows materially (>1.5x).
            val dismissedSet = dismissed
                .filter { d ->
                    val current = counts.firstOrNull { it.packageName == d.packageName }?.cnt ?: 0
                    current <= d.countAtDismiss * 1.5
                }
                .map { it.packageName }
                .toSet()

            generate(volumes, dismissedSet)
        }
    }

    override suspend fun dismiss(packageName: String) {
        val now = time.now()
        val current = notificationDao.countForPackageSince(packageName, now - SEVEN_DAYS_MS)
        dismissedDao.upsert(DismissedRecommendationEntity(packageName, now, current))
    }

    private companion object {
        const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
    }
}
