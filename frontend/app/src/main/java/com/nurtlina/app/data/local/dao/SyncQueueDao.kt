package com.nurtlina.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nurtlina.app.data.local.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {
    @Upsert
    suspend fun upsert(item: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue WHERE nextRetryAt <= :nowMillis ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getReady(nowMillis: Long, limit: Int): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE id = :id")
    suspend fun getById(id: String): SyncQueueEntity?

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun count(): Int
}
