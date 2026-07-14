package com.nurtlina.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nurtlina.app.data.local.entity.FeedLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedLogDao {

    @Query("SELECT * FROM feed_logs WHERE babyId = :babyId ORDER BY startedAt DESC")
    fun observeByBaby(babyId: String): Flow<List<FeedLogEntity>>

    @Query("SELECT * FROM feed_logs WHERE babyId = :babyId AND startedAt >= :from AND startedAt < :to ORDER BY startedAt DESC")
    fun observeByBabyAndRange(babyId: String, from: Long, to: Long): Flow<List<FeedLogEntity>>

    @Query("SELECT * FROM feed_logs WHERE babyId = :babyId AND startedAt >= :from AND startedAt < :to ORDER BY startedAt DESC")
    suspend fun getByBabyAndRange(babyId: String, from: Long, to: Long): List<FeedLogEntity>

    @Upsert
    suspend fun upsert(log: FeedLogEntity)

    @Query("DELETE FROM feed_logs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM feed_logs WHERE babyId = :babyId ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecentByBaby(babyId: String, limit: Int = 10): List<FeedLogEntity>

    @Query("SELECT * FROM feed_logs WHERE id = :id")
    suspend fun getById(id: String): FeedLogEntity?

    @Query("SELECT * FROM feed_logs WHERE updatedAt >= :sinceMillis")
    suspend fun getFeedLogsUpdatedSince(sinceMillis: Long): List<FeedLogEntity>

    @Query("UPDATE feed_logs SET syncStatus = :status, lastSyncedAt = :lastSyncedAt WHERE id = :id")
    suspend fun updateSyncState(id: String, status: String, lastSyncedAt: Long?)
}
