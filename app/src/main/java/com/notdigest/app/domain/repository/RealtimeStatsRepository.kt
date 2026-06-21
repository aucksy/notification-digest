package com.notdigest.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Content-free volume stats for Real-Time (un-batched) apps. Lets the app spot which un-batched apps
 * are noisy and suggest moving them into Digest. Only package + timestamp are ever recorded.
 */
interface RealtimeStatsRepository {
    suspend fun record(packageName: String, appName: String, postedAt: Long)
    fun observePerAppCountsSince(since: Long): Flow<Map<String, Int>>
    suspend fun countForPackageSince(packageName: String, since: Long): Int
    suspend fun purgeOlderThan(olderThan: Long): Int
}
