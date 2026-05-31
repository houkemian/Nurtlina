package com.nurtlina.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nurtlina.app.data.local.entity.BottleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BottleDao {

    @Query("""
        SELECT * FROM bottles 
        WHERE babyId = :babyId 
        AND status NOT IN ('FED', 'DISCARDED', 'CANCELED')
        ORDER BY createdAt DESC
    """)
    fun observeActive(babyId: String): Flow<List<BottleEntity>>

    @Query("SELECT * FROM bottles WHERE babyId = :babyId ORDER BY createdAt DESC")
    fun observeAll(babyId: String): Flow<List<BottleEntity>>

    @Query("SELECT * FROM bottles WHERE id = :id")
    fun observeById(id: String): Flow<BottleEntity?>

    @Query("SELECT * FROM bottles WHERE id = :id")
    suspend fun getById(id: String): BottleEntity?

    @Query("""
        SELECT * FROM bottles 
        WHERE status NOT IN ('FED', 'DISCARDED', 'CANCELED')
    """)
    suspend fun getAllActive(): List<BottleEntity>

    @Upsert
    suspend fun upsert(bottle: BottleEntity)

    @Query("DELETE FROM bottles WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM bottles WHERE updatedAt >= :sinceMillis")
    suspend fun getBottlesUpdatedSince(sinceMillis: Long): List<BottleEntity>

    @Query("UPDATE bottles SET syncStatus = :status, lastSyncedAt = :lastSyncedAt WHERE id = :id")
    suspend fun updateSyncState(id: String, status: String, lastSyncedAt: Long?)
}
