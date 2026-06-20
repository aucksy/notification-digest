package com.notdigest.app.domain.repository

import com.notdigest.app.domain.model.NotificationStats
import kotlinx.coroutines.flow.Flow

/** Derived, on-device-only metrics for the dashboard. */
interface StatsRepository {
    fun observeStats(): Flow<NotificationStats>
}
