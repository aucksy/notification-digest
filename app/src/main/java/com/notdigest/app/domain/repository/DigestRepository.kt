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

    suspend fun deleteDigest(id: Long)

    suspend fun deleteOlderThan(olderThan: Long): Int
}
