package com.notdigest.app.domain.system

/**
 * Schedules the next digest delivery. Implemented over WorkManager in the work layer; abstracted
 * here so use cases can request a reschedule without knowing about Android scheduling.
 */
interface DigestScheduler {

    /** Recompute the next enabled delivery time and (re)enqueue the delivery worker (fire-and-forget). */
    fun reschedule()

    /**
     * Like [reschedule] but suspends until the next delivery is actually enqueued. Workers and
     * receivers MUST use this so the re-arm completes inside their guaranteed execution window —
     * a fire-and-forget reschedule can be lost if the process is reclaimed right after.
     */
    suspend fun rescheduleNow()

    /** Periodic retention cleanup. */
    fun ensureCleanupScheduled()

    fun cancelAll()
}
