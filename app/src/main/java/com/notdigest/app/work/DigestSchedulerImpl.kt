package com.notdigest.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import com.notdigest.app.core.Constants
import com.notdigest.app.core.util.TimeProvider
import com.notdigest.app.di.ApplicationScope
import com.notdigest.app.domain.repository.ScheduleRepository
import com.notdigest.app.domain.system.DigestScheduler
import com.notdigest.app.domain.usecase.ComputeNextDigestTimeUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager-backed implementation of [DigestScheduler]. The next delivery is always a single
 * unique one-time work item; the delivery worker re-arms the following one after it runs, so the
 * chain survives reboots (re-armed by BootReceiver) and schedule edits.
 */
@Singleton
class DigestSchedulerImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val computeNextDigestTime: ComputeNextDigestTimeUseCase,
    private val time: TimeProvider,
    @ApplicationScope private val scope: CoroutineScope,
) : DigestScheduler {

    // Lazy so WorkManager is initialised on first use — after the Application has finished
    // injecting its HiltWorkerFactory — never during this singleton's construction.
    private val workManager by lazy { WorkManager.getInstance(context) }

    override fun reschedule() {
        scope.launch { runCatching { rescheduleInternal() } }
    }

    override suspend fun rescheduleNow() = rescheduleInternal()

    private suspend fun rescheduleInternal() {
        val schedules = scheduleRepository.snapshot()
        val next = computeNextDigestTime(schedules, time.now())
        if (next == null) {
            // await(): enqueueUniqueWork/cancelUniqueWork only dispatch the real DB write to WorkManager's
            // own executor and return immediately. Awaiting the Operation makes rescheduleNow() resume
            // only after the next request is durably committed — so a caller (the delivery worker, or
            // BootReceiver.goAsync) that holds its execution window open until we return has actually
            // guaranteed the chain is re-armed, instead of racing the process being reclaimed.
            workManager.cancelUniqueWork(Constants.WORK_DIGEST_DELIVERY).await()
            return
        }
        val delay = (next - time.now()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<DigestDeliveryWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(DigestDeliveryWorker.KEY_MODE to DigestDeliveryWorker.MODE_SCHEDULED))
            .addTag(TAG)
            .build()
        workManager.enqueueUniqueWork(
            Constants.WORK_DIGEST_DELIVERY,
            ExistingWorkPolicy.REPLACE,
            request,
        ).await()
    }

    override fun ensureCleanupScheduled() {
        val request = PeriodicWorkRequestBuilder<RetentionCleanupWorker>(1, TimeUnit.DAYS)
            .addTag(TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            Constants.WORK_RETENTION_CLEANUP,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun cancelAll() {
        workManager.cancelUniqueWork(Constants.WORK_DIGEST_DELIVERY)
    }

    private companion object {
        const val TAG = "notdigest"
    }
}
