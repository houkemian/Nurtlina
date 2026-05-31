package com.nurtlina.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nurtlina.app.data.local.entity.SleepLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepLogDao {

    @Query("SELECT * FROM sleep_logs WHERE babyId = :babyId ORDER BY startedAt DESC")
    fun observeByBaby(babyId: String): Flow<List<SleepLogEntity>>

    @Query("SELECT * FROM sleep_logs WHERE babyId = :babyId AND startedAt >= :from AND startedAt < :to ORDER BY startedAt DESC")
    fun observeByBabyAndRange(babyId: String, from: Long, to: Long): Flow<List<SleepLogEntity>>

    @Query("SELECT * FROM sleep_logs WHERE babyId = :babyId AND endedAt IS NULL LIMIT 1")
    fun observeActiveSleep(babyId: String): Flow<SleepLogEntity?>

    @Query("SELECT * FROM sleep_logs WHERE babyId = :babyId AND startedAt >= :from AND startedAt < :to ORDER BY startedAt DESC")
    suspend fun getByBabyAndRange(babyId: String, from: Long, to: Long): List<SleepLogEntity>

    @Query("SELECT * FROM sleep_logs WHERE babyId = :babyId AND endedAt IS NULL LIMIT 1")
    suspend fun getActiveSleep(babyId: String): SleepLogEntity?

    @Upsert
    suspend fun upsert(log: SleepLogEntity)

    @Query("DELETE FROM sleep_logs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM sleep_logs WHERE id = :id")
    suspend fun getById(id: String): SleepLogEntity?

    @Query("SELECT * FROM sleep_logs WHERE updatedAt >= :sinceMillis")
    suspend fun getSleepLogsUpdatedSince(sinceMillis: Long): List<SleepLogEntity>

    @Query("UPDATE sleep_logs SET syncStatus = :status, lastSyncedAt = :lastSyncedAt WHERE id = :id")
    suspend fun updateSyncState(id: String, status: String, lastSyncedAt: Long?)
}
