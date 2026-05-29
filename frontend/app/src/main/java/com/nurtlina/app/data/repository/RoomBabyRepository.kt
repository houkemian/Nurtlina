package com.nurtlina.app.data.repository

import com.nurtlina.app.data.local.dao.BabyDao
import com.nurtlina.app.data.local.entity.BabyEntity
import com.nurtlina.app.domain.model.Baby
import com.nurtlina.app.domain.repository.BabyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomBabyRepository @Inject constructor(
    private val dao: BabyDao,
) : BabyRepository {

    override fun observeAll(): Flow<List<Baby>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Baby?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun upsert(baby: Baby) =
        dao.upsert(BabyEntity.fromDomain(baby))

    override suspend fun archive(id: String) =
        dao.archive(id, Instant.now().toEpochMilli())

    override suspend fun delete(id: String) =
        dao.delete(id)

    override suspend fun count(): Int =
        dao.count()
}
