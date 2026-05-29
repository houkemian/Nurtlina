package com.nurtlina.app.data.repository

import com.nurtlina.app.data.local.dao.FeedLogDao
import com.nurtlina.app.data.local.entity.FeedLogEntity
import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.repository.FeedLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomFeedLogRepository @Inject constructor(
    private val dao: FeedLogDao,
) : FeedLogRepository {

    override fun observeByBaby(babyId: String): Flow<List<FeedLog>> =
        dao.observeByBaby(babyId).map { it.map { e -> e.toDomain() } }

    override fun observeByBabyAndRange(babyId: String, from: Instant, to: Instant): Flow<List<FeedLog>> =
        dao.observeByBabyAndRange(babyId, from.toEpochMilli(), to.toEpochMilli())
            .map { it.map { e -> e.toDomain() } }

    override suspend fun getByBabyAndRange(babyId: String, from: Instant, to: Instant): List<FeedLog> =
        dao.getByBabyAndRange(babyId, from.toEpochMilli(), to.toEpochMilli()).map { it.toDomain() }

    override suspend fun upsert(log: FeedLog) =
        dao.upsert(FeedLogEntity.fromDomain(log))

    override suspend fun delete(id: String) =
        dao.delete(id)
}
