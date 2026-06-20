package com.notdigest.app.domain.system

/**
 * Schedules the next digest delivery. Implemented over WorkManager in the work layer; abstracted
 * here so use cases can request a reschedule without knowing about Android scheduling.
 */
interface DigestScheduler {

    /** Recompute the next enabled delivery time and (re)enqueue the delivery worker. */
    fun reschedule()

    /** Periodic retention cleanup. */
    fun ensureCleanupScheduled()

    fun cancelAll()
}
