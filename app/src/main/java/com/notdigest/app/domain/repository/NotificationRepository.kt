package com.notdigest.app.domain.repository

import com.notdigest.app.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

/** Captured notifications: the inbox queue, search, read/delete, and digest assignment. */
interface NotificationRepository {

    /** Undelivered notifications, newest first. */
    fun observePending(): Flow<List<AppNotification>>

    /** Live count of undelivered notifications (cheap; backed by a COUNT query). */
    fun observePendingCount(): Flow<Int>

    /** Full-text-ish search across pending notifications (title, body, app name). */
    fun searchPending(query: String): Flow<List<AppNotification>>

    /** Delivered notifications — the read-anytime archive shown in the Inbox after delivery. */
    fun observeDelivered(): Flow<List<AppNotification>>

    fun searchDelivered(query: String): Flow<List<AppNotification>>

    suspend fun markAllDeliveredRead()

    /** All notifications belonging to a delivered digest, newest first. */
    fun observeByDigest(digestId: Long): Flow<List<AppNotification>>

    suspend fun pendingSnapshot(): List<AppNotification>

    suspend fun getById(id: Long): AppNotification?

    suspend fun insert(notification: AppNotification): Long

    suspend fun markRead(ids: List<Long>)

    suspend fun markAllPendingRead()

    suspend fun delete(ids: List<Long>)

    suspend fun deletePendingAll()

    /** Attach a set of notifications to a digest and mark them delivered. */
    suspend fun assignToDigest(ids: List<Long>, digestId: Long, deliveredAt: Long)

    /** Remove notifications older than [olderThan] (epoch millis). Returns rows deleted. */
    suspend fun purgeOlderThan(olderThan: Long): Int

    // --- Statistics helpers ---
    suspend fun countDeliveredSince(since: Long): Int
    suspend fun countCapturedSince(since: Long): Int
    suspend fun totalCaptured(): Long
    fun observePerAppCountsSince(since: Long): Flow<Map<String, Int>>
}
