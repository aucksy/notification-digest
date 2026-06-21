package com.notdigest.app.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notdigest.app.core.util.TimeProvider
import com.notdigest.app.domain.repository.DigestRepository
import com.notdigest.app.domain.repository.NotificationRepository
import com.notdigest.app.domain.repository.PreferencesRepository
import com.notdigest.app.domain.repository.RealtimeStatsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Periodically removes notifications and digests older than the user's retention window. */
@HiltWorker
class RetentionCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferencesRepository: PreferencesRepository,
    private val notificationRepository: NotificationRepository,
    private val digestRepository: DigestRepository,
    private val realtimeStats: RealtimeStatsRepository,
    private val time: TimeProvider,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val days = preferencesRepository.snapshot().retentionDays
        val cutoff = time.now() - days * DAY_MS
        runCatching {
            val removed = notificationRepository.purgeOlderThan(cutoff)
            digestRepository.deleteOlderThan(cutoff)
            // Drop any digest left with no notifications so History never shows a phantom/under-counted batch.
            digestRepository.deleteEmptyDigests()
            realtimeStats.purgeOlderThan(cutoff)
            removed
        }.onFailure { Log.w(TAG, "Retention cleanup failed", it) }
        return Result.success()
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
        const val TAG = "RetentionCleanupWorker"
    }
}
