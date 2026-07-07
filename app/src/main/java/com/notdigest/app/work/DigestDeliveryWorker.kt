package com.notdigest.app.work

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.notdigest.app.domain.model.DigestType
import com.notdigest.app.domain.system.DigestScheduler
import com.notdigest.app.service.DigestNotificationListenerService
import com.notdigest.app.domain.usecase.DeliverDigestUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Delivers a digest, then (for scheduled runs) re-arms the next delivery. Used both for the timed
 * chain and for "Deliver Now" — the only difference is the [DigestType] and whether it re-arms.
 */
@HiltWorker
class DigestDeliveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val deliverDigest: DeliverDigestUseCase,
    private val scheduler: DigestScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val mode = inputData.getString(KEY_MODE) ?: MODE_SCHEDULED
        val type = if (mode == MODE_MANUAL) DigestType.MANUAL else DigestType.SCHEDULED

        // We're already awake to deliver — nudge the listener to rebind so anything that slipped through
        // while it was killed gets swept out of the shade (and captured for the next digest).
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(applicationContext, DigestNotificationListenerService::class.java),
            )
        }

        runCatching { deliverDigest(type) }
            .onFailure { Log.w(TAG, "Digest delivery failed", it) }

        // Re-arm the NEXT delivery and AWAIT it — must finish inside the worker's guaranteed
        // execution window, or process death right after could lose the chain permanently.
        if (mode == MODE_SCHEDULED) runCatching { scheduler.rescheduleNow() }
            .onFailure { Log.w(TAG, "Reschedule failed", it) }
        return Result.success()
    }

    companion object {
        const val KEY_MODE = "mode"
        const val MODE_SCHEDULED = "scheduled"
        const val MODE_MANUAL = "manual"
        private const val TAG = "DigestDeliveryWorker"

        /** Enqueue an immediate manual delivery (used by the status-notification action). */
        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<DigestDeliveryWorker>()
                .setInputData(workDataOf(KEY_MODE to MODE_MANUAL))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
