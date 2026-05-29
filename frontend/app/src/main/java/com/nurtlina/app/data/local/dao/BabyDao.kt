package com.nurtlina.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nurtlina.app.data.local.entity.BabyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BabyDao {

    @Query("SELECT * FROM babies WHERE archivedAt IS NULL ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<BabyEntity>>

    @Query("SELECT * FROM babies WHERE id = :id")
    fun observeById(id: String): Flow<BabyEntity?>

    @Upsert
    suspend fun upsert(baby: BabyEntity)

    @Query("UPDATE babies SET archivedAt = :archivedAt WHERE id = :id")
    suspend fun archive(id: String, archivedAt: Long)

    @Query("DELETE FROM babies WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM babies WHERE archivedAt IS NULL")
    suspend fun count(): Int

    @Query("SELECT * FROM babies WHERE id = :id")
    suspend fun getById(id: String): BabyEntity?

    @Query("SELECT * FROM babies WHERE updatedAt >= :sinceMillis")
    suspend fun getBabiesUpdatedSince(sinceMillis: Long): List<BabyEntity>
}
