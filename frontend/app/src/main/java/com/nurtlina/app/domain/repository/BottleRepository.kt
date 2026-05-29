package com.nurtlina.app.domain.repository

import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import kotlinx.coroutines.flow.Flow

interface BottleRepository {
    fun observeActive(babyId: String): Flow<List<Bottle>>
    fun observeAll(babyId: String): Flow<List<Bottle>>
    fun observeById(id: String): Flow<Bottle?>
    suspend fun getById(id: String): Bottle?
    suspend fun getAllActive(): List<Bottle>
    suspend fun upsert(bottle: Bottle)
    suspend fun updateStatus(id: String, bottle: Bottle)
    suspend fun delete(id: String)
}
