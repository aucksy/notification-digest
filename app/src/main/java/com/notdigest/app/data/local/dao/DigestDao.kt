package com.notdigest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.notdigest.app.data.local.entity.DigestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DigestDao {

    @Insert
    suspend fun insert(entity: DigestEntity): Long

    @Query("SELECT * FROM digests ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DigestEntity>>

    @Query("SELECT * FROM digests WHERE id = :id")
    suspend fun getById(id: Long): DigestEntity?

    @Query("DELETE FROM digests WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM digests WHERE createdAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long): Int

    /** Remove digests that no longer have any linked notifications (e.g. after retention purges). */
    @Query("DELETE FROM digests WHERE id NOT IN (SELECT DISTINCT digestId FROM notifications WHERE digestId IS NOT NULL)")
    suspend fun deleteEmptyDigests(): Int

    @Query(
        """
        SELECT DISTINCT d.* FROM digests d
        LEFT JOIN notifications n ON n.digestId = d.id
        WHERE n.title LIKE '%' || :q || '%'
           OR n.text LIKE '%' || :q || '%'
           OR n.appName LIKE '%' || :q || '%'
        ORDER BY d.createdAt DESC
        """,
    )
    fun search(q: String): Flow<List<DigestEntity>>
}
