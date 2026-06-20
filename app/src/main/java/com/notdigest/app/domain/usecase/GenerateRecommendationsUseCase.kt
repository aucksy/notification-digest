package com.notdigest.app.domain.usecase

import com.notdigest.app.core.Constants
import com.notdigest.app.domain.model.AppRecommendation
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.model.RecommendationType
import javax.inject.Inject

/**
 * Derives subtle suggestions purely from weekly notification volume — no AI, no profiling.
 *
 * Rules (only ever surfaced for apps currently in Digest mode):
 *  - very high volume  -> offer to move it to Real-Time so nothing important is missed
 *  - moderate volume   -> gentle affirmation that Digest is keeping it calm
 *  - below moderate, dismissed, or already Real-Time -> nothing
 *
 * Capped and ordered by volume so the dashboard never feels noisy.
 */
class GenerateRecommendationsUseCase @Inject constructor() {

    data class AppVolume(
        val packageName: String,
        val appName: String,
        val mode: DigestMode,
        val weeklyCount: Int,
    )

    operator fun invoke(
        volumes: List<AppVolume>,
        dismissed: Set<String>,
        maxResults: Int = Constants.MAX_RECOMMENDATIONS,
    ): List<AppRecommendation> =
        volumes
            .asSequence()
            .filter { it.mode == DigestMode.DIGEST }
            .filter { it.packageName !in dismissed }
            .filter { it.weeklyCount >= Constants.RECOMMEND_MODERATE_WEEKLY }
            .sortedByDescending { it.weeklyCount }
            .take(maxResults)
            .map { v ->
                val type =
                    if (v.weeklyCount >= Constants.RECOMMEND_HIGH_WEEKLY) RecommendationType.MOVE_TO_REALTIME
                    else RecommendationType.KEEP_IN_DIGEST
                AppRecommendation(
                    packageName = v.packageName,
                    appName = v.appName,
                    weeklyCount = v.weeklyCount,
                    type = type,
                    currentMode = v.mode,
                )
            }
            .toList()
}
