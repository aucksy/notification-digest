package com.notdigest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.notdigest.app.data.local.entity.DismissedRecommendationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DismissedRecommendationDao {

    @Query("SELECT * FROM dismissed_recommendations")
    fun observeAll(): Flow<List<DismissedRecommendationEntity>>

    @Upsert
    suspend fun upsert(entity: DismissedRecommendationEntity)

    @Query("DELETE FROM dismissed_recommendations WHERE packageName = :pkg")
    suspend fun remove(pkg: String)
}
