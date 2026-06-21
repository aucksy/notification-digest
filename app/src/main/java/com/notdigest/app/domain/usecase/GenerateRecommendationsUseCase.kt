package com.notdigest.app.domain.usecase

import com.notdigest.app.core.Constants
import com.notdigest.app.core.util.CriticalDefaults
import com.notdigest.app.domain.model.AppRecommendation
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.model.RecommendationType
import javax.inject.Inject

/**
 * Derives subtle suggestions purely from weekly notification volume — no AI, no profiling.
 *
 * Only one kind of suggestion, and only ever actionable: a **Real-Time (un-batched) app that's
 * sending a lot** — offer to move it into Digest to quiet it. Critical apps (SMS, OTP, dialer, …)
 * are never suggested for quieting; neither are apps already in Digest (there's nothing to do).
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
            .filter { it.mode == DigestMode.REALTIME }
            .filter { !CriticalDefaults.isCritical(it.packageName, it.appName) }
            .filter { it.packageName !in dismissed }
            .filter { it.weeklyCount >= Constants.RECOMMEND_HIGH_WEEKLY }
            .sortedByDescending { it.weeklyCount }
            .take(maxResults)
            .map { v ->
                AppRecommendation(
                    packageName = v.packageName,
                    appName = v.appName,
                    weeklyCount = v.weeklyCount,
                    type = RecommendationType.MOVE_TO_DIGEST,
                    currentMode = v.mode,
                )
            }
            .toList()
}
