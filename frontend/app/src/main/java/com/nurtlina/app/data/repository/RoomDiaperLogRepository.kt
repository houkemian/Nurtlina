package com.nurtlina.app.data.repository

import androidx.room.withTransaction
import com.nurtlina.app.data.local.dao.DiaperLogDao
import com.nurtlina.app.data.local.db.NurtlinaDatabase
import com.nurtlina.app.data.local.entity.DiaperLogEntity
import com.nurtlina.app.data.local.entity.SyncStatus
import com.nurtlina.app.data.sync.SyncOperations
import com.nurtlina.app.data.sync.SyncQueueWriter
import com.nurtlina.app.domain.model.DiaperLog
import com.nurtlina.app.domain.repository.DiaperLogRepository
import com.nurtlina.app.domain.repository.SessionRepository
import com.nurtlina.app.domain.repository.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomDiaperLogRepository @Inject constructor(
    private val dao: DiaperLogDao,
    private val database: NurtlinaDatabase,
    private val syncQueueWriter: SyncQueueWriter,
    private val sessionRepository: SessionRepository,
    private val syncManager: SyncManager,
) : DiaperLogRepository {

    override fun observeByBaby(babyId: String): Flow<List<DiaperLog>> =
        dao.observeByBaby(babyId).map { it.map { e -> e.toDomain() } }

    override fun observeByBabyAndRange(babyId: String, from: Instant, to: Instant): Flow<List<DiaperLog>> =
        dao.observeByBabyAndRange(babyId, from.toEpochMilli(), to.toEpochMilli())
            .map { it.map { e -> e.toDomain() } }

    override suspend fun getByBabyAndRange(babyId: String, from: Instant, to: Instant): List<DiaperLog> =
        dao.getByBabyAndRange(babyId, from.toEpochMilli(), to.toEpochMilli()).map { it.toDomain() }

    override suspend fun upsert(log: DiaperLog) {
        val entity = DiaperLogEntity.fromDomain(log).withSyncMetadata()
        database.withTransaction {
            dao.upsert(entity)
            syncQueueWriter.enqueueDiaperLog(entity)
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
            syncQueueWriter.enqueueDiaperLog(entity, SyncOperations.DELETE_DIAPER_LOG)
            dao.delete(id)
        }
        syncManager.requestSyncSoon()
    }

    private suspend fun DiaperLogEntity.withSyncMetadata(): DiaperLogEntity {
        val session = sessionRepository.get()
        return copy(
            familyId = familyId ?: session.defaultFamilyId,
            clientId = clientId ?: session.clientId,
            syncStatus = SyncStatus.PENDING.name,
            lastSyncedAt = null,
        )
    }
}
