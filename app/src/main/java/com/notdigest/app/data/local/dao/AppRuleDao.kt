package com.notdigest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.notdigest.app.data.local.entity.AppRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {

    @Query("SELECT * FROM app_rules ORDER BY appName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rules ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecentlyChanged(limit: Int): Flow<List<AppRuleEntity>>

    @Query("SELECT COUNT(*) FROM app_rules WHERE mode = :mode")
    fun observeCountByMode(mode: String): Flow<Int>

    @Query("SELECT * FROM app_rules WHERE packageName = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): AppRuleEntity?

    @Query("SELECT * FROM app_rules")
    suspend fun snapshot(): List<AppRuleEntity>

    @Upsert
    suspend fun upsert(rule: AppRuleEntity)

    @Upsert
    suspend fun upsertAll(rules: List<AppRuleEntity>)

    /** Seed only apps we haven't seen — never clobbers an existing user choice. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(rules: List<AppRuleEntity>)
}
