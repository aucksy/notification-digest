package com.notdigest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.notdigest.app.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(entity: NotificationEntity): Long

    /** Id of an undelivered notification with the same system key (a re-post/update of the same one). */
    @Query("SELECT id FROM notifications WHERE sbnKey = :key AND isDelivered = 0 LIMIT 1")
    suspend fun pendingIdByKey(key: String): Long?

    // --- Inbox (pending == not yet delivered) ---

    @Query("SELECT * FROM notifications WHERE isDelivered = 0 ORDER BY postedAt DESC")
    fun observePending(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isDelivered = 0")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM notifications WHERE isDelivered = 0 ORDER BY postedAt DESC")
    suspend fun pendingSnapshot(): List<NotificationEntity>

    @Query(
        """
        SELECT * FROM notifications
        WHERE isDelivered = 0
          AND (title LIKE '%' || :q || '%'
            OR text LIKE '%' || :q || '%'
            OR appName LIKE '%' || :q || '%')
        ORDER BY postedAt DESC
        """,
    )
    fun searchPending(q: String): Flow<List<NotificationEntity>>

    // --- Delivered (the read-anytime archive shown in the Inbox after delivery) ---

    @Query("SELECT * FROM notifications WHERE isDelivered = 1 ORDER BY deliveredAt DESC, postedAt DESC")
    fun observeDelivered(): Flow<List<NotificationEntity>>

    @Query(
        """
        SELECT * FROM notifications
        WHERE isDelivered = 1
          AND (title LIKE '%' || :q || '%'
            OR text LIKE '%' || :q || '%'
            OR appName LIKE '%' || :q || '%')
        ORDER BY deliveredAt DESC, postedAt DESC
        """,
    )
    fun searchDelivered(q: String): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET isRead = 1 WHERE isDelivered = 1")
    suspend fun markAllDeliveredRead()

    // --- Digest items ---

    @Query("SELECT * FROM notifications WHERE digestId = :digestId ORDER BY postedAt DESC")
    fun observeByDigest(digestId: Long): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE digestId = :digestId ORDER BY postedAt DESC")
    suspend fun byDigest(digestId: Long): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getById(id: Long): NotificationEntity?

    // --- Mutations ---

    @Query("UPDATE notifications SET isRead = 1 WHERE id IN (:ids)")
    suspend fun markRead(ids: List<Long>)

    @Query("UPDATE notifications SET isRead = 1 WHERE isDelivered = 0")
    suspend fun markAllPendingRead()

    @Query("DELETE FROM notifications WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("DELETE FROM notifications WHERE isDelivered = 0")
    suspend fun deletePendingAll()

    @Query("DELETE FROM notifications WHERE digestId = :digestId")
    suspend fun deleteByDigest(digestId: Long)

    @Query(
        "UPDATE notifications SET isDelivered = 1, digestId = :digestId, deliveredAt = :deliveredAt WHERE id IN (:ids)",
    )
    suspend fun assignToDigest(ids: List<Long>, digestId: Long, deliveredAt: Long)

    @Query("DELETE FROM notifications WHERE postedAt < :olderThan")
    suspend fun purgeOlderThan(olderThan: Long): Int

    // --- Statistics ---

    @Query("SELECT COUNT(*) FROM notifications WHERE deliveredAt IS NOT NULL AND deliveredAt >= :since")
    suspend fun countDeliveredSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE postedAt >= :since")
    suspend fun countCapturedSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun totalCaptured(): Long

    @Query("SELECT COUNT(*) FROM notifications WHERE packageName = :pkg AND postedAt >= :since")
    suspend fun countForPackageSince(pkg: String, since: Long): Int

    @Query("SELECT packageName, COUNT(*) AS cnt FROM notifications WHERE postedAt >= :since GROUP BY packageName")
    fun observePerAppCountsSince(since: Long): Flow<List<PackageCount>>

    // --- Reactive stat streams (the `since` boundary is fixed at subscription time) ---

    @Query("SELECT COUNT(*) FROM notifications WHERE postedAt >= :since")
    fun observeCapturedSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE deliveredAt IS NOT NULL AND deliveredAt >= :since")
    fun observeDeliveredSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications")
    fun observeTotal(): Flow<Long>
}
