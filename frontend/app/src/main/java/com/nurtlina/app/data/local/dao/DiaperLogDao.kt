package com.nurtlina.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nurtlina.app.data.local.entity.DiaperLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaperLogDao {

    @Query("SELECT * FROM diaper_logs WHERE babyId = :babyId ORDER BY changedAt DESC")
    fun observeByBaby(babyId: String): Flow<List<DiaperLogEntity>>

    @Query("SELECT * FROM diaper_logs WHERE babyId = :babyId AND changedAt >= :from AND changedAt < :to ORDER BY changedAt DESC")
    fun observeByBabyAndRange(babyId: String, from: Long, to: Long): Flow<List<DiaperLogEntity>>

    @Query("SELECT * FROM diaper_logs WHERE babyId = :babyId AND changedAt >= :from AND changedAt < :to ORDER BY changedAt DESC")
    suspend fun getByBabyAndRange(babyId: String, from: Long, to: Long): List<DiaperLogEntity>

    @Upsert
    suspend fun upsert(log: DiaperLogEntity)

    @Query("DELETE FROM diaper_logs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM diaper_logs WHERE id = :id")
    suspend fun getById(id: String): DiaperLogEntity?

    @Query("SELECT * FROM diaper_logs WHERE updatedAt >= :sinceMillis")
    suspend fun getDiaperLogsUpdatedSince(sinceMillis: Long): List<DiaperLogEntity>

    @Query("UPDATE diaper_logs SET syncStatus = :status, lastSyncedAt = :lastSyncedAt WHERE id = :id")
    suspend fun updateSyncState(id: String, status: String, lastSyncedAt: Long?)
}
