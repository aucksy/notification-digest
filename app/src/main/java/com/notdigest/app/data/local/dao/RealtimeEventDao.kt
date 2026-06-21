package com.notdigest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.notdigest.app.data.local.entity.RealtimeEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RealtimeEventDao {

    @Insert
    suspend fun insert(event: RealtimeEventEntity)

    /** Per-app counts of Real-Time notifications since [since] — the noisiness signal for suggestions. */
    @Query("SELECT packageName, COUNT(*) AS cnt FROM realtime_events WHERE postedAt >= :since GROUP BY packageName")
    fun observePerAppCountsSince(since: Long): Flow<List<PackageCount>>

    @Query("SELECT COUNT(*) FROM realtime_events WHERE packageName = :pkg AND postedAt >= :since")
    suspend fun countForPackageSince(pkg: String, since: Long): Int

    @Query("DELETE FROM realtime_events WHERE postedAt < :olderThan")
    suspend fun purgeOlderThan(olderThan: Long): Int
}
