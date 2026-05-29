package com.nurtlina.app.data.repository

import com.nurtlina.app.data.local.dao.DiaperLogDao
import com.nurtlina.app.data.local.entity.DiaperLogEntity
import com.nurtlina.app.domain.model.DiaperLog
import com.nurtlina.app.domain.repository.DiaperLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomDiaperLogRepository @Inject constructor(
    private val dao: DiaperLogDao,
) : DiaperLogRepository {

    override fun observeByBaby(babyId: String): Flow<List<DiaperLog>> =
        dao.observeByBaby(babyId).map { it.map { e -> e.toDomain() } }

    override fun observeByBabyAndRange(babyId: String, from: Instant, to: Instant): Flow<List<DiaperLog>> =
        dao.observeByBabyAndRange(babyId, from.toEpochMilli(), to.toEpochMilli())
            .map { it.map { e -> e.toDomain() } }

    override suspend fun getByBabyAndRange(babyId: String, from: Instant, to: Instant): List<DiaperLog> =
        dao.getByBabyAndRange(babyId, from.toEpochMilli(), to.toEpochMilli()).map { it.toDomain() }

    override suspend fun upsert(log: DiaperLog) =
        dao.upsert(DiaperLogEntity.fromDomain(log))

    override suspend fun delete(id: String) =
        dao.delete(id)
}
