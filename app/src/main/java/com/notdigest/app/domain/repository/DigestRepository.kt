package com.notdigest.app.domain.repository

import com.notdigest.app.domain.model.Digest
import com.notdigest.app.domain.model.DigestType
import com.notdigest.app.domain.model.DigestWithItems
import kotlinx.coroutines.flow.Flow

/** Delivered digest batches (history). */
interface DigestRepository {

    fun observeDigests(): Flow<List<Digest>>

    fun searchDigests(query: String): Flow<List<Digest>>

    suspend fun getDigestWithItems(id: Long): DigestWithItems?

    /** Create a digest header row and return its id. Items are linked separately. */
    suspend fun createDigest(type: DigestType, createdAt: Long, notificationCount: Int, appCount: Int): Long

    /**
     * Atomically create a digest header AND link its notifications in one transaction, so a digest is
     * never observable with zero linked notifications. Prevents [deleteEmptyDigests] (run by the
     * retention worker on another thread) from race-deleting a just-created, not-yet-linked digest and
     * orphaning the delivered notifications.
     */
    suspend fun createDigestWithAssignment(
        type: DigestType,
        createdAt: Long,
        notificationCount: Int,
        appCount: Int,
        notificationIds: List<Long>,
    ): Long

    suspend fun deleteDigest(id: Long)

    suspend fun deleteOlderThan(olderThan: Long): Int

    /** Remove digests left with no notifications (e.g. after a retention purge) so counts never lie. */
    suspend fun deleteEmptyDigests(): Int
}
