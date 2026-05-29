package com.nurtlina.app.data.repository

import com.nurtlina.app.data.local.dao.SleepLogDao
import com.nurtlina.app.data.local.entity.SleepLogEntity
import com.nurtlina.app.domain.model.SleepLog
import com.nurtlina.app.domain.repository.SleepLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomSleepLogRepository @Inject constructor(
    private val dao: SleepLogDao,
) : SleepLogRepository {

    override fun observeByBaby(babyId: String): Flow<List<SleepLog>> =
        dao.observeByBaby(babyId).map { it.map { e -> e.toDomain() } }

    override fun observeByBabyAndRange(babyId: String, from: Instant, to: Instant): Flow<List<SleepLog>> =
        dao.observeByBabyAndRange(babyId, from.toEpochMilli(), to.toEpochMilli())
            .map { it.map { e -> e.toDomain() } }

    override fun observeActiveSleep(babyId: String): Flow<SleepLog?> =
        dao.observeActiveSleep(babyId).map { it?.toDomain() }

    override suspend fun getByBabyAndRange(babyId: String, from: Instant, to: Instant): List<SleepLog> =
        dao.getByBabyAndRange(babyId, from.toEpochMilli(), to.toEpochMilli()).map { it.toDomain() }

    override suspend fun getActiveSleep(babyId: String): SleepLog? =
        dao.getActiveSleep(babyId)?.toDomain()

    override suspend fun upsert(log: SleepLog) =
        dao.upsert(SleepLogEntity.fromDomain(log))

    override suspend fun delete(id: String) =
        dao.delete(id)
}
