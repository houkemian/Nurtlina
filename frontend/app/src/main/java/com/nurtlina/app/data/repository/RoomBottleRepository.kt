package com.nurtlina.app.data.repository

import com.nurtlina.app.data.local.dao.BottleDao
import com.nurtlina.app.data.local.entity.BottleEntity
import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.repository.BottleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomBottleRepository @Inject constructor(
    private val dao: BottleDao,
) : BottleRepository {

    override fun observeActive(babyId: String): Flow<List<Bottle>> =
        dao.observeActive(babyId).map { it.map { e -> e.toDomain() } }

    override fun observeAll(babyId: String): Flow<List<Bottle>> =
        dao.observeAll(babyId).map { it.map { e -> e.toDomain() } }

    override fun observeById(id: String): Flow<Bottle?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getById(id: String): Bottle? =
        dao.getById(id)?.toDomain()

    override suspend fun getAllActive(): List<Bottle> =
        dao.getAllActive().map { it.toDomain() }

    override suspend fun upsert(bottle: Bottle) =
        dao.upsert(BottleEntity.fromDomain(bottle))

    override suspend fun updateStatus(id: String, bottle: Bottle) =
        dao.upsert(BottleEntity.fromDomain(bottle))

    override suspend fun delete(id: String) =
        dao.delete(id)
}
