package com.notdigest.app.data.repository

import com.notdigest.app.core.util.TimeProvider
import com.notdigest.app.data.local.dao.AppRuleDao
import com.notdigest.app.data.local.dao.DismissedRecommendationDao
import com.notdigest.app.data.local.entity.DismissedRecommendationEntity
import com.notdigest.app.domain.model.AppRecommendation
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.repository.RealtimeStatsRepository
import com.notdigest.app.domain.repository.RecommendationRepository
import com.notdigest.app.domain.usecase.GenerateRecommendationsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationRepositoryImpl @Inject constructor(
    private val realtimeStats: RealtimeStatsRepository,
    private val appRuleDao: AppRuleDao,
    private val dismissedDao: DismissedRecommendationDao,
    private val generate: GenerateRecommendationsUseCase,
    private val time: TimeProvider,
) : RecommendationRepository {

    override fun observeRecommendations(): Flow<List<AppRecommendation>> {
        val weekAgo = time.now() - SEVEN_DAYS_MS
        return combine(
            // Volume of un-batched (Real-Time) apps — the only thing we suggest acting on.
            realtimeStats.observePerAppCountsSince(weekAgo),
            appRuleDao.observeAll(),
            dismissedDao.observeAll(),
        ) { counts, rules, dismissed ->
            val ruleByPkg = rules.associateBy { it.packageName }
            val volumes = counts.map { (pkg, cnt) ->
                val rule = ruleByPkg[pkg]
                GenerateRecommendationsUseCase.AppVolume(
                    packageName = pkg,
                    appName = rule?.appName ?: pkg,
                    mode = rule?.let { runCatching { DigestMode.valueOf(it.mode) }.getOrNull() }
                        ?: DigestMode.DIGEST,
                    weeklyCount = cnt,
                )
            }
            // A dismissed suggestion is gone for good: once the user dismisses an app's suggestion we
            // never surface it again, no matter how noisy that app later becomes. (They can still move
            // the app to Digest themselves anytime.) countAtDismiss is retained only for history.
            val dismissedSet = dismissed.map { it.packageName }.toSet()

            generate(volumes, dismissedSet)
        }
    }

    override suspend fun dismiss(packageName: String) {
        val now = time.now()
        val current = realtimeStats.countForPackageSince(packageName, now - SEVEN_DAYS_MS)
        dismissedDao.upsert(DismissedRecommendationEntity(packageName, now, current))
    }

    private companion object {
        const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
    }
}
