package com.nurtlina.app.data.repository

import androidx.room.withTransaction
import com.nurtlina.app.data.local.dao.FeedLogDao
import com.nurtlina.app.data.local.db.NurtlinaDatabase
import com.nurtlina.app.data.local.entity.FeedLogEntity
import com.nurtlina.app.data.local.entity.SyncStatus
import com.nurtlina.app.data.sync.SyncOperations
import com.nurtlina.app.data.sync.SyncQueueWriter
import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.repository.FeedLogRepository
import com.nurtlina.app.domain.repository.SessionRepository
import com.nurtlina.app.domain.repository.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomFeedLogRepository @Inject constructor(
    private val dao: FeedLogDao,
    private val database: NurtlinaDatabase,
    private val syncQueueWriter: SyncQueueWriter,
    private val sessionRepository: SessionRepository,
    private val syncManager: SyncManager,
) : FeedLogRepository {

    override fun observeByBaby(babyId: String): Flow<List<FeedLog>> =
        dao.observeByBaby(babyId).map { it.map { e -> e.toDomain() } }

    override fun observeByBabyAndRange(babyId: String, from: Instant, to: Instant): Flow<List<FeedLog>> =
        dao.observeByBabyAndRange(babyId, from.toEpochMilli(), to.toEpochMilli())
            .map { it.map { e -> e.toDomain() } }

    override suspend fun getByBabyAndRange(babyId: String, from: Instant, to: Instant): List<FeedLog> =
        dao.getByBabyAndRange(babyId, from.toEpochMilli(), to.toEpochMilli()).map { it.toDomain() }

    override suspend fun upsert(log: FeedLog) {
        val entity = FeedLogEntity.fromDomain(log).withSyncMetadata()
        database.withTransaction {
            dao.upsert(entity)
            syncQueueWriter.enqueueFeedLog(entity)
        }
        syncManager.requestSyncSoon()
    }

    override suspend fun delete(id: String) {
        val existing = dao.getById(id) ?: return
        val now = Instant.now().toEpochMilli()
        val entity = existing.copy(
            deletedAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING.name,
            lastSyncedAt = null,
        ).withSyncMetadata()
        database.withTransaction {
            syncQueueWriter.enqueueFeedLog(entity, SyncOperations.DELETE_FEED_LOG)
            dao.delete(id)
        }
        syncManager.requestSyncSoon()
    }

    private suspend fun FeedLogEntity.withSyncMetadata(): FeedLogEntity {
        val session = sessionRepository.get()
        return copy(
            familyId = familyId ?: session.defaultFamilyId,
            clientId = clientId ?: session.clientId,
            syncStatus = SyncStatus.PENDING.name,
            lastSyncedAt = null,
        )
    }
}
