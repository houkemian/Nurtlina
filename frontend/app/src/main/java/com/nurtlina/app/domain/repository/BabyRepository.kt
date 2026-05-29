package com.nurtlina.app.domain.repository

import com.nurtlina.app.domain.model.Baby
import kotlinx.coroutines.flow.Flow

interface BabyRepository {
    fun observeAll(): Flow<List<Baby>>
    fun observeById(id: String): Flow<Baby?>
    suspend fun upsert(baby: Baby)
    suspend fun archive(id: String)
    suspend fun delete(id: String)
    suspend fun count(): Int
}
