package com.notdigest.app.domain.repository

import com.notdigest.app.domain.model.AppRecommendation
import kotlinx.coroutines.flow.Flow

/** Surfaces and dismisses volume-based app suggestions. */
interface RecommendationRepository {

    fun observeRecommendations(): Flow<List<AppRecommendation>>

    /** Hide a suggestion for an app; it won't reappear until volume changes materially. */
    suspend fun dismiss(packageName: String)
}
